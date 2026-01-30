/**
 * 音效资源解析器
 *
 * 从 assets/namespace/sounds.json 文件中解析音效事件 ID
 */

import type {FileNode} from '@/types/fileSystem';
import {parseJsonWithComments} from './jsonParser';

export interface SoundEvent {
  /** 音效事件 ID（不含命名空间前缀） */
  id: string;
  /** 完整的命名空间 ID */
  namespaceId: string;
  /** 命名空间 */
  namespace: string;
  /** sounds.json 文件路径 */
  filePath: string;
  /** 音效文件路径列表 */
  sounds?: string[];
  /** 字幕键 */
  subtitle?: string;
}

/**
 * 解析 sounds.json 内容
 */
export function parseSoundsJson(content: string, filePath: string): SoundEvent[] {
  try {
    const data = parseJsonWithComments(content);
    const events: SoundEvent[] = [];

    // 规范化路径分隔符为正斜杠
    const normalizedPath = filePath.replace(/\\/g, '/');

    // 从文件路径提取命名空间
    // 例如: assets/ywzj_vehicle/sounds.json -> ywzj_vehicle
    //      src/main/resources/assets/ywzj_vehicle/sounds.json -> ywzj_vehicle
    // 正则会匹配以 assets/ 结尾的路径（无论前面有什么）
    const pathMatch = normalizedPath.match(/assets\/([^/]+)\/sounds\.json$/);
    if (!pathMatch) {
      console.warn('[SoundParser] Invalid sounds.json path:', filePath, 'normalized:', normalizedPath);
      return events;
    }

    const namespace = pathMatch[1];
    console.log(`[SoundParser] 从路径 ${normalizedPath} 提取命名空间: ${namespace}`);

    // 遍历所有音效事件
    for (const [eventId, eventData] of Object.entries(data)) {
      if (typeof eventData === 'object' && eventData !== null) {
        const soundData = eventData as any;

        events.push({
          id: eventId,
          namespaceId: `${namespace}:${eventId}`,
          namespace,
          filePath,
          sounds: soundData.sounds || [],
          subtitle: soundData.subtitle
        });
      }
    }

    console.log(`[SoundParser] Parsed ${events.length} sound events from ${filePath}`);
    return events;
  } catch (err) {
    console.error('[SoundParser] Failed to parse sounds.json:', filePath, err);
    return [];
  }
}

/**
 * 从文件树中查找所有 sounds.json 文件并解析
 */
export function extractSoundEventsFromFileTree(
  fileTree: FileNode[],
  fileContents: Map<string, string>
): SoundEvent[] {
  const allEvents: SoundEvent[] = [];

  function traverse(nodes: FileNode[]) {
    for (const node of nodes) {
      if (node.type === 'folder' && node.children) {
        traverse(node.children);
      } else if (node.type === 'file') {
        // 规范化路径分隔符
        const normalizedPath = node.path.replace(/\\/g, '/');
        
        // 检查是否是 sounds.json（支持有无前导斜杠）
        if (node.name === 'sounds.json' && (normalizedPath.includes('/assets/') || normalizedPath.startsWith('assets/'))) {
          const content = fileContents.get(normalizedPath);
          if (content) {
            const events = parseSoundsJson(content, normalizedPath);
            allEvents.push(...events);
          }
        }
      }
    }
  }

  traverse(fileTree);
  return allEvents;
}

/**
 * 音效资源管理器
 */
export class SoundEventManager {
  private events: SoundEvent[] = [];
  private eventsByNamespace: Map<string, SoundEvent[]> = new Map();

  /**
   * 更新音效事件列表
   */
  updateEvents(events: SoundEvent[]): void {
    this.events = events;
    this.rebuildIndex();
  }

  /**
   * 重建索引
   */
  private rebuildIndex(): void {
    this.eventsByNamespace.clear();

    for (const event of this.events) {
      if (!this.eventsByNamespace.has(event.namespace)) {
        this.eventsByNamespace.set(event.namespace, []);
      }
      this.eventsByNamespace.get(event.namespace)!.push(event);
    }
  }

  /**
   * 获取所有音效事件
   */
  getAllEvents(): SoundEvent[] {
    return [...this.events];
  }

  /**
   * 根据命名空间获取音效事件
   */
  getEventsByNamespace(namespace: string): SoundEvent[] {
    return this.eventsByNamespace.get(namespace) || [];
  }

  /**
   * 搜索音效事件
   */
  searchEvents(query: string): SoundEvent[] {
    const lowerQuery = query.toLowerCase();
    return this.events.filter(event =>
      event.namespaceId.toLowerCase().includes(lowerQuery) ||
      event.id.toLowerCase().includes(lowerQuery)
    );
  }

  /**
   * 清空
   */
  clear(): void {
    this.events = [];
    this.eventsByNamespace.clear();
  }
}

/**
 * 全局音效事件管理器实例
 */
export const globalSoundEventManager = new SoundEventManager();
