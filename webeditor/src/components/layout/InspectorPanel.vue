<template>
  <div class="inspector-panel">
    <!-- Tab bar — only shown for animation controllers -->
    <div v-if="isAnimController" class="inspector-tabs">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'file' }"
        @click="activeTab = 'file'"
      >文件属性</button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'node' }"
        @click="activeTab = 'node'"
      >节点属性</button>
    </div>
    <div v-else class="inspector-header">
      <span>属性</span>
    </div>

    <!-- Node properties tab -->
    <div v-if="isAnimController && activeTab === 'node'" class="node-props-wrapper">
      <NodePropertiesPanel
        :selection="acSelection"
        :root="acRoot"
        @patch="handlePatch"
      />
    </div>

    <!-- File properties tab (always shown for non-AC files, or when tab=file) -->
    <div v-show="!isAnimController || activeTab === 'file'" class="inspector-content">
      <!-- 命名空间 ID 信息 -->
      <div v-if="namespaceId" class="info-card namespace-card">
        <div class="card-header">
          <el-icon><Document /></el-icon>
          <span>命名空间 ID</span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span class="label">ID</span>
            <span class="value namespace-id-value">{{ namespaceId }}</span>
          </div>
          <div class="info-row" v-if="namespace">
            <span class="label">命名空间</span>
            <span class="value">{{ namespace }}</span>
          </div>
          <div class="info-row" v-if="resourceCategory">
            <span class="label">资源类型</span>
            <span class="value">{{ resourceCategory }}</span>
          </div>
        </div>
      </div>

      <!-- 文件信息 -->
      <div class="info-card">
        <div class="card-header">
          <el-icon><Document /></el-icon>
          <span>文件信息</span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span class="label">路径</span>
            <span class="value file-path">{{ path }}</span>
          </div>
          <div class="info-row">
            <span class="label">类型</span>
            <span class="value">{{ fileType }}</span>
          </div>
          <div class="info-row">
            <span class="label">大小</span>
            <span class="value">{{ formatSize(content.length) }}</span>
          </div>
          <div class="info-row" v-if="isTextFile">
            <span class="label">行数</span>
            <span class="value">{{ lineCount }}</span>
          </div>
        </div>
      </div>

      <!-- Schema 信息 -->
      <div v-if="schemaInfo" class="info-card">
        <div class="card-header">
          <el-icon><Document /></el-icon>
          <span>Schema 验证</span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span class="label">Schema</span>
            <span class="value">{{ schemaInfo.title }}</span>
          </div>
          <div class="info-row">
            <span class="label">状态</span>
            <span class="value">
              <el-tag v-if="validationResult?.valid" type="success" size="small">
                <el-icon><CircleCheck /></el-icon>
                验证通过
              </el-tag>
              <el-tag v-else type="danger" size="small">
                <el-icon><CircleClose /></el-icon>
                {{ validationResult?.errors?.length || 0 }} 个错误
              </el-tag>
            </span>
          </div>
        </div>

        <!-- 验证错误列表 -->
        <div v-if="validationResult && !validationResult.valid" class="error-list">
          <div class="error-list-header">
            <el-icon><WarningFilled /></el-icon>
            <span>验证错误</span>
          </div>
          <el-alert
            v-for="(error, idx) in validationResult.errors"
            :key="idx"
            :title="error.field"
            type="error"
            :closable="false"
            class="error-item"
          >
            {{ error.message }}
          </el-alert>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {CircleCheck, CircleClose, Document, WarningFilled} from '@element-plus/icons-vue';
import {formatFileSize, getFileType} from '@/utils/fileTypes';
import {findSchemaByPath} from '@/utils/schemaRegistry';
import {validateJsonString, type ValidationResult} from '@/utils/validator';
import {getNamespace, getResourceCategory, pathToNamespaceId} from '@/utils/namespaceId';
import {isAnimationControllerFile, parseAnimationControllerContent} from '@/utils/animationControllerGraph';
import {useAnimationControllerEditorStore} from '@/stores/animationControllerEditor';
import NodePropertiesPanel from '@/components/editors/animation-controller/NodePropertiesPanel.vue';

interface Props {
  content: string;
  path: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{ update: [content: string] }>();

const validationResult = ref<ValidationResult | null>(null);
const activeTab = ref<'file' | 'node'>('file');

const fileName = computed(() => props.path.split('/').pop() || '');
const fileType = computed(() => getFileType(fileName.value));
const isTextFile = computed(() => ['json', 'text'].includes(fileType.value));

// 命名空间相关信息
const namespaceId = computed(() => pathToNamespaceId(props.path));
const namespace = computed(() => getNamespace(props.path));
const resourceCategory = computed(() => getResourceCategory(props.path));

const lineCount = computed(() => {
  if (!isTextFile.value) return 0;
  // 大文件不计算行数，避免split操作
  if (props.content.length > 500 * 1024) {
    return '(文件过大，跳过统计)';
  }
  return props.content.split('\n').length;
});

const schemaInfo = computed(() => {
  if (fileType.value !== 'json') return null;
  return findSchemaByPath(props.path);
});

// 监听内容变化，自动验证
watch(() => props.content, () => {
  validateContent();
}, { immediate: true });

watch(() => props.path, () => {
  validateContent();
  activeTab.value = 'file';
});

function validateContent() {
  if (fileType.value !== 'json' || !schemaInfo.value) {
    validationResult.value = null;
    return;
  }

  validationResult.value = validateJsonString(props.path, props.content);
}

const formatSize = (bytes: number) => formatFileSize(bytes);

// ── Animation controller integration ─────────────────────────────────────────

const isAnimController = computed(() => isAnimationControllerFile(props.path));

const acEditorStore = useAnimationControllerEditorStore();

const acSelection = computed(() =>
  isAnimController.value ? acEditorStore.getContext(props.path).selection : null,
);

const acRoot = computed(() => {
  if (!isAnimController.value) return {};
  try {
    return parseAnimationControllerContent(props.content);
  } catch {
    return {};
  }
});

// Auto-switch to node tab when something is selected
watch(acSelection, (sel) => {
  if (sel && isAnimController.value) {
    activeTab.value = 'node';
  }
});

function handlePatch(updates: Record<string, any>) {
  // Import patchAnimationControllerSource lazily to avoid circular dep issues
  import('@/utils/animationControllerGraph').then(({ patchAnimationControllerSource }) => {
    const patched = patchAnimationControllerSource(props.content, updates);
    emit('update', patched);
  });
}
</script>

<style scoped>
.inspector-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background-color: var(--el-bg-color);
}

.inspector-tabs {
  display: flex;
  border-bottom: 1px solid var(--el-border-color);
  flex-shrink: 0;
  background-color: var(--el-bg-color);
}

.tab-btn {
  flex: 1;
  padding: 10px 8px;
  font-size: 13px;
  font-weight: 500;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--el-text-color-secondary);
  border-bottom: 2px solid transparent;
  transition: color 0.15s, border-color 0.15s;
}

.tab-btn.active {
  color: var(--el-color-primary);
  border-bottom-color: var(--el-color-primary);
}

.inspector-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color);
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
  flex-shrink: 0;
  background-color: var(--el-bg-color);
}

.inspector-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 卡片样式 */
.info-card {
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
}

.namespace-card {
  border-color: var(--el-color-primary-light-5);
  background-color: var(--el-color-primary-light-9);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background-color: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-weight: 600;
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.namespace-card .card-header {
  background-color: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
}

.card-body {
  padding: 8px 12px;
}

.info-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  flex-shrink: 0;
  width: 80px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.info-row .value {
  flex: 1;
  font-size: 13px;
  color: var(--el-text-color-primary);
  word-break: break-all;
  line-height: 1.5;
}

.file-path {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
}

.namespace-id-value {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-weight: 600;
  color: var(--el-color-primary);
  font-size: 13px;
}

/* 错误列表 */
.error-list {
  border-top: 1px solid var(--el-border-color-lighter);
  background-color: var(--el-color-danger-light-9);
}

.error-list-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-color-danger);
  background-color: var(--el-color-danger-light-8);
}

.error-item {
  margin: 8px 12px;
}

.error-item:last-child {
  margin-bottom: 12px;
}

.error-item :deep(.el-alert__title) {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 4px;
}

.error-item :deep(.el-alert__description) {
  font-size: 12px;
}

.node-props-wrapper {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.node-props-wrapper :deep(.node-props) {
  height: auto;
  overflow-y: visible;
}
</style>
