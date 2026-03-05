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
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ArrowRight, Box, Document, Picture} from '@element-plus/icons-vue';
import {useFileSystemStore} from '@/stores/fileSystem';
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
}
</style>
