<template>
  <div class="namespace-id-select">
    <el-autocomplete
      :model-value="modelValue ?? ''"
      @update:model-value="handleUpdate"
      :fetch-suggestions="querySearch"
      :placeholder="placeholder"
      :trigger-on-focus="true"
      clearable
      style="width: 100%"
    >
      <template #default="{ item }">
        <div class="namespace-option">
          <span class="namespace-id">{{ item.value }}</span>
          <span class="namespace-detail">{{ item.detail }}</span>
        </div>
      </template>
    </el-autocomplete>
    <el-button
      v-if="resourceType"
      :icon="FolderOpened"
      :disabled="!modelValue || !fileExists"
      size="small"
      @click="openFile"
      title="打开文件"
    />
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {ElMessage} from 'element-plus';
import {FolderOpened} from '@element-plus/icons-vue';
import {useFileSystemStore} from '@/stores/fileSystem';
import {globalNamespaceIdProvider} from '@/utils/namespaceIdCompletion';

interface Props {
  modelValue?: string;
  packType?: 'data' | 'assets';
  category?: string;
  fieldName?: string;
  placeholder?: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const fileSystemStore = useFileSystemStore();

// 推断资源类型
const resourceType = computed<{ packType: 'data' | 'assets', category: string } | null>(() => {
  if (props.packType && props.category) {
    return { packType: props.packType, category: props.category };
  }
  return null;
});

// 直接从 provider 读取缓存，文件树变化由 store 统一触发 provider 更新
const suggestions = computed(() => {
  if (!resourceType.value) return [];
  // 依赖 fileTree 使 computed 在文件树变化时重新求值
  void fileSystemStore.fileTree;
  return globalNamespaceIdProvider
    .getCompletionsByType(resourceType.value.packType, resourceType.value.category)
    .map(item => ({ value: item.namespaceId, label: item.namespaceId, detail: item.detail || '' }));
});

const fileExists = computed(() =>
  !!props.modelValue && suggestions.value.some(item => item.value === props.modelValue)
);

// autocomplete 查询函数
function querySearch(queryString: string, cb: (results: any[]) => void) {
  if (!queryString) {
    cb(suggestions.value);
    return;
  }
  const lowerQuery = queryString.toLowerCase();
  cb(suggestions.value.filter(item => item.value.toLowerCase().includes(lowerQuery)));
}

function handleUpdate(value: string) {
  emit('update:modelValue', value);
}

function findFileNode(nodes: any[], targetPath: string): any {
  for (const node of nodes) {
    if (node.path === targetPath) return node;
    if (node.children) {
      const found = findFileNode(node.children, targetPath);
      if (found) return found;
    }
  }
  return null;
}

async function openFile() {
  if (!props.modelValue || !resourceType.value) return;
  const items = globalNamespaceIdProvider.getCompletionsByType(
    resourceType.value.packType,
    resourceType.value.category
  );
  const item = items.find(i => i.namespaceId === props.modelValue);
  if (!item?.filePath) {
    ElMessage.error(`找不到文件：${props.modelValue}`);
    return;
  }
  const node = findFileNode(fileSystemStore.fileTree, item.filePath);
  if (!node?.handle) {
    ElMessage.error(`无法打开文件：${item.filePath}`);
    return;
  }
  try {
    await fileSystemStore.openFile(item.filePath, node.handle as FileSystemFileHandle);
  } catch {
    ElMessage.error(`打开文件失败：${item.filePath}`);
  }
}
</script>

<style scoped>
.namespace-id-select {
  display: flex;
  gap: 4px;
  align-items: center;
  width: 100%;
}

.namespace-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.namespace-id {
  flex: 1;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.namespace-detail {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-left: 8px;
  white-space: nowrap;
}
</style>
