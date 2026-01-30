/**
 * Bedrock Model 类型定义
 * 基于 Minecraft Bedrock Edition 模型格式
 */

export interface Vector3 {
  x: number;
  y: number;
  z: number;
}

export type Vector3Array = [number, number, number];

export interface UVFace {
  uv: [number, number];
  uv_size: [number, number];
}

export interface CubeUV {
  north?: UVFace;
  east?: UVFace;
  south?: UVFace;
  west?: UVFace;
  up?: UVFace;
  down?: UVFace;
}

export interface BedrockCube {
  origin: Vector3Array;
  size: Vector3Array;
  pivot?: Vector3Array;
  rotation?: Vector3Array;
  inflate?: number;
  uv?: CubeUV | Vector3Array;
  mirror?: boolean;
}

export interface BedrockBone {
  name: string;
  parent?: string;
  pivot?: Vector3Array;
  rotation?: Vector3Array;
  cubes?: BedrockCube[];
  locators?: Record<string, Vector3Array>;
}

export interface GeometryDescription {
  identifier: string;
  texture_width: number;
  texture_height: number;
  visible_bounds_width?: number;
  visible_bounds_height?: number;
  visible_bounds_offset?: Vector3Array;
}

export interface BedrockGeometry {
  description: GeometryDescription;
  bones: BedrockBone[];
}

export interface BedrockModelRoot {
  format_version: string;
  'minecraft:geometry': BedrockGeometry[];
}

/**
 * 解析后的骨骼数据（带索引）
 */
export interface ParsedBone {
  name: string;
  parent: string | null;
  pivot: Vector3;
  rotation: Vector3;
  cubes: BedrockCube[];
  children: string[];
}

/**
 * 解析后的模型数据
 */
export interface ParsedBedrockModel {
  identifier: string;
  textureWidth: number;
  textureHeight: number;
  bones: Map<string, ParsedBone>;
  rootBones: string[];
}
