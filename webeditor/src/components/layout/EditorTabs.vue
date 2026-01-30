<template>
  <div class="editor-tabs">
    <div class="tabs-container">
      <div
        v-for="tab in tabs"
        :key="tab.path"
        :class="['tab-item', { active: tab.path === active }]"
        @click="emit('select', tab.path)"
      >
        <span class="tab-name">
          <span v-if="tab.modified" class="modified-indicator">●</span>
          {{ tab.name }}
        </span>
        <el-icon
          class="tab-close"
          @click.stop="emit('close', tab.path)"
        >
          <Close />
        </el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {Close} from '@element-plus/icons-vue';

interface Tab {
  path: string;
  name: string;
  modified: boolean;
}

interface Props {
  tabs: Tab[];
  active: string;
}

defineProps<Props>();
const emit = defineEmits<{
  select: [path: string];
  close: [path: string];
}>();
</script>

<style scoped>
.editor-tabs {
  height: var(--tab-height);
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  flex-shrink: 0;
}

.tabs-container {
  display: flex;
  height: 100%;
  overflow-x: auto;
  overflow-y: hidden;
}

.tabs-container::-webkit-scrollbar {
  height: 4px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  min-width: 120px;
  max-width: 200px;
  cursor: pointer;
  border-right: 1px solid var(--el-border-color);
  background: var(--el-fill-color-lighter);
  transition: background-color 0.2s;
  user-select: none;
}

.tab-item:hover {
  background: var(--el-fill-color-light);
}

.tab-item.active {
  background: var(--el-bg-color);
  border-bottom: 2px solid var(--el-color-primary);
}

.tab-name {
  flex: 1;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: center;
  gap: 4px;
}

.modified-indicator {
  color: var(--el-color-warning);
  font-size: 16px;
  line-height: 1;
}

.tab-close {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.tab-close:hover {
  color: var(--el-text-color-primary);
  background: var(--el-fill-color);
  border-radius: 2px;
}
</style>
