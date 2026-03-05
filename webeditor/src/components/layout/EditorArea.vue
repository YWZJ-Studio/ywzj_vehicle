<template>
  <div class="editor-area">
    <!-- 载具详情视图 -->
    <VehicleDetail v-if="isVehicleTab && vehicleData" :vehicle="vehicleData" />

    <!-- 普通文件编辑器 -->
    <template v-else-if="!isVehicleTab">
      <!-- 视图切换工具栏 -->
      <div v-if="hasSchemaSupport" class="editor-toolbar">
        <el-segmented v-model="viewMode" :options="viewModeOptions" size="small" />
        <div class="toolbar-info">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ schemaTitle }}</span>
        </div>
      </div>

      <!-- 编辑器内容 -->
      <div class="editor-content">
        <KeepAlive>
          <component
            :is="currentEditor"
            :content="content"
            :path="path"
            :auto-texture="previewContext?.autoTexture"
            :auto-texture-name="previewContext?.autoTextureName"
            :auto-structure-model="previewContext?.autoStructureModel"
            :auto-structure-model-name="previewContext?.autoStructureModelName"
            @update="emit('update', $event)"
          />
        </KeepAlive>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {InfoFilled} from '@element-plus/icons-vue';
import {getFileType} from '@/utils/fileTypes';
import {findSchemaByPath} from '@/utils/schemaRegistry';
import {useVehicleStore} from '@/stores/vehicle';
import {useFileSystemStore} from '@/stores/fileSystem';
import MonacoEditor from '@/components/editors/MonacoEditor.vue';
import ImagePreview from '@/components/editors/ImagePreview.vue';
import JsonFormEditor from '@/components/editors/JsonFormEditor.vue';
import BedrockModelViewer from '@/components/editors/BedrockModelViewer.vue';
import VehicleDetail from '@/components/views/VehicleDetail.vue';

interface Props {
  content: string;
  path: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  update: [content: string];
}>();

const vehicleStore = useVehicleStore();
const fileSystemStore = useFileSystemStore();

type ViewMode = 'form' | 'code' | '3d';

const viewMode = ref<ViewMode>('form');

// 检测是否是载具 tab
const isVehicleTab = computed(() => props.path.startsWith('vehicle:'));

// 获取载具数据
const vehicleData = computed(() => {
  if (!isVehicleTab.value) return null;
  const vehicleId = props.path.replace('vehicle:', '');
  return vehicleStore.vehicles.find(v => v.id === vehicleId);
});

// 检查是否是 Bedrock 模型文件
const isBedrockModel = computed(() => {
  const fileName = props.path.split('/').pop() || '';
  const fileType = getFileType(fileName);

  // 必须是 JSON 文件且路径包含 models/bedrock
  return fileType === 'json' && props.path.includes('models/bedrock');
});

const viewModeOptions = computed(() => {
  const options = [{label: '代码视图', value: 'code'}];

  // 如果是 Bedrock 模型，添加 3D 预览选项
  if (isBedrockModel.value) {
    options.unshift({label: '3D预览', value: '3d'});
  } else {
    options.unshift({label: '表单视图', value: 'form'});
  }

  viewMode.value = options[0].value as ViewMode;
  return options;
});

// 检查是否有 Schema 支持或是 Bedrock 模型
const hasSchemaSupport = computed(() => {
  // Bedrock 模型也显示工具栏（用于切换 3D 预览）
  if (isBedrockModel.value) {
    return true;
  }

  const fileName = props.path.split('/').pop() || '';
  const fileType = getFileType(fileName);

  if (fileType !== 'json') {
    return false;
  }

  const schemaInfo = findSchemaByPath(props.path);
  return schemaInfo !== null;
});

const schemaTitle = computed(() => {
  if (isBedrockModel.value) {
    return 'Bedrock 模型';
  }
  const schemaInfo = findSchemaByPath(props.path);
  return schemaInfo?.title || '';
});

const previewContext = computed(() => {
  if (!isBedrockModel.value || viewMode.value !== '3d') return undefined;
  return fileSystemStore.openFiles.get(props.path)?.previewContext;
});

watch(() => props.path, () => {
  viewMode.value = isBedrockModel.value ? '3d' : 'form';
});

const currentEditor = computed(() => {
  const fileName = props.path.split('/').pop() || '';
  const fileType = getFileType(fileName);

  // 图片文件
  if (fileType === 'image') {
    return ImagePreview;
  }

  // JSON 文件根据视图模式选择编辑器
  if (fileType === 'json') {
    // 3D 预览模式（仅 Bedrock 模型）
    if (viewMode.value === '3d' && isBedrockModel.value) {
      return BedrockModelViewer;
    }

    // 表单视图
    if (viewMode.value === 'form' && hasSchemaSupport.value && !isBedrockModel.value) {
      return JsonFormEditor;
    }

    // 默认代码视图
    return MonacoEditor;
  }

  // 其他文本文件
  return MonacoEditor;
});
</script>

<style scoped>
.editor-area {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  flex-shrink: 0;
}

.toolbar-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.editor-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
