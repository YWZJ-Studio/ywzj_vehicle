/**
 * Monaco Editor 命名空间 ID 自动补全功能
 *
 * 为 JSON 编辑器提供命名空间 ID 的智能补全
 */

import type * as Monaco from 'monaco-editor';
import {getAllResourceTypes, pathToNamespaceId, type ResourceTypeConfig} from './namespaceId';
import type {FileNode} from '@/types/fileSystem';
import {globalSoundsManager} from './soundsManager';

/**
 * 命名空间 ID 补全项
 */
export interface NamespaceIdCompletionItem {
  /** 命名空间 ID */
  namespaceId: string;
  /** 文件路径 */
  filePath: string;
  /** 资源类型配置 */
  resourceType: ResourceTypeConfig;
  /** 显示标签 */
  label: string;
  /** 详细描述 */
  detail?: string;
  /** 文档说明 */
  documentation?: string;
}

/**
 * 命名空间 ID 补全提供器
 */
export class NamespaceIdCompletionProvider {
  private fileTree: FileNode[] = [];
  private completionItems: Map<string, NamespaceIdCompletionItem[]> = new Map();
  private fileTreeHash: string = '';

  /**
   * 更新文件树数据
   */
  updateFileTree(fileTree: FileNode[]): void {
    // 计算简单哈希（文件树长度 + 前几个文件路径）
    const hash = `${fileTree.length}`;

    // 如果文件树没有变化，跳过重建
    if (this.fileTreeHash === hash && this.completionItems.size > 0) {
      return;
    }

    this.fileTree = fileTree;
    this.fileTreeHash = hash;
    this.rebuildCompletionItems();
  }

  /**
   * 检查是否已有文件树数据
   */
  hasFileTree(): boolean {
    return this.fileTree.length > 0;
  }

  /**
   * 重建补全项缓存
   */
  private rebuildCompletionItems(): void {
    this.completionItems.clear();

    // 遍历文件树，为每种资源类型收集补全项
    const allTypes = getAllResourceTypes();
    const itemsByCategory = new Map<string, NamespaceIdCompletionItem[]>();

    // 收集常规文件资源
    this.collectCompletionItems(this.fileTree, itemsByCategory);

    // 从全局 soundsManager 获取音效事件
    this.collectSoundEventsFromManager(itemsByCategory);

    // 按资源类型分组存储
    allTypes.forEach(type => {
      const key = `${type.packType}/${type.category}`;
      const items = itemsByCategory.get(key) || [];
      this.completionItems.set(key, items);
    });

    // 确保 sound_events 也被添加（即使不在 resourceTypes 中）
    const soundEventsKey = 'assets/sound_events';
    if (itemsByCategory.has(soundEventsKey) && !this.completionItems.has(soundEventsKey)) {
      this.completionItems.set(soundEventsKey, itemsByCategory.get(soundEventsKey)!);
    }
  }

  /**
   * 递归收集补全项
   */
  private collectCompletionItems(
    nodes: FileNode[],
    itemsByCategory: Map<string, NamespaceIdCompletionItem[]>
  ): void {
    nodes.forEach(node => {
      if (node.type === 'folder' && node.children) {
        this.collectCompletionItems(node.children, itemsByCategory);
      } else if (node.type === 'file') {
        const nsId = pathToNamespaceId(node.path);
        if (nsId) {
          const resourceType = this.getResourceTypeForPath(node.path);
          if (resourceType) {
            const key = `${resourceType.packType}/${resourceType.category}`;
            if (!itemsByCategory.has(key)) {
              itemsByCategory.set(key, []);
            }

            const item: NamespaceIdCompletionItem = {
              namespaceId: nsId,
              filePath: node.path,
              resourceType,
              label: nsId,
              detail: `${resourceType.name} (${node.name})`,
              documentation: `路径: ${node.path}`
            };

            itemsByCategory.get(key)!.push(item);
          }
        }
      }
    });
  }

  /**
   * 从全局 soundsManager 收集音效事件
   */
  private collectSoundEventsFromManager(
    itemsByCategory: Map<string, NamespaceIdCompletionItem[]>
  ): void {
    // 从全局管理器获取音效事件
    const soundEvents = globalSoundsManager.getSoundEvents();

    if (soundEvents.length === 0) {
      return;
    }

    const key = 'assets/sound_events';
    if (!itemsByCategory.has(key)) {
      itemsByCategory.set(key, []);
    }

    // 创建虚拟的资源类型配置
    const soundResourceType: ResourceTypeConfig = {
      name: '音效事件',
      packType: 'assets',
      category: 'sound_events',
      extensions: ['json'],
      pathStrategy: 'fullpath',
      description: '音效事件（来自 sounds.json）'
    };

    // 为每个音效事件创建补全项
    soundEvents.forEach(event => {
      const item: NamespaceIdCompletionItem = {
        namespaceId: event.namespaceId,
        filePath: event.filePath,
        resourceType: soundResourceType,
        label: event.namespaceId,
        detail: `音效事件 (${event.id})`,
        documentation: `定义在: ${event.filePath}\n音效文件: ${event.sounds?.join(', ') || '无'}`
      };

      itemsByCategory.get(key)!.push(item);
    });
  }

  /**
   * 获取路径对应的资源类型
   */
  private getResourceTypeForPath(path: string): ResourceTypeConfig | null {
    const allTypes = getAllResourceTypes();
    const normalizedPath = path.replace(/\\/g, '/');

    for (const type of allTypes) {
      const pattern = new RegExp(
        `(?:^|/)${type.packType}/([^/]+)/${type.category.replace(/\//g, '/')}/(.+)$`
      );
      const match = normalizedPath.match(pattern);
      if (match) {
        const ext = match[2].split('.').pop()?.toLowerCase();
        if (ext && type.extensions.includes(ext)) {
          return type;
        }
      }
    }
    return null;
  }

  /**
   * 根据资源类型获取补全项
   *
   * @param packType 包类型 ('data' 或 'assets')
   * @param category 资源类别（支持多级，如 'models/bedrock'）
   * @returns 补全项数组
   */
  getCompletionsByType(packType: 'data' | 'assets', category: string): NamespaceIdCompletionItem[] {
    const key = `${packType}/${category}`;
    const items = this.completionItems.get(key) || [];

    return items;
  }

  /**
   * 获取所有补全项
   */
  getAllCompletions(): NamespaceIdCompletionItem[] {
    const all: NamespaceIdCompletionItem[] = [];
    this.completionItems.forEach(items => {
      all.push(...items);
    });
    return all;
  }

  /**
   * 根据前缀过滤补全项
   */
  filterCompletions(
    items: NamespaceIdCompletionItem[],
    prefix: string
  ): NamespaceIdCompletionItem[] {
    const lowerPrefix = prefix.toLowerCase();
    return items.filter(item =>
      item.namespaceId.toLowerCase().includes(lowerPrefix) ||
      item.label.toLowerCase().includes(lowerPrefix)
    );
  }

  /**
   * 清空所有缓存
   */
  clear(): void {
    this.fileTree = [];
    this.completionItems.clear();
    this.fileTreeHash = '';
    console.log('[NamespaceIdCompletionProvider] 🔄 缓存已清空');
  }
}

/**
 * 创建 Monaco Editor 补全提供器
 *
 * @param monaco Monaco Editor 实例
 * @param provider 命名空间 ID 补全提供器
 * @param options 补全配置选项
 */
export function createMonacoCompletionProvider(
  monaco: typeof Monaco,
  provider: NamespaceIdCompletionProvider,
  options?: {
    /** 触发字符 */
    triggerCharacters?: string[];
    /** 自定义过滤器，用于判断是否应该提供补全 */
    shouldProvideCompletions?: (model: Monaco.editor.ITextModel, position: Monaco.Position) => boolean;
    /** 自定义资源类型推断，根据上下文推断需要的资源类型 */
    inferResourceType?: (model: Monaco.editor.ITextModel, position: Monaco.Position) => { packType: 'data' | 'assets', category: string } | null;
  }
): Monaco.languages.CompletionItemProvider {
  return {
    triggerCharacters: options?.triggerCharacters || ['"', ':'],

    provideCompletionItems(model, position) {
      // 检查是否应该提供补全
      if (options?.shouldProvideCompletions && !options.shouldProvideCompletions(model, position)) {
        return { suggestions: [] };
      }

      // 获取当前行内容，找到字符串值的开始和结束位置
      const lineContent = model.getLineContent(position.lineNumber);
      const beforeCursor = lineContent.substring(0, position.column - 1);
      const afterCursor = lineContent.substring(position.column - 1);

      // 找到当前字符串值的起始引号位置
      let stringStart = beforeCursor.lastIndexOf('"');
      if (stringStart === -1) {
        return { suggestions: [] };
      }
      stringStart += 1; // 跳过引号本身

      // 找到当前字符串值的结束引号位置
      let stringEnd = afterCursor.indexOf('"');
      if (stringEnd === -1) {
        stringEnd = lineContent.length;
      } else {
        stringEnd = position.column - 1 + stringEnd;
      }

      // 计算替换范围：从字符串开始到光标位置（或字符串结束）
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: stringStart + 1, // Monaco 列从 1 开始
        endColumn: stringEnd + 1
      };

      // 获取当前已输入的值（用于过滤）
      const currentValue = lineContent.substring(stringStart, position.column - 1);

      // 尝试推断资源类型
      let completionItems: NamespaceIdCompletionItem[] = [];

      if (options?.inferResourceType) {
        const resourceType = options.inferResourceType(model, position);
        if (resourceType) {
          // 只在成功推断出资源类型时提供补全
          completionItems = provider.getCompletionsByType(resourceType.packType, resourceType.category);

          // 根据当前输入过滤
          if (currentValue) {
            completionItems = provider.filterCompletions(completionItems, currentValue);
          }
        }
        // 如果推断失败，返回空列表（不显示补全）
      } else {
        // 如果没有提供推断函数，提供所有补全项（降级行为）
        completionItems = provider.getAllCompletions();
        if (currentValue) {
          completionItems = provider.filterCompletions(completionItems, currentValue);
        }
      }

      // 转换为 Monaco 补全项
      const suggestions: Monaco.languages.CompletionItem[] = completionItems.map(item => ({
        label: item.label,
        kind: monaco.languages.CompletionItemKind.Reference,
        detail: item.detail,
        documentation: item.documentation,
        insertText: item.namespaceId,
        range
      }));

      return { suggestions };
    }
  };
}

/**
 * 智能推断资源类型的辅助函数
 * 根据 JSON 字段名推断需要的资源类型
 *
 * @param ruleManager 补全规则管理器（可选，默认使用全局管理器）
 *
 * @example
 * ```json
 * {
 *   "model": "ywzj_vehicle:|",  // 推断为 assets/models/bedrock
 *   "texture": "ywzj_vehicle:|",  // 推断为 assets/textures
 *   "type": "ywzj_vehicle:|"  // 推断为 data/vehicles
 * }
 * ```
 */
export function createSmartResourceTypeInferrer(
  ruleManager?: { findMatchingRule: (fieldName: string) => { packType: 'data' | 'assets', category: string } | null }
): (
  model: Monaco.editor.ITextModel,
  position: Monaco.Position
) => { packType: 'data' | 'assets', category: string } | null {
  return (model, position) => {
    try {
      // 获取当前行内容
      const currentLine = model.getLineContent(position.lineNumber);
      const beforeCursor = currentLine.substring(0, position.column - 1);

      // 方法1: 匹配当前行的字段名 "fieldName": "value|..."
      // 这个模式更宽松，允许光标在字符串中间
      let fieldMatch = beforeCursor.match(/"([^"]+)"\s*:\s*"[^"]*$/);

      // 方法2: 如果方法1失败，尝试匹配整行 "fieldName": "任意内容"
      if (!fieldMatch) {
        fieldMatch = currentLine.match(/"([^"]+)"\s*:\s*"/);
      }

      if (fieldMatch && fieldMatch[1]) {
        const fieldName = fieldMatch[1];

        // 使用规则管理器查找匹配规则
        if (ruleManager) {
          const rule = ruleManager.findMatchingRule(fieldName);
          if (rule) {
            console.log(`[ResourceTypeInferrer] 字段 "${fieldName}" 匹配规则:`, rule);
            return { packType: rule.packType, category: rule.category };
          } else {
            console.log(`[ResourceTypeInferrer] 字段 "${fieldName}" 无匹配规则`);
          }
        }
      } else {
        console.log(`[ResourceTypeInferrer] 无法从行中提取字段名:`, currentLine);
      }

      return null;
    } catch (err) {
      console.error('Error inferring resource type:', err);
      return null;
    }
  };
}

/**
 * 注册命名空间 ID 补全到 Monaco Editor
 *
 * @param monaco Monaco Editor 实例
 * @param provider 命名空间 ID 补全提供器
 * @param ruleManager 补全规则管理器（可选）
 * @returns 注册的 disposable 对象
 */
export function registerNamespaceIdCompletion(
  monaco: typeof Monaco,
  provider: NamespaceIdCompletionProvider,
  ruleManager?: { findMatchingRule: (fieldName: string) => { packType: 'data' | 'assets', category: string } | null }
): Monaco.IDisposable {
  return monaco.languages.registerCompletionItemProvider('json',
    createMonacoCompletionProvider(monaco, provider, {
      triggerCharacters: ['"', ':'],
      inferResourceType: createSmartResourceTypeInferrer(ruleManager),
      shouldProvideCompletions: (model, position) => {
        // 只在字符串值中提供补全
        const lineContent = model.getLineContent(position.lineNumber);
        const beforeCursor = lineContent.substring(0, position.column - 1);

        // 检查是否在字符串内
        const quotesBefore = (beforeCursor.match(/"/g) || []).length;
        return quotesBefore % 2 === 1; // 奇数个引号表示在字符串内
      }
    })
  );
}

/**
 * 创建全局命名空间 ID 补全提供器实例
 */
export const globalNamespaceIdProvider = new NamespaceIdCompletionProvider();

/**
 * 从 Schema 注册表初始化补全系统
 *
 * @param schemaRegistry Schema 注册表
 * @param ruleManager 规则管理器（可选，默认使用全局管理器）
 *
 * @example
 * ```typescript
 * import { schemaRegistry } from '@/utils/schemaRegistry';
 * import { globalCompletionRuleManager } from '@/utils/completionRules';
 *
 * initCompletionFromSchemas(schemaRegistry, globalCompletionRuleManager);
 * ```
 */
export async function initCompletionFromSchemas(
  schemaRegistry: Array<{ schema: any; title?: string }>,
  ruleManager?: { addRules: (rules: any[]) => void }
): Promise<void> {
  const { extractCompletionRulesFromSchemaRegistry, deduplicateCompletionRules } =
    await import('./schemaCompletionExtractor');

  const schemasWithTitle = schemaRegistry.map(item => ({
    schema: item.schema,
    title: item.title || 'Untitled Schema'
  }));

  const rules = extractCompletionRulesFromSchemaRegistry(schemasWithTitle);

  const uniqueRules = deduplicateCompletionRules(rules);

  if (ruleManager) {
    ruleManager.addRules(uniqueRules);
  }
}

