<template>
  <div class="image-preview">
    <div class="preview-toolbar">
      <span class="image-info">{{ imageInfo }}</span>
      <el-button-group>
        <el-button size="small" :icon="ZoomOut" @click="zoomOut" />
        <el-button size="small" @click="resetZoom">{{ Math.round(zoom * 100) }}%</el-button>
        <el-button size="small" :icon="ZoomIn" @click="zoomIn" />
      </el-button-group>
    </div>
    <div class="preview-content" @wheel="handleWheel">
      <img
        v-if="imageUrl"
        :src="imageUrl"
        :alt="path"
        :style="{ transform: `scale(${zoom})` }"
        draggable="false"
        @load="handleImageLoad"
        @dragstart.prevent
      />
      <div v-else class="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref, watch} from 'vue';
import {Loading, ZoomIn, ZoomOut} from '@element-plus/icons-vue';

interface Props {
  content: string;
  path: string;
}

const props = defineProps<Props>();

const imageUrl = ref('');
const zoom = ref(1);
const imageInfo = ref('');

onMounted(() => {
  // Content is already a data URL from fileSystemManager.readFileAsDataURL
  imageUrl.value = props.content;
});

// Watch for content changes (e.g., when switching between files)
watch(() => props.content, (newContent) => {
  imageUrl.value = newContent;
});

function handleImageLoad(event: Event) {
  const img = event.target as HTMLImageElement;
  imageInfo.value = `${img.naturalWidth} × ${img.naturalHeight}`;
}

function zoomIn() {
  zoom.value = Math.min(zoom.value + 0.1, 5);
}

function zoomOut() {
  zoom.value = Math.max(zoom.value - 0.1, 0.1);
}

function resetZoom() {
  zoom.value = 1;
}

function handleWheel(event: WheelEvent) {
  if (event.ctrlKey || event.metaKey) {
    event.preventDefault();
    const delta = -event.deltaY * 0.001;
    zoom.value = Math.max(0.1, Math.min(5, zoom.value + delta));
  }
}
</script>

<style scoped>
.image-preview {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--el-bg-color);
}

.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  flex-shrink: 0;
}

.image-info {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.preview-content {
  flex: 1;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image:
    linear-gradient(45deg, #f0f0f0 25%, transparent 25%),
    linear-gradient(-45deg, #f0f0f0 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, #f0f0f0 75%),
    linear-gradient(-45deg, transparent 75%, #f0f0f0 75%);
  background-size: 20px 20px;
  background-position: 0 0, 0 10px, 10px -10px, -10px 0px;
}

.preview-content img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.1s;
  cursor: grab;
}

.preview-content img:active {
  cursor: grabbing;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--el-text-color-secondary);
}

.loading .el-icon {
  font-size: 32px;
}
</style>
