<template>
  <div class="activity-bar">
    <div
      v-for="item in items"
      :key="item.id"
      class="activity-item"
      :class="{ active: activeView === item.id }"
      @click="$emit('select', item.id)"
      :title="item.title"
    >
      <el-icon :size="24">
        <component :is="item.icon" />
      </el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import {FolderOpened, Van} from '@element-plus/icons-vue';

interface ActivityItem {
  id: string;
  title: string;
  icon: any;
}

defineProps<{
  activeView: string;
}>();

defineEmits<{
  select: [id: string];
}>();

const items: ActivityItem[] = [
  { id: 'vehicles', title: '载具管理', icon: Van },
  { id: 'files', title: '文件浏览', icon: FolderOpened },
];
</script>

<style scoped>
.activity-bar {
  width: 48px;
  background: var(--el-bg-color-overlay);
  border-right: 1px solid var(--el-border-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
  gap: 4px;
}

.activity-item {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--el-text-color-secondary);
  transition: all 0.2s;
  position: relative;
}

.activity-item:hover {
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-light);
}

.activity-item.active {
  color: var(--el-color-primary);
}

.activity-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: var(--el-color-primary);
}
</style>
