<template>
  <el-autocomplete
    :model-value="modelValue"
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
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {useFileSystemStore} from '@/stores/fileSystem';
import {globalNamespaceIdProvider} from '@/utils/namespaceIdCompletion';

interface Props {
  modelValue: string;
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
const suggestions = ref<Array<{ value: string; label: string; detail: string }>>([]);

// 用于防止重复加载
const isLoading = ref(false);
const lastLoadKey = ref<string>('');

// 推断资源类型
const resourceType = computed<{ packType: 'data' | 'assets', category: string } | null>(() => {
  if (props.packType && props.category) {
    return { packType: props.packType, category: props.category };
  }
  return null;
});


// 加载补全建议
async function loadSuggestions() {
  if (!resourceType.value) {
    suggestions.value = [];
    return;
  }

  // 生成一个唯一的加载键，用于检测是否需要重新加载
  const loadKey = `${resourceType.value.packType}:${resourceType.value.category}:${fileSystemStore.fileTree.length}`;

  if (isLoading.value || lastLoadKey.value === loadKey) {
    return;
  }

  // 标记为正在加载
  isLoading.value = true;
  lastLoadKey.value = loadKey;

  try {
    // 更新文件树数据
    globalNamespaceIdProvider.updateFileTree(fileSystemStore.fileTree);

    const items = globalNamespaceIdProvider.getCompletionsByType(
      resourceType.value.packType,
      resourceType.value.category
    );

    // 转换为下拉选项格式
    suggestions.value = items.map(item => ({
      value: item.namespaceId,
      label: item.namespaceId,
      detail: item.detail || ''
    }));
  } catch (err) {
    console.error('[NamespaceIdSelect] Error loading suggestions:', err);
    suggestions.value = [];
  } finally {
    // 解除加载状态
    isLoading.value = false;
  }
}

// autocomplete 查询函数
function querySearch(queryString: string, cb: (results: any[]) => void) {
  if (!queryString) {
    // 如果查询字符串为空，返回所有建议
    cb(suggestions.value);
    return;
  }

  // 过滤匹配的建议
  const lowerQuery = queryString.toLowerCase();
  const results = suggestions.value.filter(item =>
    item.value.toLowerCase().includes(lowerQuery)
  );

  cb(results);
}

// 处理值更新
function handleUpdate(value: string) {
  emit('update:modelValue', value);
}

// 监听文件树变化
watch(() => fileSystemStore.fileTree, () => {
  loadSuggestions();
}, { deep: true });

// 监听资源类型变化（immediate: true 会在组件挂载时立即执行一次）
watch(resourceType, () => {
  loadSuggestions();
}, { immediate: true });
</script>

<style scoped>
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
