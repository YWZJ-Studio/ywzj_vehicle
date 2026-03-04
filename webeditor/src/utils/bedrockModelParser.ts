/**
 * Bedrock Model 解析器
 * 将 Bedrock JSON 模型解析为便于渲染的数据结构
 */

import type {BedrockModelRoot, ParsedBedrockModel, ParsedBone, Vector3, Vector3Array} from '@/types/bedrockModel';
import {parseJsonWithComments} from './jsonParser';

/**
 * 将 Vector3Array 转换为 Vector3 对象
 */
function arrayToVector3(arr?: Vector3Array): Vector3 {
  if (!arr || arr.length !== 3) {
    return { x: 0, y: 0, z: 0 };
  }
  return { x: arr[0], y: arr[1], z: arr[2] };
}

/**
 * 解析 Bedrock 模型 JSON
 */
export function parseBedrockModel(jsonContent: string): ParsedBedrockModel {
  const root: BedrockModelRoot = parseJsonWithComments(jsonContent);

  if (!root['minecraft:geometry'] || root['minecraft:geometry'].length === 0) {
    throw new Error('Invalid Bedrock model: missing minecraft:geometry');
  }

  // 使用第一个几何体定义
  const geometry = root['minecraft:geometry'][0];
  const bones = new Map<string, ParsedBone>();
  const childrenMap = new Map<string, string[]>();

  // 第一遍：创建所有骨骼
  for (const bone of geometry.bones) {
    const parsedBone: ParsedBone = {
      name: bone.name,
      parent: bone.parent || null,
      pivot: arrayToVector3(bone.pivot),
      rotation: arrayToVector3(bone.rotation),
      cubes: bone.cubes || [],
      children: []
    };
    bones.set(bone.name, parsedBone);

    // 记录父子关系
    if (bone.parent) {
      if (!childrenMap.has(bone.parent)) {
        childrenMap.set(bone.parent, []);
      }
      childrenMap.get(bone.parent)!.push(bone.name);
    }
  }

  // 第二遍：填充子骨骼列表
  for (const [parentName, children] of childrenMap) {
    const parent = bones.get(parentName);
    if (parent) {
      parent.children = children;
    }
  }

  // 找出根骨骼（没有父骨骼的）
  const rootBones: string[] = [];
  for (const [name, bone] of bones) {
    if (!bone.parent) {
      rootBones.push(name);
    }
  }

  return {
    identifier: geometry.description.identifier,
    textureWidth: geometry.description.texture_width,
    textureHeight: geometry.description.texture_height,
    bones,
    rootBones
  };
}

/**
 * 计算骨骼的世界坐标变换
 */
export function calculateWorldTransform(
  boneName: string,
  bones: Map<string, ParsedBone>
): { position: Vector3; rotation: Vector3 } {
  const bone = bones.get(boneName);
  if (!bone) {
    return { position: { x: 0, y: 0, z: 0 }, rotation: { x: 0, y: 0, z: 0 } };
  }

  let position = { ...bone.pivot };
  let rotation = { ...bone.rotation };

  // 递归累积父骨骼的变换
  let currentBone = bone;
  while (currentBone.parent) {
    const parentBone = bones.get(currentBone.parent);
    if (!parentBone) break;

    // 累加位置
    position.x += parentBone.pivot.x;
    position.y += parentBone.pivot.y;
    position.z += parentBone.pivot.z;

    // 累加旋转
    rotation.x += parentBone.rotation.x;
    rotation.y += parentBone.rotation.y;
    rotation.z += parentBone.rotation.z;

    currentBone = parentBone;
  }

  return { position, rotation };
}
