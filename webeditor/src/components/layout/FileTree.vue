<template>
  <div class="file-tree">
    <div class="file-tree-header">
      <span>文件浏览器</span>
    </div>
    <div class="file-tree-content">
      <el-tree
        ref="treeRef"
        :data="tree"
        :props="treeProps"
        :highlight-current="true"
        :expand-on-click-node="false"
        :current-node-key="activePath"
        node-key="path"
        @node-click="handleNodeClick"
      >
        <template #default="{ node, data }">
          <div
            class="tree-node"
            @contextmenu.prevent="handleContextMenu($event, node, data)"
          >
            <el-icon :size="16">
              <component :is="getIcon(data)" />
            </el-icon>
            <span class="node-label">{{ node.label }}</span>
          </div>
        </template>
      </el-tree>
    </div>

    <!-- Context Menu -->
    <el-dropdown
      ref="contextMenuRef"
      trigger="contextmenu"
      :virtual-triggering="true"
      :virtual-ref="triggerRef"
      @command="handleCommand"
    >
      <span></span>
      <template #dropdown>
        <el-dropdown-menu>
          <!-- 命名空间 ID 显示 -->
          <div v-if="contextNodeNamespaceId" class="namespace-id-display">
            <el-icon><Document /></el-icon>
            <span class="namespace-label">命名空间 ID:</span>
            <span class="namespace-value">{{ contextNodeNamespaceId }}</span>
          </div>
          <el-dropdown-item
            v-if="contextNode?.type === 'folder'"
            command="createFile"
            :icon="DocumentAdd"
          >
            新建文件
          </el-dropdown-item>
          <el-dropdown-item
            v-if="contextNode?.type === 'folder'"
            command="createFolder"
            :icon="FolderAdd"
          >
            新建文件夹
          </el-dropdown-item>
          <el-dropdown-item
            command="openInExplorer"
            :icon="FolderOpened"
            divided
          >
            复制路径
          </el-dropdown-item>
          <el-dropdown-item
            command="delete"
            :icon="Delete"
          >
            删除
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 新建文件对话框 -->
    <el-dialog
      v-model="createFileDialogVisible"
      title="新建文件"
      width="400px"
    >
      <el-form @submit.prevent="confirmCreateFile">
        <el-form-item label="文件名">
          <el-input
            v-model="newFileName"
            placeholder="请输入文件名"
            @keyup.enter="confirmCreateFile"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createFileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreateFile">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建文件夹对话框 -->
    <el-dialog
      v-model="createFolderDialogVisible"
      title="新建文件夹"
      width="400px"
    >
      <el-form @submit.prevent="confirmCreateFolder">
        <el-form-item label="文件夹名">
          <el-input
            v-model="newFolderName"
            placeholder="请输入文件夹名"
            @keyup.enter="confirmCreateFolder"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createFolderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreateFolder">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, ref, shallowRef, watch} from 'vue';
import {
  Delete,
  Document,
  DocumentAdd,
  Folder,
  FolderAdd,
  FolderOpened,
  Headset,
  Picture
} from '@element-plus/icons-vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import type {FileNode} from '@/types/fileSystem';
import {getFileIcon} from '@/utils/fileTypes';
import {pathToNamespaceId} from '@/utils/namespaceId';

interface Props {
  tree: FileNode[];
  activePath: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  select: [node: FileNode];
  createFile: [parentPath: string, fileName: string];
  createFolder: [parentPath: string, folderName: string];
  delete: [path: string];
  copyPath: [path: string];
}>();

const treeRef = ref();
const contextMenuRef = ref();
const triggerRef = shallowRef();
const contextNode = ref<FileNode | null>(null);

const createFileDialogVisible = ref(false);
const createFolderDialogVisible = ref(false);
const newFileName = ref('');
const newFolderName = ref('');

// 计算当前右键节点的命名空间 ID
const contextNodeNamespaceId = computed(() => {
  if (!contextNode.value) return null;
  return pathToNamespaceId(contextNode.value.path);
});

// 保存展开的节点路径
const expandedKeys = ref<string[]>([]);

// 监听 tree 变化，恢复展开状态
watch(() => props.tree, async () => {
  await nextTick();
  restoreExpandedState();
}, { deep: true });

// 保存当前展开状态
function saveExpandedState() {
  if (!treeRef.value) return;
  const store = treeRef.value.store;
  if (!store) return;

  const expanded: string[] = [];
  for (const key in store.nodesMap) {
    const node = store.nodesMap[key];
    if (node.expanded) {
      expanded.push(key);
    }
  }
  expandedKeys.value = expanded;
}

// 恢复展开状态
function restoreExpandedState() {
  if (!treeRef.value || expandedKeys.value.length === 0) return;

  expandedKeys.value.forEach(key => {
    const node = treeRef.value.getNode(key);
    if (node) {
      node.expanded = true;
    }
  });
}

const treeProps = {
  children: 'children',
  label: 'name',
};

function getIcon(data: FileNode) {
  const iconName = getFileIcon(data.name, data.type === 'folder');

  switch (iconName) {
    case 'folder':
      return Folder;
    case 'picture':
      return Picture;
    case 'headset':
      return Headset;
    default:
      return Document;
  }
}

function handleNodeClick(data: FileNode) {
  emit('select', data);
}

function handleContextMenu(event: MouseEvent, _node: any, data: FileNode) {
  event.preventDefault();
  event.stopPropagation();

  contextNode.value = data;

  // 使用虚拟触发器来显示右键菜单
  triggerRef.value = {
    getBoundingClientRect() {
      return {
        left: event.clientX,
        top: event.clientY,
        right: event.clientX,
        bottom: event.clientY,
        width: 0,
        height: 0,
      };
    },
  };

  contextMenuRef.value?.handleOpen();
}

function handleCommand(command: string) {
  if (!contextNode.value) return;

  switch (command) {
    case 'createFile':
      newFileName.value = '';
      createFileDialogVisible.value = true;
      break;
    case 'createFolder':
      newFolderName.value = '';
      createFolderDialogVisible.value = true;
      break;
    case 'copyPath':
      copyPath();
      break;
    case 'delete':
      handleDelete();
      break;
  }
}

async function copyPath() {
  if (!contextNode.value) return;
  emit('copyPath', contextNode.value.path);
}

async function confirmCreateFile() {
  if (!newFileName.value.trim()) {
    ElMessage.warning('请输入文件名');
    return;
  }

  if (!contextNode.value) return;

  const parentPath = contextNode.value.type === 'folder'
    ? contextNode.value.path
    : contextNode.value.path.substring(0, contextNode.value.path.lastIndexOf('/'));

  // 保存展开状态
  saveExpandedState();

  emit('createFile', parentPath, newFileName.value.trim());
  createFileDialogVisible.value = false;
  newFileName.value = '';
}

async function confirmCreateFolder() {
  if (!newFolderName.value.trim()) {
    ElMessage.warning('请输入文件夹名');
    return;
  }

  if (!contextNode.value) return;

  const parentPath = contextNode.value.type === 'folder'
    ? contextNode.value.path
    : contextNode.value.path.substring(0, contextNode.value.path.lastIndexOf('/'));

  // 保存展开状态
  saveExpandedState();

  emit('createFolder', parentPath, newFolderName.value.trim());
  createFolderDialogVisible.value = false;
  newFolderName.value = '';
}

async function handleDelete() {
  if (!contextNode.value) return;

  try {
    await ElMessageBox.confirm(
      `确定要删除 "${contextNode.value.name}" 吗？${contextNode.value.type === 'folder' ? '（包含所有子文件和文件夹）' : ''}`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      }
    );

    // 保存展开状态
    saveExpandedState();

    emit('delete', contextNode.value.path);
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.file-tree {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.file-tree-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color);
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
  flex-shrink: 0;
}

.file-tree-content {
  flex: 1;
  overflow: auto;
  padding: 8px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  padding: 4px 0;
}

.node-label {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.el-tree-node__content) {
  height: 32px;
  border-radius: 4px;
}

:deep(.el-tree-node__content:hover) {
  background-color: var(--el-fill-color-light);
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.namespace-id-display {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  margin-bottom: 4px;
  background-color: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 12px;
}

.namespace-label {
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.namespace-value {
  color: var(--el-color-primary);
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-weight: 600;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
