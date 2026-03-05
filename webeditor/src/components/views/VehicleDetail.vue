<template>
  <div class="vehicle-detail">
    <div class="detail-header">
      <h3>{{ vehicle.name }}</h3>
      <el-tag size="small">{{ vehicle.id }}</el-tag>
    </div>

    <div class="detail-content">
      <div class="file-section">
        <h4>配置文件</h4>
        <div class="file-list">
          <div v-if="vehicle.dataFile" class="file-item" @click="openFile(vehicle.dataFile)">
            <el-icon><Document /></el-icon>
            <span>Data 文件</span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
          <div v-if="vehicle.displayFile" class="file-item" @click="openFile(vehicle.displayFile)">
            <el-icon><Document /></el-icon>
            <span>Display 文件</span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
          <div v-if="!vehicle.dataFile && !vehicle.displayFile" class="empty-hint">
            暂无配置文件
          </div>
        </div>
      </div>

      <div class="file-section" v-if="vehicle.models.length > 0">
        <h4>模型文件 ({{ vehicle.models.length }})</h4>
        <div class="file-list">
          <div v-for="model in vehicle.models" :key="model" class="file-item" @click="openFile(model)">
            <el-icon><Box /></el-icon>
            <span>{{ getFileName(model) }}</span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <div class="file-section" v-if="vehicle.textures.length > 0">
        <h4>贴图文件 ({{ vehicle.textures.length }})</h4>
        <div class="file-list">
          <div v-for="texture in vehicle.textures" :key="texture" class="file-item" @click="openFile(texture)">
            <el-icon><Picture /></el-icon>
            <span>{{ getFileName(texture) }}</span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <div class="actions">
        <el-button type="primary" @click="openAllFiles">打开所有配置文件</el-button>
        <el-button type="success" :disabled="!vehicle.displayFile" @click="previewVehicleModel">预览模型</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ElMessage} from 'element-plus';
import {ArrowRight, Box, Document, Picture} from '@element-plus/icons-vue';
import {useFileSystemStore} from '@/stores/fileSystem';
import {parseJsonWithComments} from '@/utils/jsonParser';
import {globalNamespaceIdProvider} from '@/utils/namespaceIdCompletion';
import type {ModelPreviewContext} from '@/types/fileSystem';
import type {Vehicle} from '@/types/vehicle';

const props = defineProps<{
  vehicle: Vehicle;
}>();

const fileSystemStore = useFileSystemStore();

function getFileName(path: string): string {
  return path.split('/').pop() || path;
}

async function openFile(path: string) {
  const node = findFileNode(fileSystemStore.fileTree, path);
  if (node?.handle) {
    await fileSystemStore.openFile(path, node.handle as FileSystemFileHandle);
  }
}

async function openAllFiles() {
  const files = [props.vehicle.dataFile, props.vehicle.displayFile].filter(Boolean) as string[];
  for (const path of files) {
    await openFile(path);
  }
}

async function previewVehicleModel() {
  if (!props.vehicle.displayFile) {
    ElMessage.warning('未找到 display 文件');
    return;
  }

  const displayText = await readTextFile(props.vehicle.displayFile);
  if (!displayText) {
    ElMessage.error('读取 display 文件失败');
    return;
  }

  let displayData: any;
  try {
    displayData = parseJsonWithComments(displayText);
  } catch (err: any) {
    ElMessage.error(`display 文件解析失败: ${err.message || err}`);
    return;
  }

  const modelRef = pickString(displayData, [
    'model',
    'display.model',
    'vehicle_display.model',
    'vehicle.display.model',
  ]);
  const textureRef = pickString(displayData, [
    'texture',
    'display.texture',
    'vehicle_display.texture',
    'vehicle.display.texture',
  ]);

  if (!modelRef) {
    ElMessage.error('display 文件中未找到 model 字段');
    return;
  }

  const modelPath = resolveResourcePath(modelRef, 'model');
  if (!modelPath) {
    ElMessage.error(`无法解析模型路径: ${modelRef}`);
    return;
  }

  let structureRef: string | undefined;
  if (props.vehicle.dataFile) {
    const dataText = await readTextFile(props.vehicle.dataFile);
    if (dataText) {
      try {
        const data = parseJsonWithComments(dataText);
        structureRef = pickString(data, ['structure_model', 'vehicle.structure_model']);
      } catch {
        // ignore data parse errors for model preview
      }
    }
  }

  const texturePath = textureRef ? resolveResourcePath(textureRef, 'texture') : undefined;
  const structurePath = structureRef ? resolveResourcePath(structureRef, 'structure') : undefined;

  let autoTexture: string | undefined;
  if (texturePath) {
    autoTexture = await readImageAsDataUrl(texturePath);
  } else if (textureRef) {
    ElMessage.warning(`未解析到贴图路径: ${textureRef}`);
  }

  let autoStructureModel: string | undefined;
  if (structurePath) {
    autoStructureModel = await readTextFile(structurePath);
  } else if (structureRef) {
    ElMessage.warning(`未解析到结构模型路径: ${structureRef}`);
  }

  const previewContext: ModelPreviewContext = {
    autoTexture,
    autoTextureName: texturePath ? getFileName(texturePath) : undefined,
    autoStructureModel,
    autoStructureModelName: structurePath ? getFileName(structurePath) : undefined,
  };

  fileSystemStore.setModelPreviewContext(modelPath, previewContext);
  await openFile(modelPath);
  ElMessage.success('模型预览已打开');
}

function readNestedString(obj: any, path: string): string | undefined {
  const parts = path.split('.');
  let current = obj;

  for (const part of parts) {
    if (!current || typeof current !== 'object') return undefined;
    current = current[part];
  }

  return typeof current === 'string' ? current : undefined;
}

function pickString(obj: any, paths: string[]): string | undefined {
  for (const path of paths) {
    const value = readNestedString(obj, path);
    if (value) return value;
  }
  return undefined;
}

function resolveResourcePath(
  rawValue: string,
  type: 'model' | 'texture' | 'structure'
): string | undefined {
  const value = rawValue.trim();
  if (!value) return undefined;

  if ((value.startsWith('assets/') || value.startsWith('data/')) && fileExists(value)) {
    return value;
  }

  const completionTypes = type === 'texture'
    ? [{packType: 'assets' as const, category: 'textures'}]
    : [
      {packType: 'assets' as const, category: 'models/bedrock'},
      {packType: 'data' as const, category: 'models/bedrock'},
    ];

  for (const itemType of completionTypes) {
    const items = globalNamespaceIdProvider.getCompletionsByType(itemType.packType, itemType.category);
    const found = items.find(item => item.namespaceId === value);
    if (found) {
      return found.filePath;
    }
  }

  const [namespace, relRaw] = value.split(':');
  if (!namespace || !relRaw) return undefined;

  if (type === 'texture') {
    const rel = relRaw.startsWith('textures/') ? relRaw : `textures/${relRaw}`;
    const candidates = [
      `assets/${namespace}/${rel}`,
      `assets/${namespace}/${rel}.png`,
      `assets/${namespace}/${rel}.jpg`,
      `assets/${namespace}/${rel}.jpeg`,
    ];
    return candidates.find(fileExists);
  }

  const relNoExt = relRaw
    .replace(/^models\/bedrock\//, '')
    .replace(/\.json$/, '');
  const candidates = [
    `assets/${namespace}/models/bedrock/${relNoExt}.json`,
    `data/${namespace}/models/bedrock/${relNoExt}.json`,
  ];
  return candidates.find(fileExists);
}

function fileExists(path: string): boolean {
  return !!findFileNode(fileSystemStore.fileTree, path);
}

async function readTextFile(path: string): Promise<string | undefined> {
  const opened = fileSystemStore.openFiles.get(path);
  if (opened?.content) return opened.content;

  const node = findFileNode(fileSystemStore.fileTree, path);
  if (!node?.handle) return undefined;

  const file = await (node.handle as FileSystemFileHandle).getFile();
  return await file.text();
}

async function readImageAsDataUrl(path: string): Promise<string | undefined> {
  const opened = fileSystemStore.openFiles.get(path);
  if (opened?.content?.startsWith('data:image/')) return opened.content;

  const node = findFileNode(fileSystemStore.fileTree, path);
  if (!node?.handle) return undefined;

  const file = await (node.handle as FileSystemFileHandle).getFile();
  return await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

function findFileNode(nodes: any[], targetPath: string, currentPath = ''): any {
  for (const node of nodes) {
    const path = currentPath ? `${currentPath}/${node.name}` : node.name;
    if (path === targetPath) return node;
    if (node.children) {
      const found = findFileNode(node.children, targetPath, path);
      if (found) return found;
    }
  }
  return null;
}
</script>

<style scoped>
.vehicle-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}

.detail-header {
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color);
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  flex: 1;
}

.detail-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.file-section {
  margin-bottom: 24px;
}

.file-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--el-border-color-lighter);
}

.file-item:hover {
  background: var(--el-fill-color-light);
  border-color: var(--el-color-primary);
}

.file-item span {
  flex: 1;
  font-size: 13px;
}

.file-item .arrow {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.empty-hint {
  padding: 12px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  gap: 8px;
}
</style>
