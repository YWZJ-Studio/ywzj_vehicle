import {defineStore} from 'pinia';
import {fileSystemManager} from '@/utils/fileSystemApi';
import {getFileType} from '@/utils/fileTypes';
import {globalSoundsManager} from '@/utils/soundsManager';
import {globalNamespaceIdProvider} from '@/utils/namespaceIdCompletion';
import type {FileNode, OpenFile} from '@/types/fileSystem';

export const useFileSystemStore = defineStore('fileSystem', {
  state: () => ({
    rootHandle: null as FileSystemDirectoryHandle | null,
    fileTree: [] as FileNode[],
    openFiles: new Map<string, OpenFile>(),
    activeFilePath: '' as string,
    loading: false,
    fileLoading: false, // 文件加载状态
  }),

  getters: {
    hasOpenFolder: (state) => state.rootHandle !== null,

    hasUnsavedChanges: (state) => {
      for (const file of state.openFiles.values()) {
        if (file.modified) return true;
      }
      return false;
    },

    activeFile: (state) => {
      if (!state.activeFilePath) return null;
      return state.openFiles.get(state.activeFilePath) || null;
    },

    openFileTabs(state) {
      return Array.from(state.openFiles.entries()).map(([path, data]) => ({
        path,
        name: path.split('/').pop() || '',
        modified: data.modified,
      }));
    },
  },

  actions: {
    /**
     * 获取未保存的文件列表
     */
    getUnsavedFiles(): string[] {
      return Array.from(this.openFiles.entries())
        .filter(([, data]) => data.modified)
        .map(([path]) => path.split('/').pop() || path);
    },

    /**
     * 打开载具包文件夹
     * 打开前会清空所有标签页和缓存
     *
     * @param skipPrompt 跳过未保存提示（由调用方处理）
     */
    async openFolder(skipPrompt: boolean = false) {
      try {
        // 检查是否有未保存的更改（如果未跳过提示）
        if (!skipPrompt && this.hasUnsavedChanges) {
          // 返回 false，让调用方处理提示
          return false;
        }

        this.loading = true;

        // 清空所有打开的标签页
        console.log('[FileSystem] 🗑️ 清空所有打开的标签页');
        this.openFiles.clear();
        this.activeFilePath = '';

        // 清空所有缓存
        console.log('[FileSystem] 🧹 清空所有缓存...');

        // 清空音效管理器缓存
        globalSoundsManager.reset();
        console.log('[FileSystem] ✅ 音效管理器缓存已清空');

        // 清空命名空间 ID 补全缓存
        globalNamespaceIdProvider.clear();
        console.log('[FileSystem] ✅ 命名空间补全缓存已清空');

        // 打开新的文件夹
        console.log('[FileSystem] 📂 打开文件夹选择器...');
        this.rootHandle = await fileSystemManager.openFolder();
        this.fileTree = await fileSystemManager.buildFileTree(this.rootHandle);

        // 初始化音效管理器，加载所有 sounds.json
        await globalSoundsManager.initialize(this.fileTree);

        console.log('[FileSystem] ✅ 载具包打开成功');
        return true;
      } catch (err: any) {
        console.error('Failed to open folder:', err);
        if (err.name !== 'AbortError') {
          throw err;
        }
        return false;
      } finally {
        this.loading = false;
      }
    },

    async openFile(path: string, fileHandle: FileSystemFileHandle) {
      if (this.openFiles.has(path)) {
        this.activeFilePath = path;
        return;
      }

      try {
        this.fileLoading = true;

        console.log('[FileSystem] 🔍 Step 1: Getting file size...', path);

        // 检查文件大小
        const fileSize = await fileSystemManager.getFileSize(fileHandle);

        console.log('[FileSystem] 📏 Step 2: File size:', {
          path,
          bytes: fileSize,
          KB: (fileSize / 1024).toFixed(2) + 'KB',
          MB: (fileSize / 1024 / 1024).toFixed(2) + 'MB'
        });

        const MAX_SAFE_SIZE = 5 * 1024 * 1024; // 5MB - 绝对上限
        const WARN_SIZE = 1 * 1024 * 1024; // 1MB - 警告阈值

        // 如果文件超过5MB，拒绝打开
        if (fileSize > MAX_SAFE_SIZE) {
          const sizeMB = (fileSize / 1024 / 1024).toFixed(2);
          throw new Error(`文件过大 (${sizeMB}MB)，浏览器无法处理。\n请使用VS Code等桌面编辑器打开。`);
        }

        // 如果文件大于1MB，给出警告
        if (fileSize > WARN_SIZE) {
          const sizeMB = (fileSize / 1024 / 1024).toFixed(2);
          console.warn(`⚠️ 警告：文件较大 (${sizeMB}MB)，可能导致编辑器性能问题`);
        }

        console.log('[FileSystem] 📖 Step 3: Reading file content...');

        // 检测文件类型，图片文件使用 data URL 读取
        const fileName = path.split('/').pop() || '';
        const fileType = getFileType(fileName);

        let content: string;
        if (fileType === 'image') {
          console.log('[FileSystem] 🖼️ Detected image file, reading as data URL...');
          content = await fileSystemManager.readFileAsDataURL(fileHandle);
        } else {
          content = await fileSystemManager.readFile(fileHandle);
        }

        console.log('[FileSystem] ✅ Step 4: File read successfully, content length:', content.length);

        this.openFiles.set(path, {
          handle: fileHandle,
          content,
          modified: false,
          savedContent: content,
        });
        this.activeFilePath = path;

        console.log('[FileSystem] ✅ Step 5: File opened successfully');
      } catch (err) {
        console.error('[FileSystem] ❌ Error in openFile:', err);
        throw err;
      } finally {
        this.fileLoading = false;
      }
    },

    updateFileContent(path: string, content: string) {
      const fileData = this.openFiles.get(path);
      if (!fileData) return;

      fileData.content = content;
      fileData.modified = content !== fileData.savedContent;
    },

    async saveFile(path: string) {
      const fileData = this.openFiles.get(path);
      if (!fileData || !fileData.modified) return;

      try {
        await fileSystemManager.writeFile(fileData.handle, fileData.content);
        fileData.savedContent = fileData.content;
        fileData.modified = false;

        // 如果保存的是 sounds.json，重新加载音效事件
        if (path.endsWith('sounds.json')) {
          console.log('[FileSystem] 💾 检测到 sounds.json 保存，触发重新加载');
          await globalSoundsManager.reload(this.fileTree);
        }

        return true;
      } catch (err) {
        console.error('Failed to save file:', err);
        return false;
      }
    },

    async saveAllFiles() {
      const promises = Array.from(this.openFiles.entries())
        .filter(([, data]) => data.modified)
        .map(([path]) => this.saveFile(path));

      await Promise.all(promises);
    },

    closeFile(path: string) {
      if (this.openFiles.has(path)) {
        this.openFiles.delete(path);
        if (this.activeFilePath === path) {
          const remaining = Array.from(this.openFiles.keys());
          this.activeFilePath = remaining[remaining.length - 1] || '';
        }
      }
    },

    setActiveFile(path: string) {
      if (this.openFiles.has(path)) {
        this.activeFilePath = path;
      }
    },

    async refreshFileTree() {
      if (!this.rootHandle) return;
      this.fileTree = await fileSystemManager.buildFileTree(this.rootHandle);
    },

    async createFile(parentPath: string, fileName: string) {
      if (!this.rootHandle) return;

      try {
        // 获取父目录句柄
        const parentHandle = await this.getDirectoryHandle(parentPath);
        if (!parentHandle) {
          throw new Error('找不到父目录');
        }

        // 创建文件
        await fileSystemManager.createFile(parentHandle, fileName);

        // 刷新文件树
        await this.refreshFileTree();
        return true;
      } catch (err: any) {
        console.error('Failed to create file:', err);
        throw err;
      }
    },

    async createFolder(parentPath: string, folderName: string) {
      if (!this.rootHandle) return;

      try {
        // 获取父目录句柄
        const parentHandle = await this.getDirectoryHandle(parentPath);
        if (!parentHandle) {
          throw new Error('找不到父目录');
        }

        // 创建文件夹
        await fileSystemManager.createFolder(parentHandle, folderName);

        // 刷新文件树
        await this.refreshFileTree();
        return true;
      } catch (err: any) {
        console.error('Failed to create folder:', err);
        throw err;
      }
    },

    async deleteFileOrFolder(path: string) {
      if (!this.rootHandle) return;

      try {
        // 解析路径
        const pathParts = path.split('/');
        const name = pathParts.pop();
        if (!name) throw new Error('无效的路径');

        // 获取父目录句柄
        const parentPath = pathParts.join('/');
        const parentHandle = await this.getDirectoryHandle(parentPath);
        if (!parentHandle) {
          throw new Error('找不到父目录');
        }

        // 查找节点以确定类型
        const node = this.findNodeByPath(path);
        if (!node) {
          throw new Error('找不到要删除的文件或文件夹');
        }

        // 如果是打开的文件，先关闭它
        if (node.type === 'file' && this.openFiles.has(path)) {
          this.closeFile(path);
        }

        // 删除文件或文件夹
        if (node.type === 'folder') {
          await fileSystemManager.deleteFolder(parentHandle, name);
        } else {
          await fileSystemManager.deleteFile(parentHandle, name);
        }

        // 刷新文件树
        await this.refreshFileTree();
        return true;
      } catch (err: any) {
        console.error('Failed to delete:', err);
        throw err;
      }
    },

    async getDirectoryHandle(path: string): Promise<FileSystemDirectoryHandle | null> {
      if (!this.rootHandle) return null;
      if (!path) return this.rootHandle;

      const parts = path.split('/').filter(p => p);
      let currentHandle: FileSystemDirectoryHandle = this.rootHandle;

      for (const part of parts) {
        try {
          currentHandle = await currentHandle.getDirectoryHandle(part);
        } catch {
          return null;
        }
      }

      return currentHandle;
    },

    findNodeByPath(path: string): FileNode | null {
      const parts = path.split('/').filter(p => p);

      function search(nodes: FileNode[], depth: number): FileNode | null {
        if (depth >= parts.length) return null;

        for (const node of nodes) {
          if (node.name === parts[depth]) {
            if (depth === parts.length - 1) {
              return node;
            }
            if (node.children) {
              return search(node.children, depth + 1);
            }
          }
        }
        return null;
      }

      return search(this.fileTree, 0);
    },
  },
});
