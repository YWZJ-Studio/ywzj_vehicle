/**
 * 音效事件管理器
 * 负责统一管理 sounds.json 的加载和解析
 */

import type {FileNode} from '@/types/fileSystem';
import {fileSystemManager} from './fileSystemApi';
import {parseSoundsJson, type SoundEvent} from './soundParser';

export class SoundsManager {
  private soundEvents: SoundEvent[] = [];
  private soundsJsonCache: Map<string, string> = new Map();
  private initialized = false;
  private loading = false;

  /**
   * 初始化：从文件树中加载所有 sounds.json
   */
  async initialize(fileTree: FileNode[]): Promise<void> {
    if (this.initialized || this.loading) {
      console.log('[SoundsManager] ⏭️ 已初始化或正在加载，跳过');
      return;
    }

    this.loading = true;
    console.log('[SoundsManager] 🎵 开始初始化，加载所有 sounds.json...');

    try {
      // 清空之前的数据
      this.soundEvents = [];
      this.soundsJsonCache.clear();

      // 遍历文件树，找到所有 sounds.json 文件并读取
      await this.loadSoundsJsonFiles(fileTree);

      // 解析所有 sounds.json
      this.parseSoundsJsonFiles();

      this.initialized = true;
      console.log(`[SoundsManager] ✅ 初始化完成，共解析 ${this.soundEvents.length} 个音效事件`);
    } catch (err) {
      console.error('[SoundsManager] ❌ 初始化失败:', err);
    } finally {
      this.loading = false;
    }
  }

  /**
   * 重新加载 sounds.json（用于文件保存后）
   */
  async reload(fileTree: FileNode[]): Promise<void> {
    console.log('[SoundsManager] 🔄 重新加载 sounds.json...');
    this.initialized = false;
    await this.initialize(fileTree);
  }

  /**
   * 从文件树中加载所有 sounds.json 文件
   */
  private async loadSoundsJsonFiles(fileTree: FileNode[]): Promise<void> {
    const traverse = async (nodes: FileNode[]): Promise<void> => {
      for (const node of nodes) {
        if (node.type === 'folder' && node.children) {
          await traverse(node.children);
        } else if (node.type === 'file' && node.name === 'sounds.json') {
          const normalizedPath = node.path.replace(/\\/g, '/');

          // 检查是否在 assets 目录下
          if (normalizedPath.includes('/assets/') || normalizedPath.startsWith('assets/')) {
            try {
              if (node.handle && 'getFile' in node.handle) {
                console.log(`[SoundsManager] 📖 读取: ${normalizedPath}`);
                const content = await fileSystemManager.readFile(node.handle as FileSystemFileHandle);
                this.soundsJsonCache.set(normalizedPath, content);
              }
            } catch (err) {
              console.error(`[SoundsManager] ❌ 读取失败: ${normalizedPath}`, err);
            }
          }
        }
      }
    };

    await traverse(fileTree);
    console.log(`[SoundsManager] 📚 读取了 ${this.soundsJsonCache.size} 个 sounds.json 文件`);
  }

  /**
   * 解析所有已加载的 sounds.json 文件
   */
  private parseSoundsJsonFiles(): void {
    this.soundEvents = [];

    for (const [path, content] of this.soundsJsonCache.entries()) {
      try {
        const events = parseSoundsJson(content, path);
        this.soundEvents.push(...events);
        console.log(`[SoundsManager] ✅ 从 ${path} 解析了 ${events.length} 个事件`);
      } catch (err) {
        console.error(`[SoundsManager] ❌ 解析失败: ${path}`, err);
      }
    }
  }

  /**
   * 获取所有音效事件
   */
  getSoundEvents(): SoundEvent[] {
    return this.soundEvents;
  }

  /**
   * 检查是否已初始化
   */
  isInitialized(): boolean {
    return this.initialized;
  }

  /**
   * 重置状态
   */
  reset(): void {
    this.soundEvents = [];
    this.soundsJsonCache.clear();
    this.initialized = false;
    this.loading = false;
    console.log('[SoundsManager] 🔄 状态已重置');
  }
}

/**
 * 全局音效管理器实例
 */
export const globalSoundsManager = new SoundsManager();
