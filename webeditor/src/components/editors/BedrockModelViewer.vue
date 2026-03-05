<template>
  <div class="bedrock-model-viewer">
    <div class="viewer-toolbar">
      <div class="model-info">
        <el-icon><View /></el-icon>
        <span v-if="modelData">{{ modelData.identifier }}</span>
        <span v-else>加载中...</span>
      </div>
      <div class="viewer-controls">
        <el-button-group size="small">
          <el-button @click="handleTextureSelect">
            <el-icon><Picture /></el-icon>
            选择纹理
          </el-button>
          <el-button @click="resetCamera">
            <el-icon><Refresh /></el-icon>
            重置视图
          </el-button>
          <el-button @click="toggleGrid">
            <el-icon><Grid /></el-icon>
            {{ showGrid ? '隐藏' : '显示' }}网格
          </el-button>
          <el-button @click="toggleAxes">
            <el-icon><Coordinate /></el-icon>
            {{ showAxes ? '隐藏' : '显示' }}坐标轴
          </el-button>
        </el-button-group>
        <el-button-group size="small">
          <el-button
            :type="renderMode === 'texture' ? 'primary' : ''"
            @click="renderMode = 'texture'"
            :disabled="!currentTexture"
          >
            纹理
          </el-button>
          <el-button
            :type="renderMode === 'debug' ? 'primary' : ''"
            @click="renderMode = 'debug'"
          >
            调试
          </el-button>
          <el-button
            :type="renderMode === 'wireframe' ? 'primary' : ''"
            @click="renderMode = 'wireframe'"
          >
            线框
          </el-button>
        </el-button-group>
        <el-button-group size="small">
          <el-button @click="handleStructureModelSelect">
            结构模型
          </el-button>
          <el-button @click="clearStructureModel" :disabled="!structureModelData">
            清除结构
          </el-button>
        </el-button-group>
        <el-button-group size="small">
          <el-button @click="toggleStats">
            {{ showStats ? '隐藏' : '显示' }}FPS
          </el-button>
        </el-button-group>
      </div>
    </div>

    <div ref="canvasContainer" class="canvas-container">
      <div v-if="error" class="error-message">
        <el-alert
          :title="error"
          type="error"
          show-icon
          :closable="false"
        />
      </div>
      <div v-if="textureLoading" class="loading-overlay">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <div class="loading-text">加载纹理中...</div>
      </div>
    </div>

    <div class="viewer-info">
      <div class="info-item" v-if="currentTexture">
        <span class="label">纹理:</span>
        <span class="value texture-name" :title="textureName">{{ textureName }}</span>
      </div>
      <div class="info-item" v-if="structureModelName">
        <span class="label">结构模型:</span>
        <span class="value texture-name" :title="structureModelName">{{ structureModelName }}</span>
      </div>
      <div class="info-item">
        <span class="label">骨骼数量:</span>
        <span class="value">{{ modelData?.bones.size || 0 }}</span>
      </div>
      <div class="info-item">
        <span class="label">纹理尺寸:</span>
        <span class="value">{{ modelData?.textureWidth || 0 }} × {{ modelData?.textureHeight || 0 }}</span>
      </div>
      <div class="info-item">
        <span class="label">根骨骼:</span>
        <span class="value">{{ modelData?.rootBones.length || 0 }}</span>
      </div>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="textureInput"
      type="file"
      accept="image/png"
      style="display: none"
      @change="handleTextureFileChange"
    />
    <input
      ref="structureInput"
      type="file"
      accept="application/json,.json"
      style="display: none"
      @change="handleStructureFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import {onActivated, onDeactivated, onMounted, onUnmounted, ref, watch} from 'vue';
import {Coordinate, Grid, Loading, Picture, Refresh, View} from '@element-plus/icons-vue';
import * as THREE from 'three';
import {OrbitControls} from 'three/examples/jsm/controls/OrbitControls.js';
import Stats from 'stats.js';
import {parseBedrockModel} from '@/utils/bedrockModelParser';
import type {BedrockCube, CubeUV, ParsedBedrockModel, ParsedBone, UVFace} from '@/types/bedrockModel';

interface Props {
  content: string;
  path: string;
  autoTexture?: string;
  autoTextureName?: string;
  autoStructureModel?: string;
  autoStructureModelName?: string;
}

const props = defineProps<Props>();

const canvasContainer = ref<HTMLDivElement>();
const textureInput = ref<HTMLInputElement>();
const structureInput = ref<HTMLInputElement>();
const modelData = ref<ParsedBedrockModel | null>(null);
const error = ref<string>('');
const showGrid = ref(true);
const showAxes = ref(true);
const showStats = ref(false);
const renderMode = ref<'texture' | 'debug' | 'wireframe'>('debug');
const currentTexture = ref<THREE.Texture | null>(null);
const textureName = ref<string>('');
const textureLoading = ref(false);
const structureModelData = ref<ParsedBedrockModel | null>(null);
const structureModelName = ref<string>('');

// Three.js 对象
let scene: THREE.Scene;
let camera: THREE.PerspectiveCamera;
let renderer: THREE.WebGLRenderer;
let controls: OrbitControls;
let gridHelper: THREE.GridHelper;
let axesHelper: THREE.Group; // 改为 Group 以支持自定义坐标轴
let animationId: number;
let stats: Stats | null = null;
let structureGroup: THREE.Group | null = null;

// 存储所有渲染对象用于模式切换
let modelMeshes: THREE.Group[] = [];

// 性能优化相关
let needsRender = true; // 标记是否需要重新渲染
let geometryCache = new Map<string, THREE.BufferGeometry>(); // 几何体缓存
let materialCache = new Map<string, THREE.Material>(); // 材质缓存

// 颜色数组，用于区分不同骨骼
const boneColors = [
  0x4CAF50, 0x2196F3, 0xFFC107, 0xE91E63, 0x9C27B0,
  0x00BCD4, 0xFF5722, 0x8BC34A, 0xFF9800, 0x03A9F4
];

onMounted(() => {
  try {
    initThreeJS();
    loadModel();
    if (props.autoTexture) {
      loadAutoTexture();
    }
    if (props.autoStructureModel) {
      loadAutoStructureModel();
    }
  } catch (err: any) {
    error.value = '初始化失败: ' + (err.message || err);
  }
});

onUnmounted(() => {
  cleanup();
});

onActivated(() => {
  // 恢复渲染循环
  if (!animationId) {
    animate();
  }
  // 重新调整大小以适应布局
  handleResize();
});

onDeactivated(() => {
  // 暂停渲染循环以节省资源
  if (animationId) {
    cancelAnimationFrame(animationId);
    animationId = 0;
  }
});

watch(() => props.content, () => {
  loadModel();
});

watch(() => props.autoTexture, (newTexture) => {
  if (newTexture) {
    loadAutoTexture();
  }
});

watch(() => props.autoStructureModel, (newStructureModel) => {
  if (newStructureModel) {
    loadAutoStructureModel();
  } else {
    clearStructureModel();
  }
});

watch(renderMode, () => {
  if (modelData.value) {
    reloadModel();
  }
});



/**
 * 创建符合 Bedrock/Blockbench 坐标系统的坐标轴
 * X轴和Z轴都需要反向以匹配 Blockbench 的显示
 */
function createBedrockAxesHelper(size: number): THREE.Group {
  const axesGroup = new THREE.Group();

  // X 轴 - 红色 (反向，以匹配 Bedrock 坐标系)
  const xGeometry = new THREE.BufferGeometry().setFromPoints([
    new THREE.Vector3(0, 0, 0),
    new THREE.Vector3(-size, 0, 0) // X轴反向
  ]);
  const xMaterial = new THREE.LineBasicMaterial({ color: 0xff0000 });
  const xAxis = new THREE.Line(xGeometry, xMaterial);
  axesGroup.add(xAxis);

  // Y 轴 - 绿色 (不变)
  const yGeometry = new THREE.BufferGeometry().setFromPoints([
    new THREE.Vector3(0, 0, 0),
    new THREE.Vector3(0, size, 0)
  ]);
  const yMaterial = new THREE.LineBasicMaterial({ color: 0x00ff00 });
  const yAxis = new THREE.Line(yGeometry, yMaterial);
  axesGroup.add(yAxis);

  // Z 轴 - 蓝色 (反向，以匹配 Bedrock 坐标系)
  const zGeometry = new THREE.BufferGeometry().setFromPoints([
    new THREE.Vector3(0, 0, 0),
    new THREE.Vector3(0, 0, -size) // Z轴反向
  ]);
  const zMaterial = new THREE.LineBasicMaterial({ color: 0x0000ff });
  const zAxis = new THREE.Line(zGeometry, zMaterial);
  axesGroup.add(zAxis);

  return axesGroup;
}

function initThreeJS() {
  if (!canvasContainer.value) return;

  // 创建场景
  scene = new THREE.Scene();
  scene.background = new THREE.Color(0x2c2c2c);

  // 创建相机
  const width = canvasContainer.value.clientWidth;
  const height = canvasContainer.value.clientHeight;
  camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 10000);
  camera.position.set(50, 50, 50);
  camera.lookAt(0, 0, 0);

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({
    antialias: true,
    powerPreference: 'high-performance', // 使用高性能GPU
    stencil: false, // 不需要模板缓冲
    depth: true // 保留深度缓冲
  });
  renderer.setSize(width, height);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2)); // 限制最大像素比避免过度渲染
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;

  // 启用 frustum culling 优化（默认已启用，但明确设置）
  // 这会自动跳过不在视野内的对象

  canvasContainer.value.appendChild(renderer.domElement);

  // 添加光照
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
  scene.add(ambientLight);

  const directionalLight1 = new THREE.DirectionalLight(0xffffff, 0.8);
  directionalLight1.position.set(50, 50, 50);
  directionalLight1.castShadow = true;
  directionalLight1.shadow.mapSize.set(1024, 1024);
  directionalLight1.shadow.camera.near = 1;
  directionalLight1.shadow.camera.far = 500;
  directionalLight1.shadow.camera.left = -200;
  directionalLight1.shadow.camera.right = 200;
  directionalLight1.shadow.camera.top = 200;
  directionalLight1.shadow.camera.bottom = -200;
  scene.add(directionalLight1);

  const directionalLight2 = new THREE.DirectionalLight(0xffffff, 0.4);
  directionalLight2.position.set(-50, -50, -50);
  scene.add(directionalLight2);

  // 添加网格
  gridHelper = new THREE.GridHelper(100, 20, 0x444444, 0x222222);
  scene.add(gridHelper);

  // 添加自定义坐标轴（匹配 Bedrock/Blockbench 坐标系统）
  // 由于代码中 Z 轴取反，这里创建匹配的坐标轴
  axesHelper = createBedrockAxesHelper(50);
  scene.add(axesHelper);

  // 添加轨道控制
  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.dampingFactor = 0.05;
  controls.minDistance = 10;
  controls.maxDistance = 500;

  // 监听控制器变化，标记需要重新渲染
  controls.addEventListener('change', () => {
    needsRender = true;
  });

  // 初始化 Stats（FPS 监控）
  stats = new Stats();
  stats.showPanel(0); // 0: fps, 1: ms, 2: mb
  stats.dom.style.position = 'absolute';
  stats.dom.style.left = '0px';
  stats.dom.style.top = '0px';
  stats.dom.style.display = showStats.value ? 'block' : 'none';
  canvasContainer.value.appendChild(stats.dom);

  // 监听窗口大小变化
  window.addEventListener('resize', handleResize);


  // 开始渲染循环
  animate();
}

function handleResize() {
  if (!canvasContainer.value) return;

  const width = canvasContainer.value.clientWidth;
  const height = canvasContainer.value.clientHeight;

  camera.aspect = width / height;
  camera.updateProjectionMatrix();
  renderer.setSize(width, height);

  needsRender = true; // 标记需要重新渲染
}

function animate() {
  animationId = requestAnimationFrame(animate);

  // 更新 FPS 统计
  if (stats) stats.begin();

  // 只有在需要渲染时才渲染
  // 由于启用了 damping，controls.update() 可能会改变相机位置
  const controlsUpdated = controls.update();

  if (needsRender || controlsUpdated) {
    renderer.render(scene, camera);
    needsRender = false;
  }

  if (stats) stats.end();
}

function loadModel() {
  error.value = '';

  try {
    // 解析模型
    const parsed = parseBedrockModel(props.content);
    modelData.value = parsed;

    // 清除旧模型
    clearModel();

    // 渲染新模型
    renderModel(parsed);

    // 结构模型一起渲染
    renderStructureModel();

    // 自动调整相机位置
    fitCameraToModel();
  } catch (err: any) {
    error.value = '解析模型失败: ' + (err.message || err);
    console.error('Failed to parse model:', err);
  }
}

function reloadModel() {
  if (!modelData.value) return;
  clearModel();
  renderModel(modelData.value);
  renderStructureModel();
  fitCameraToModel();
}

function handleTextureSelect() {
  textureInput.value?.click();
}

async function handleTextureFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (!file) return;

  if (!file.type.startsWith('image/png')) {
    error.value = '请选择 PNG 格式的纹理文件';
    return;
  }

  try {
    textureLoading.value = true;
    error.value = '';

    const dataUrl = await readFileAsDataURL(file);
    const texture = await loadTexture(dataUrl);

    if (currentTexture.value) {
      currentTexture.value.dispose();
    }

    currentTexture.value = texture;
    textureName.value = file.name;

    renderMode.value = 'texture';
    reloadModel();
  } catch (err: any) {
    error.value = '纹理加载失败: ' + (err.message || err);
    console.error('Failed to load texture:', err);
  } finally {
    textureLoading.value = false;
    input.value = '';
  }
}

function readFileAsDataURL(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

function loadTexture(dataUrl: string): Promise<THREE.Texture> {
  return new Promise((resolve, reject) => {
    const loader = new THREE.TextureLoader();
    loader.load(
      dataUrl,
      (texture) => {
        texture.minFilter = THREE.NearestFilter;
        texture.magFilter = THREE.NearestFilter;
        texture.wrapS = THREE.ClampToEdgeWrapping;
        texture.wrapT = THREE.ClampToEdgeWrapping;
        texture.flipY = false;
        resolve(texture);
      },
      undefined,
      reject
    );
  });
}

async function loadAutoTexture() {
  if (!props.autoTexture) return;

  try {
    textureLoading.value = true;
    error.value = '';

    const texture = await loadTexture(props.autoTexture);

    if (currentTexture.value) {
      currentTexture.value.dispose();
    }

    currentTexture.value = texture;
    textureName.value = '自动加载';

    renderMode.value = 'texture';
    reloadModel();
  } catch (err: any) {
    console.error('自动加载纹理失败:', err);
  } finally {
    textureLoading.value = false;
  }
}

function loadAutoStructureModel() {
  if (!props.autoStructureModel) return;

  try {
    error.value = '';
    const parsed = parseBedrockModel(props.autoStructureModel);
    structureModelData.value = parsed;
    structureModelName.value = props.autoStructureModelName || '自动加载';
    renderStructureModel();
  } catch (err: any) {
    console.error('自动加载结构模型失败:', err);
  }
}
function handleStructureModelSelect() {
  structureInput.value?.click();
}

async function handleStructureFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (!file) return;

  if (!file.name.toLowerCase().endsWith('.json')) {
    error.value = '请选择结构模型的 JSON 文件';
    return;
  }

  try {
    error.value = '';
    const text = await file.text();
    const parsed = parseBedrockModel(text);
    structureModelData.value = parsed;
    structureModelName.value = file.name;
    renderStructureModel();
  } catch (err: any) {
    error.value = '结构模型加载失败: ' + (err.message || err);
    console.error('Failed to load structure model:', err);
  } finally {
    input.value = '';
  }
}

function clearStructureModel() {
  structureModelData.value = null;
  structureModelName.value = '';
  if (structureGroup) {
    scene.remove(structureGroup);
    structureGroup = null;
    needsRender = true;
  }
}

function clearModel() {
  // 移除所有对象（除了网格和坐标轴）
  const objectsToRemove: THREE.Object3D[] = [];
  scene.children.forEach((obj) => {
    if (obj !== gridHelper && obj !== axesHelper && !['AmbientLight', 'DirectionalLight'].includes(obj.type)) {
      objectsToRemove.push(obj);
    }
  });

  objectsToRemove.forEach(obj => {
    // 递归清理对象及其子对象
    obj.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        child.geometry.dispose();
        if (child.material instanceof THREE.Material) {
          child.material.dispose();
        } else if (Array.isArray(child.material)) {
          child.material.forEach(mat => mat.dispose());
        }
      } else if (child instanceof THREE.LineSegments) {
        child.geometry.dispose();
        if (child.material instanceof THREE.Material) {
          child.material.dispose();
        }
      }
    });
    scene.remove(obj);
  });

  modelMeshes = [];
  structureGroup = null;

  // 清理几何体缓存
  geometryCache.forEach(geometry => geometry.dispose());
  geometryCache.clear();

  // 清理材质缓存（但保留纹理材质）
  materialCache.forEach((material, key) => {
    if (!key.includes('texture')) {
      material.dispose();
    }
  });
  materialCache.clear();

  needsRender = true; // 标记需要重新渲染
}

function renderModel(model: ParsedBedrockModel) {
  let colorIndex = 0;
  const boneGroupMap = new Map<string, THREE.Group>();

  // 第一步：创建所有骨骼组并渲染内容
  for (const [name, bone] of model.bones) {
    const boneColor = boneColors[colorIndex % boneColors.length];
    colorIndex++;

    // 创建骨骼组
    const boneGroup = new THREE.Group();

    // 应用骨骼旋转 (ZYX order in Bedrock)
    boneGroup.rotation.order = 'ZYX';
    boneGroup.rotation.set(
      THREE.MathUtils.degToRad(bone.rotation.x),
      THREE.MathUtils.degToRad(-bone.rotation.y),
      THREE.MathUtils.degToRad(-bone.rotation.z)
    );

    // 渲染该骨骼的所有立方体
    for (const cube of bone.cubes) {
      renderCube(bone, cube, boneColor, boneGroup, model);
    }

    boneGroupMap.set(name, boneGroup);
    modelMeshes.push(boneGroup);
  }

  // 第二步：构建层级关系并设置位置
  for (const [name, bone] of model.bones) {
    const boneGroup = boneGroupMap.get(name)!;

    if (bone.parent) {
      const parentGroup = boneGroupMap.get(bone.parent);
      const parentBone = model.bones.get(bone.parent);

      if (parentGroup && parentBone) {
        // 子骨骼位置相对于父骨骼枢轴
        boneGroup.position.set(
          bone.pivot.x - parentBone.pivot.x,
          bone.pivot.y - parentBone.pivot.y,
          -(bone.pivot.z - parentBone.pivot.z)
        );
        parentGroup.add(boneGroup);
      } else {
        // 如果找不到父骨骼，作为根骨骼处理
        boneGroup.position.set(
          bone.pivot.x,
          bone.pivot.y,
          -bone.pivot.z
        );
        scene.add(boneGroup);
      }
    } else {
      // 根骨骼
      boneGroup.position.set(
        bone.pivot.x,
        bone.pivot.y,
        -bone.pivot.z
      );
      scene.add(boneGroup);
    }
  }

  needsRender = true; // 标记需要重新渲染
}

function renderStructureModel() {
  if (!structureModelData.value) return;

  if (structureGroup) {
    scene.remove(structureGroup);
    structureGroup = null;
  }

  const model = structureModelData.value;
  const lineMaterial = new THREE.LineBasicMaterial({ color: 0xffffff, transparent: false });
  structureGroup = new THREE.Group();

  const boneGroupMap = new Map<string, THREE.Group>();

  // Pass 1: Create groups and geometry
  for (const [name, bone] of model.bones) {
    const boneGroup = new THREE.Group();

    boneGroup.rotation.order = 'ZYX';
    boneGroup.rotation.set(
      THREE.MathUtils.degToRad(bone.rotation.x),
      THREE.MathUtils.degToRad(-bone.rotation.y),
      THREE.MathUtils.degToRad(-bone.rotation.z)
    );

    for (const cube of bone.cubes) {
      const [ox, oy, oz] = cube.origin;
      const [sx, sy, sz] = cube.size;
      const inflate = cube.inflate || 0;

      const geometry = new THREE.BoxGeometry(
        sx + inflate * 2,
        sy + inflate * 2,
        sz + inflate * 2
      );
      const edges = new THREE.EdgesGeometry(geometry);
      const line = new THREE.LineSegments(edges, lineMaterial);

      if (cube.rotation && cube.pivot) {
        const cubeGroup = new THREE.Group();
        const [px, py, pz] = cube.pivot;

        cubeGroup.position.set(
          px - bone.pivot.x,
          py - bone.pivot.y,
          -(pz - bone.pivot.z)
        );

        cubeGroup.rotation.order = 'ZYX';
        cubeGroup.rotation.set(
          THREE.MathUtils.degToRad(cube.rotation[0]),
          THREE.MathUtils.degToRad(-cube.rotation[1]),
          THREE.MathUtils.degToRad(-cube.rotation[2])
        );

        line.position.set(
          ox + sx / 2 - px,
          oy + sy / 2 - py,
          -(oz + sz / 2 - pz)
        );

        cubeGroup.add(line);
        boneGroup.add(cubeGroup);
      } else {
        line.position.set(
          ox + sx / 2 - bone.pivot.x,
          oy + sy / 2 - bone.pivot.y,
          -(oz + sz / 2 - bone.pivot.z)
        );

        boneGroup.add(line);
      }
    }

    boneGroupMap.set(name, boneGroup);
  }

  // Pass 2: Hierarchy
  for (const [name, bone] of model.bones) {
    const boneGroup = boneGroupMap.get(name)!;

    if (bone.parent) {
      const parentGroup = boneGroupMap.get(bone.parent);
      const parentBone = model.bones.get(bone.parent);

      if (parentGroup && parentBone) {
        boneGroup.position.set(
          bone.pivot.x - parentBone.pivot.x,
          bone.pivot.y - parentBone.pivot.y,
          -(bone.pivot.z - parentBone.pivot.z)
        );
        parentGroup.add(boneGroup);
      } else {
        boneGroup.position.set(bone.pivot.x, bone.pivot.y, -bone.pivot.z);
        structureGroup.add(boneGroup);
      }
    } else {
      boneGroup.position.set(bone.pivot.x, bone.pivot.y, -bone.pivot.z);
      structureGroup.add(boneGroup);
    }
  }

  scene.add(structureGroup);
  needsRender = true;
}

function renderCube(
  bone: ParsedBone,
  cube: BedrockCube,
  color: number,
  boneGroup: THREE.Group,
  model: ParsedBedrockModel
) {
  // Bedrock 坐标系转换：
  // Bedrock: X右 Y上 Z南（向玩家）
  // Three.js: X右 Y上 Z前（-Z向玩家）

  const [ox, oy, oz] = cube.origin;
  const [sx, sy, sz] = cube.size;
  const inflate = cube.inflate || 0;

  // 创建几何体缓存键
  const geometryKey = `${sx}_${sy}_${sz}_${inflate}_${renderMode.value}_${!!currentTexture.value}`;

  // 创建立方体几何体（考虑 inflate 膨胀值）
  let geometry: THREE.BufferGeometry;

  // 检查缓存
  if (geometryCache.has(geometryKey)) {
    geometry = geometryCache.get(geometryKey)!;
  } else {
    // 如果是纹理模式且有纹理和 UV 数据，创建自定义 UV 几何体
    if (renderMode.value === 'texture' && currentTexture.value && cube.uv && typeof cube.uv === 'object' && !Array.isArray(cube.uv)) {
      geometry = createCubeWithUV(sx, sy, sz, inflate, cube.uv as CubeUV, model);
    } else {
      geometry = new THREE.BoxGeometry(
        sx + inflate * 2,
        sy + inflate * 2,
        sz + inflate * 2
      );
    }
    // 缓存几何体（仅缓存标准几何体，不缓存带自定义UV的）
    if (renderMode.value !== 'texture') {
      geometryCache.set(geometryKey, geometry);
    }
  }

  // 创建材质缓存键
  const materialKey = `${renderMode.value}_${color}_${!!currentTexture.value}`;

  // 创建材质
  let material: THREE.Material;

  // 检查材质缓存
  if (materialCache.has(materialKey)) {
    material = materialCache.get(materialKey)!;
  } else {
    if (renderMode.value === 'wireframe') {
      // 线框模式
      material = new THREE.MeshBasicMaterial({
        color: color,
        wireframe: true
      });
    } else if (renderMode.value === 'texture' && currentTexture.value) {
      // 纹理模式
      material = new THREE.MeshStandardMaterial({
        map: currentTexture.value,
        side: THREE.DoubleSide,
        transparent: false,
        alphaTest: 0.5
      });
    } else {
      // 调试模式（彩色）
      material = new THREE.MeshLambertMaterial({
        color: color,
        transparent: false,
        opacity: 1,
        side: THREE.DoubleSide
      });
    }
    // 缓存材质
    materialCache.set(materialKey, material);
  }

  const mesh = new THREE.Mesh(geometry, material);
  mesh.castShadow = true;
  mesh.receiveShadow = true;

  // 如果立方体有旋转，需要创建子组处理旋转
  if (cube.rotation && cube.pivot) {
    const cubeGroup = new THREE.Group();

    // 立方体枢轴点位置（相对于骨骼枢轴）
    const [px, py, pz] = cube.pivot;
    cubeGroup.position.set(
      px - bone.pivot.x,
      py - bone.pivot.y,
      -(pz - bone.pivot.z) // Z轴取反
    );

    // 应用立方体旋转（绕立方体枢轴旋转）
    // 由于 Z 坐标取反，Y 和 Z 旋转都需要取反
    cubeGroup.rotation.order = 'ZYX';
    cubeGroup.rotation.set(
      THREE.MathUtils.degToRad(cube.rotation[0]),   // X 轴不变
      THREE.MathUtils.degToRad(-cube.rotation[1]),  // Y 轴取反
      THREE.MathUtils.degToRad(-cube.rotation[2])   // Z 轴取反
    );

    // 立方体几何中心相对于立方体枢轴的位置
    mesh.position.set(
      ox + sx / 2 - px,
      oy + sy / 2 - py,
      -(oz + sz / 2 - pz) // Z轴取反
    );

    cubeGroup.add(mesh);

    boneGroup.add(cubeGroup);
  } else {
    // 没有旋转的立方体：几何中心相对于骨骼枢轴的位置
    mesh.position.set(
      ox + sx / 2 - bone.pivot.x,
      oy + sy / 2 - bone.pivot.y,
      -(oz + sz / 2 - bone.pivot.z) // Z轴取反
    );

    boneGroup.add(mesh);
  }
}

/**
 * 创建带有自定义 UV 映射的立方体几何体
 * 基于 Blockbench 的实现，完全遵循 Bedrock 模型 UV 标准
 *
 * Bedrock 坐标系: X右(东), Y上, Z南(向玩家)
 * Three.js 坐标系: X右, Y上, Z前(向屏幕外)
 * 转换: ThreeZ = -BedrockZ
 *
 * Bedrock UV 坐标: 原点左上(0,0), U向右, V向下
 * Three.js UV 坐标: 原点左下(0,0), U向右, V向上
 * 转换: ThreeV = 1 - BedrockV
 */
function createCubeWithUV(
  width: number,
  height: number,
  depth: number,
  inflate: number,
  cubeUV: CubeUV,
  model: ParsedBedrockModel
): THREE.BufferGeometry {
  const w = width + inflate * 2;
  const h = height + inflate * 2;
  const d = depth + inflate * 2;

  const hw = w / 2;
  const hh = h / 2;
  const hd = d / 2;

  const texWidth = model.textureWidth;
  const texHeight = model.textureHeight;

  // 辅助函数：将 Bedrock UV 转换为 Three.js UV
  const getUV = (uvFace: UVFace | undefined, corner: 'tl' | 'tr' | 'br' | 'bl'): [number, number] => {
    if (!uvFace) {
      return [0, 0];
    }

    const [u, v] = uvFace.uv;
    const [uSize, vSize] = uvFace.uv_size;

    // 计算四个角的 Bedrock UV 坐标（处理负尺寸=翻转）
    const u0 = uSize >= 0 ? u : u + uSize;
    const u1 = uSize >= 0 ? u + uSize : u;
    const v0 = vSize >= 0 ? v : v + vSize;
    const v1 = vSize >= 0 ? v + vSize : v;

    // 归一化
    const uMin = u0 / texWidth;
    const uMax = u1 / texWidth;
    const vMin = v0 / texHeight;
    const vMax = v1 / texHeight;

    // 返回对应角的UV
    switch (corner) {
      case 'tl': return [uMin, vMin];  // 左上
      case 'tr': return [uMax, vMin];  // 右上
      case 'br': return [uMax, vMax];  // 右下
      case 'bl': return [uMin, vMax];  // 左下
    }
  };

  // 存储所有顶点数据
  const positions: number[] = [];
  const uvs: number[] = [];
  const indices: number[] = [];
  let vertexIndex = 0;

  // 辅助函数：添加四边形（两个三角形）
  const addQuad = (
    v0: [number, number, number], uv0: [number, number],
    v1: [number, number, number], uv1: [number, number],
    v2: [number, number, number], uv2: [number, number],
    v3: [number, number, number], uv3: [number, number]
  ) => {
    // 顶点位置
    positions.push(...v0, ...v1, ...v2, ...v3);
    // UV 坐标
    uvs.push(...uv0, ...uv1, ...uv2, ...uv3);
    // 索引（两个三角形: 0-1-2, 0-2-3）
    indices.push(
      vertexIndex, vertexIndex + 1, vertexIndex + 2,
      vertexIndex, vertexIndex + 2, vertexIndex + 3
    );
    vertexIndex += 4;
  };

  // === 定义6个面（按照 Three.js 标准立方体的面顺序）===

  // West 面 (Bedrock -X, Three.js -X)
  // 由于Z轴反转，这里使用原本East面的几何定义（+X坐标）
  // 水平翻转UV以修正镜像问题
  addQuad(
    [hw, -hh, -hd], getUV(cubeUV.west, 'br'),  // 下前（Bedrock 南）
    [hw, hh, -hd],  getUV(cubeUV.west, 'tr'),  // 上前
    [hw, hh, hd],   getUV(cubeUV.west, 'tl'),  // 上后（Bedrock 北）
    [hw, -hh, hd],  getUV(cubeUV.west, 'bl')   // 下后
  );

  // East 面 (Bedrock +X, Three.js +X)
  // 由于Z轴反转，这里使用原本West面的几何定义（-X坐标）
  // 水平翻转UV以修正镜像问题
  addQuad(
    [-hw, -hh, hd],  getUV(cubeUV.east, 'br'),  // 下后（Bedrock 北）
    [-hw, hh, hd],   getUV(cubeUV.east, 'tr'),  // 上后
    [-hw, hh, -hd],  getUV(cubeUV.east, 'tl'),  // 上前（Bedrock 南）
    [-hw, -hh, -hd], getUV(cubeUV.east, 'bl')   // 下前
  );

  // Up 面 (Bedrock +Y, Three.js +Y)
  // 从外部（+Y方向）看向立方体，逆时针顶点顺序
  addQuad(
    [-hw, hh, hd],  getUV(cubeUV.up, 'bl'),  // 左后（Bedrock 北）
    [hw, hh, hd],   getUV(cubeUV.up, 'br'),  // 右后
    [hw, hh, -hd],  getUV(cubeUV.up, 'tr'),  // 右前（Bedrock 南）
    [-hw, hh, -hd], getUV(cubeUV.up, 'tl')   // 左前
  );

  // Down 面 (Bedrock -Y, Three.js -Y)
  // 从外部（-Y方向）看向立方体，逆时针顶点顺序
  // 需要上下翻转UV来匹配Bedrock的纹理方向
  addQuad(
    [-hw, -hh, -hd], getUV(cubeUV.down, 'tl'),  // 左前（Bedrock 南）
    [hw, -hh, -hd],  getUV(cubeUV.down, 'tr'),  // 右前
    [hw, -hh, hd],   getUV(cubeUV.down, 'br'),  // 右后（Bedrock 北）
    [-hw, -hh, hd],  getUV(cubeUV.down, 'bl')   // 左后
  );

  // North 面 (Bedrock -Z, Three.js +Z)
  // 从外部（+Z方向）看向立方体，逆时针顶点顺序
  // 水平翻转UV以修正镜像问题
  addQuad(
    [hw, -hh, hd],  getUV(cubeUV.north, 'br'),  // 右下
    [hw, hh, hd],   getUV(cubeUV.north, 'tr'),  // 右上
    [-hw, hh, hd],  getUV(cubeUV.north, 'tl'),  // 左上
    [-hw, -hh, hd], getUV(cubeUV.north, 'bl')   // 左下
  );

  // South 面 (Bedrock +Z, Three.js -Z)
  // 从外部（-Z方向）看向立方体，逆时针顶点顺序
  // 水平翻转UV以修正镜像问题
  addQuad(
    [-hw, -hh, -hd], getUV(cubeUV.south, 'br'),  // 左下
    [-hw, hh, -hd],  getUV(cubeUV.south, 'tr'),  // 左上
    [hw, hh, -hd],   getUV(cubeUV.south, 'tl'),  // 右上
    [hw, -hh, -hd],  getUV(cubeUV.south, 'bl')   // 右下
  );

  // 创建几何体
  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
  geometry.setAttribute('uv', new THREE.Float32BufferAttribute(uvs, 2));
  geometry.setIndex(indices);
  geometry.computeVertexNormals();

  return geometry;
}


function fitCameraToModel() {
  if (!modelData.value) return;

  // 计算模型的包围盒
  const box = new THREE.Box3();

  scene.children.forEach((obj) => {
    if (obj !== gridHelper && obj !== axesHelper) {
      box.expandByObject(obj);
    }
  });

  if (box.isEmpty()) {
    // 如果没有网格，使用默认位置
    camera.position.set(50, 50, 50);
    controls.target.set(0, 0, 0);
    return;
  }

  const center = box.getCenter(new THREE.Vector3());
  const size = box.getSize(new THREE.Vector3());
  const maxDim = Math.max(size.x, size.y, size.z);
  const fov = camera.fov * (Math.PI / 180);
  let cameraZ = Math.abs(maxDim / 2 / Math.tan(fov / 2));
  cameraZ *= 2.5; // 增加一些边距

  camera.position.set(center.x + cameraZ, center.y + cameraZ, center.z + cameraZ);
  controls.target.copy(center);
  controls.update();

  needsRender = true; // 标记需要重新渲染
}

function resetCamera() {
  fitCameraToModel();
}

function toggleGrid() {
  showGrid.value = !showGrid.value;
  gridHelper.visible = showGrid.value;
  needsRender = true;
}

function toggleAxes() {
  showAxes.value = !showAxes.value;
  axesHelper.visible = showAxes.value;
  needsRender = true;
}


function toggleStats() {
  showStats.value = !showStats.value;
  if (stats) {
    stats.dom.style.display = showStats.value ? 'block' : 'none';
  }
}


function cleanup() {
  if (animationId) {
    cancelAnimationFrame(animationId);
  }

  window.removeEventListener('resize', handleResize);


  if (controls) {
    controls.dispose();
  }

  if (renderer) {
    renderer.dispose();
    if (canvasContainer.value && renderer.domElement) {
      canvasContainer.value.removeChild(renderer.domElement);
    }
  }

  // 释放纹理资源
  if (currentTexture.value) {
    currentTexture.value.dispose();
    currentTexture.value = null;
  }

  clearStructureModel();
  clearModel();
}
</script>

<style scoped>
.bedrock-model-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--el-bg-color);
}

.viewer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  flex-shrink: 0;
}

.model-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.viewer-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

.performance-info {
  margin-left: 8px;
}

.canvas-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #2c2c2c;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  z-index: 100;
  color: white;
}

.loading-text {
  font-size: 14px;
}

.error-message {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80%;
  max-width: 500px;
  z-index: 10;
}

.viewer-info {
  display: flex;
  gap: 24px;
  padding: 8px 16px;
  border-top: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  flex-shrink: 0;
  font-size: 12px;
}

.info-item {
  display: flex;
  gap: 8px;
}

.info-item .label {
  color: var(--el-text-color-secondary);
}

.info-item .value {
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.texture-name {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
