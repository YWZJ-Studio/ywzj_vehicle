<template>
  <div class="vehicle-pack-editor" @keydown="handleGlobalKeyDown" @contextmenu="handleContextMenu">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button
          type="primary"
          :icon="FolderOpened"
          @click="handleOpenFolder"
          :loading="loading"
        >
          打开载具包文件夹
        </el-button>

        <el-button
          v-if="hasOpenFolder"
          :icon="RefreshRight"
          @click="handleRefresh"
        >
          刷新
        </el-button>
      </div>

      <div class="toolbar-right" v-if="hasOpenFolder">
        <el-tag v-if="hasUnsavedChanges" type="warning">
          <el-icon><Warning /></el-icon>
          有未保存的更改
        </el-tag>

        <el-button
          :icon="DocumentCopy"
          @click="handleSaveAll"
          :disabled="!hasUnsavedChanges"
        >
          保存全部
        </el-button>
      </div>
    </div>

    <div v-if="hasOpenFolder" class="editor-workspace">
      <div class="main-layout">
        <ActivityBar :active-view="activeView" @select="activeView = $event" />

        <div class="left-panel" :style="{ width: leftPanelWidth + 'px' }">
          <VehicleExplorer v-if="activeView === 'vehicles'" />
          <FileTree
            v-else
            :tree="fileTree"
            :active-path="activeFilePath"
            @select="handleFileSelect"
            @create-file="handleCreateFile"
            @create-folder="handleCreateFolder"
            @delete="handleDelete"
            @copy-path="copyPath"
          />
        </div>

        <div
          class="resize-handle-col"
          @mousedown="startResize('left', $event)"
        ></div>

        <div class="center-panel">
          <EditorTabs
            v-if="openFileTabs.length > 0"
            :tabs="openFileTabs"
            :active="activeFilePath"
            @select="setActiveFile"
            @close="closeFile"
          />

          <KeepAlive :max="10">
            <EditorArea
              v-if="activeFile"
              :key="activeFilePath"
              :content="activeFile.content"
              :path="activeFilePath"
              @update="handleContentUpdate"
            />
          </KeepAlive>

          <div v-if="!activeFile" class="empty-state">
            <el-empty description="请从左侧选择一个文件开始编辑" />
          </div>
        </div>

        <div
          class="resize-handle-col"
          @mousedown="startResize('right', $event)"
        ></div>

        <div class="right-panel" :style="{ width: rightPanelWidth + 'px' }">
          <InspectorPanel
            v-if="activeFile"
            :content="activeFile.content"
            :path="activeFilePath"
          />
          <div v-else class="empty-state">
            <el-empty description="未选择文件" />
          </div>
        </div>
      </div>
    </div>

    <div v-else class="welcome-screen">
      <el-empty description="请打开载具包文件夹开始编辑">
        <el-button type="primary" size="large" :icon="FolderOpened" @click="handleOpenFolder">
          打开载具包文件夹
        </el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onBeforeUnmount, ref} from 'vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {DocumentCopy, FolderOpened, RefreshRight, Warning} from '@element-plus/icons-vue';
import {useFileSystemStore} from '@/stores/fileSystem';
import {useVehicleStore} from '@/stores/vehicle';
import ActivityBar from '@/components/layout/ActivityBar.vue';
import VehicleExplorer from '@/components/views/VehicleExplorer.vue';
import FileTree from '@/components/layout/FileTree.vue';
import EditorTabs from '@/components/layout/EditorTabs.vue';
import EditorArea from '@/components/layout/EditorArea.vue';
import InspectorPanel from '@/components/layout/InspectorPanel.vue';
import type {FileNode} from '@/types/fileSystem';

const fileSystemStore = useFileSystemStore();
const vehicleStore = useVehicleStore();

const activeView = ref('vehicles');

const leftPanelWidth = ref(250);
const rightPanelWidth = ref(300);
const bottomPanelHeight = ref(200);

const loading = computed(() => fileSystemStore.loading);
const hasOpenFolder = computed(() => fileSystemStore.hasOpenFolder);
const hasUnsavedChanges = computed(() => fileSystemStore.hasUnsavedChanges);
const fileTree = computed(() => fileSystemStore.fileTree);
const activeFilePath = computed(() => fileSystemStore.activeFilePath);
const activeFile = computed(() => fileSystemStore.activeFile);
const openFileTabs = computed(() => fileSystemStore.openFileTabs);

async function handleOpenFolder() {
  try {
    // 检查是否有未保存的更改
    if (fileSystemStore.hasUnsavedChanges) {
      const unsavedFiles = fileSystemStore.getUnsavedFiles();
      const fileList = unsavedFiles.map(file => `• ${file}`).join('\n');

      try {
        await ElMessageBox.confirm(
          `以下文件有未保存的更改：\n\n${fileList}\n\n打开新的载具包将丢失这些更改，是否继续？`,
          '警告：有未保存的更改',
          {
            confirmButtonText: '继续打开',
            cancelButtonText: '取消',
            type: 'warning',
            dangerouslyUseHTMLString: false,
          }
        );
      } catch {
        // 用户取消
        console.log('[App] 用户取消打开新载具包');
        return;
      }
    }

    // 跳过提示，直接打开
    const success = await fileSystemStore.openFolder(true);
    if (success) {
      vehicleStore.refreshVehicles();
      ElMessage.success('打开成功');
    }
  } catch (err: any) {
    ElMessage.error('打开文件夹失败：' + err.message);
  }
}

async function handleRefresh() {
  await fileSystemStore.refreshFileTree();
  vehicleStore.refreshVehicles();
  ElMessage.success('文件树已刷新');
}

async function handleFileSelect(node: FileNode) {
  if (node.type === 'file') {
    try {
      await fileSystemStore.openFile(node.path, node.handle as FileSystemFileHandle);
    } catch (err: any) {
      ElMessage.error('无法打开文件: ' + (err.message || err));
      console.error('Failed to open file:', err);
    }
  }
}

function handleContentUpdate(content: string) {
  fileSystemStore.updateFileContent(activeFilePath.value, content);
}

async function handleSaveAll() {
  await fileSystemStore.saveAllFiles();
  ElMessage.success('所有文件已保存');
}

async function handleSaveCurrentFile() {
  if (!activeFilePath.value) {
    ElMessage.warning('没有打开的文件');
    return;
  }

  try {
    await fileSystemStore.saveFile(activeFilePath.value);
    const fileName = activeFilePath.value.split('/').pop() || activeFilePath.value;
    ElMessage.success(`${fileName} 已保存`);
  } catch (err: any) {
    ElMessage.error('保存失败：' + (err.message || err));
  }
}

function setActiveFile(path: string) {
  fileSystemStore.setActiveFile(path);
}

async function handleCreateFile(parentPath: string, fileName: string) {
  try {
    await fileSystemStore.createFile(parentPath, fileName);
    ElMessage.success(`文件 "${fileName}" 创建成功`);
  } catch (err: any) {
    ElMessage.error('创建文件失败：' + (err.message || err));
  }
}

async function handleCreateFolder(parentPath: string, folderName: string) {
  try {
    await fileSystemStore.createFolder(parentPath, folderName);
    ElMessage.success(`文件夹 "${folderName}" 创建成功`);
  } catch (err: any) {
    ElMessage.error('创建文件夹失败：' + (err.message || err));
  }
}

async function handleDelete(path: string) {
  try {
    await fileSystemStore.deleteFileOrFolder(path);
    const name = path.split('/').pop() || path;
    ElMessage.success(`"${name}" 已删除`);
  } catch (err: any) {
    ElMessage.error('删除失败：' + (err.message || err));
  }
}

async function copyPath(path: string) {
  try {
    const rootHandle = fileSystemStore.rootHandle;
    if (!rootHandle) {
      ElMessage.warning('未打开文件夹');
      return;
    }

    const fullPath = path || '(根目录)';

    try {
      await navigator.clipboard.writeText(fullPath);
      ElMessage.success({
        message: `路径已复制到剪贴板：${fullPath}`,
        duration: 3000,
      });
    } catch {
      ElMessage.info({
        message: `路径：${fullPath}`,
        duration: 5000,
        showClose: true,
      });
    }
  } catch (err: any) {
    console.error('Failed to open in explorer:', err);
    ElMessage.error('操作失败：' + (err.message || err));
  }
}

async function closeFile(path: string) {
  const file = fileSystemStore.openFiles.get(path);
  if (file?.modified) {
    try {
      await ElMessageBox.confirm('文件未保存，确定关闭吗？', '提示', {
        type: 'warning',
      });
      fileSystemStore.closeFile(path);
    } catch {
      // User cancelled
    }
  } else {
    fileSystemStore.closeFile(path);
  }
}

// Panel resize logic
let resizing = false;
let resizeTarget = '';
let startX = 0;
let startY = 0;
let startWidth = 0;
let startHeight = 0;

function startResize(target: string, e: MouseEvent) {
  resizing = true;
  resizeTarget = target;
  startX = e.clientX;
  startY = e.clientY;

  if (target === 'left') {
    startWidth = leftPanelWidth.value;
  } else if (target === 'right') {
    startWidth = rightPanelWidth.value;
  } else if (target === 'bottom') {
    startHeight = bottomPanelHeight.value;
  }

  document.addEventListener('mousemove', handleResize);
  document.addEventListener('mouseup', stopResize);
  document.body.style.cursor = target === 'bottom' ? 'row-resize' : 'col-resize';
  document.body.style.userSelect = 'none';
}

function handleResize(e: MouseEvent) {
  if (!resizing) return;

  if (resizeTarget === 'left') {
    const delta = e.clientX - startX;
    leftPanelWidth.value = Math.max(200, Math.min(600, startWidth + delta));
  } else if (resizeTarget === 'right') {
    const delta = startX - e.clientX;
    rightPanelWidth.value = Math.max(250, Math.min(600, startWidth + delta));
  } else if (resizeTarget === 'bottom') {
    const delta = startY - e.clientY;
    bottomPanelHeight.value = Math.max(150, Math.min(500, startHeight + delta));
  }
}

function stopResize() {
  resizing = false;
  resizeTarget = '';
  document.removeEventListener('mousemove', handleResize);
  document.removeEventListener('mouseup', stopResize);
  document.body.style.cursor = '';
  document.body.style.userSelect = '';
}

// 防止浏览器快捷键冲突
function handleGlobalKeyDown(e: KeyboardEvent) {
  // 检测常见的编辑器快捷键
  const isEditorShortcut =
    (e.ctrlKey || e.metaKey) && (
      e.key === 's' ||  // Ctrl+S 保存
      e.key === 'f' ||  // Ctrl+F 查找
      e.key === 'h' ||  // Ctrl+H 替换
      e.key === 'g' ||  // Ctrl+G 跳转行
      e.key === 'd' ||  // Ctrl+D 删除行
      e.key === '/' ||  // Ctrl+/ 注释
      e.key === 'z' ||  // Ctrl+Z 撤销
      e.key === 'y' ||  // Ctrl+Y 重做
      e.key === 'x' ||  // Ctrl+X 剪切
      e.key === 'a' ||  // Ctrl+A 全选
      e.key === '[' ||  // Ctrl+[ 减少缩进
      e.key === ']'     // Ctrl+] 增加缩进
    );

  // 如果是编辑器快捷键，阻止浏览器默认行为
  if (isEditorShortcut) {
    e.preventDefault();
    e.stopPropagation();

    // 如果是 Ctrl+S，保存当前活动文件
    if (e.key === 's') {
      handleSaveCurrentFile();
    }
  }
}

// 允许在编辑器区域使用右键菜单
function handleContextMenu(e: MouseEvent) {
  // 检查是否在编辑器区域内
  const target = e.target as HTMLElement;
  const isInEditor = target.closest('.monaco-editor') ||
                     target.closest('.image-preview') ||
                     target.closest('.editor-content');

  // 如果在编辑器内，允许右键菜单
  if (isInEditor) {
    // Monaco Editor 会处理自己的右键菜单
    return;
  }

  // 其他区域阻止默认右键菜单
  e.preventDefault();
}

// Warn before closing if there are unsaved changes
const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (hasUnsavedChanges.value) {
    e.preventDefault();
    e.returnValue = '';
  }
};

window.addEventListener('beforeunload', handleBeforeUnload);

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload);
  stopResize();
});
</script>

<style scoped>
.resize-handle-col {
  width: 8px;
  margin: 0 -4px;
  cursor: col-resize;
  z-index: 100;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
}

.resize-handle-col::after {
  content: '';
  width: 2px;
  height: 100%;
  background-color: transparent;
  transition: background-color 0.2s;
}

.resize-handle-col:hover::after,
.resize-handle-col:active::after {
  background-color: var(--el-color-primary);
}

.vehicle-pack-editor {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--el-bg-color);
}

.toolbar {
  height: var(--toolbar-height);
  border-bottom: 1px solid var(--el-border-color);
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--el-bg-color);
  flex-shrink: 0;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.editor-workspace {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-layout {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

.left-panel,
.center-panel,
.right-panel {
  height: 100%;
  position: relative;
  overflow: hidden;
}

.left-panel {
  /* border-right: 1px solid var(--el-border-color); */
  min-width: 200px;
  max-width: 600px;
  background: var(--el-bg-color);
}

.center-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 400px;
  background: var(--el-bg-color);
  border-left: 1px solid var(--el-border-color);
  border-right: 1px solid var(--el-border-color);
}

.right-panel {
  /* border-left: 1px solid var(--el-border-color); */
  min-width: 250px;
  max-width: 600px;
  background: var(--el-bg-color);
}

.bottom-panel {
  border-top: 1px solid var(--el-border-color);
  position: relative;
  min-height: 150px;
  max-height: 500px;
  background: var(--el-bg-color);
}

.welcome-screen {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.welcome-info {
  margin-top: 24px;
  max-width: 500px;
}

.welcome-info ul {
  margin-top: 8px;
  padding-left: 24px;
}

.welcome-info li {
  margin: 4px 0;
}
</style>
