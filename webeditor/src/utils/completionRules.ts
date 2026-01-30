/**
 * 命名空间 ID 补全规则配置
 *
 * 定义 JSON 字段名到资源类型的映射规则，用于智能补全
 */

export interface CompletionRule {
  /** 字段名（支持正则表达式字符串） */
  fieldPattern: string | RegExp;
  /** 资源包类型 */
  packType: 'data' | 'assets';
  /** 资源类别 */
  category: string;
  /** 规则描述 */
  description?: string;
  /** 优先级（数字越大优先级越高） */
  priority?: number;
}

/**
 * 默认补全规则
 */
export const defaultCompletionRules: CompletionRule[] = [
  // ===== 模型相关 =====
  {
    fieldPattern: 'model',
    packType: 'assets',
    category: 'models/bedrock',
    description: '实体模型文件',
    priority: 10
  },
  {
    fieldPattern: 'geometry',
    packType: 'assets',
    category: 'models/bedrock',
    description: '几何模型定义',
    priority: 10
  },

  // ===== 纹理相关 =====
  {
    fieldPattern: /^(texture|textures)$/,
    packType: 'assets',
    category: 'textures',
    description: '纹理图片',
    priority: 10
  },
  {
    fieldPattern: 'slot_texture',
    packType: 'assets',
    category: 'textures',
    description: '槽位纹理',
    priority: 10
  },
  {
    fieldPattern: /_texture$/,
    packType: 'assets',
    category: 'textures',
    description: '纹理图片（后缀匹配）',
    priority: 5
  },

  // ===== 音效事件相关 =====
  {
    fieldPattern: /^(sound|sounds)$/,
    packType: 'assets',
    category: 'sound_events',
    description: '音效事件',
    priority: 10
  },
  {
    fieldPattern: 'engine_start',
    packType: 'assets',
    category: 'sound_events',
    description: '引擎启动音效',
    priority: 10
  },
  {
    fieldPattern: 'engine_idle',
    packType: 'assets',
    category: 'sound_events',
    description: '引擎怠速音效',
    priority: 10
  },
  {
    fieldPattern: 'engine_run',
    packType: 'assets',
    category: 'sound_events',
    description: '引擎运行音效',
    priority: 10
  },
  {
    fieldPattern: 'fire',
    packType: 'assets',
    category: 'sound_events',
    description: '开火音效',
    priority: 8
  },
  {
    fieldPattern: 'reload',
    packType: 'assets',
    category: 'sound_events',
    description: '重装填音效',
    priority: 8
  },
  {
    fieldPattern: /_sound$/,
    packType: 'assets',
    category: 'sound_events',
    description: '音效事件（后缀匹配）',
    priority: 5
  },

  // ===== 动画相关 =====
  {
    fieldPattern: /^(animation|animations)$/,
    packType: 'assets',
    category: 'models/bedrock',
    description: '动画文件',
    priority: 10
  },

  // ===== 脚本相关 =====
  {
    fieldPattern: 'script',
    packType: 'assets',
    category: 'display',
    description: '客户端脚本',
    priority: 10
  },

  // ===== 显示配置相关 =====
  {
    fieldPattern: 'display',
    packType: 'assets',
    category: 'display',
    description: '显示配置',
    priority: 10
  },

  // ===== 数据包相关 =====
  {
    fieldPattern: 'type',
    packType: 'data',
    category: 'vehicles',
    description: '载具类型',
    priority: 10
  },
  {
    fieldPattern: 'ammo',
    packType: 'data',
    category: 'weapons',
    description: '弹药类型',
    priority: 10
  },
  {
    fieldPattern: 'weapon',
    packType: 'data',
    category: 'weapons',
    description: '武器类型',
    priority: 10
  },
];

/**
 * 补全规则管理器
 */
export class CompletionRuleManager {
  private rules: CompletionRule[] = [];

  constructor(initialRules: CompletionRule[] = defaultCompletionRules) {
    this.rules = [...initialRules];
    // 按优先级排序
    this.sortRules();
  }

  /**
   * 添加规则
   */
  addRule(rule: CompletionRule): void {
    this.rules.push(rule);
    this.sortRules();
  }

  /**
   * 批量添加规则
   */
  addRules(rules: CompletionRule[]): void {
    this.rules.push(...rules);
    this.sortRules();
  }

  /**
   * 移除规则
   */
  removeRule(fieldPattern: string | RegExp): void {
    this.rules = this.rules.filter(rule => {
      if (typeof rule.fieldPattern === 'string' && typeof fieldPattern === 'string') {
        return rule.fieldPattern !== fieldPattern;
      }
      if (rule.fieldPattern instanceof RegExp && fieldPattern instanceof RegExp) {
        return rule.fieldPattern.source !== fieldPattern.source;
      }
      return true;
    });
  }

  /**
   * 获取所有规则
   */
  getAllRules(): CompletionRule[] {
    return [...this.rules];
  }

  /**
   * 根据字段名查找匹配的规则
   */
  findMatchingRule(fieldName: string): CompletionRule | null {
    console.log(`[CompletionRuleManager] 查找字段 "${fieldName}" 的匹配规则，当前共有 ${this.rules.length} 条规则`);

    for (const rule of this.rules) {
      if (this.matchesField(rule.fieldPattern, fieldName)) {
        console.log(`[CompletionRuleManager] ✅ 找到匹配规则:`, rule);
        return rule;
      }
    }

    console.log(`[CompletionRuleManager] ❌ 未找到匹配规则`);
    // 输出所有可用规则以便调试
    console.log(`[CompletionRuleManager] 可用规则列表:`, this.rules.map(r => ({
      fieldPattern: typeof r.fieldPattern === 'string' ? r.fieldPattern : r.fieldPattern.source,
      packType: r.packType,
      category: r.category
    })));

    return null;
  }

  /**
   * 检查字段名是否匹配规则
   */
  private matchesField(pattern: string | RegExp, fieldName: string): boolean {
    if (typeof pattern === 'string') {
      return pattern === fieldName;
    }
    return pattern.test(fieldName);
  }

  /**
   * 按优先级排序规则
   */
  private sortRules(): void {
    this.rules.sort((a, b) => {
      const priorityA = a.priority ?? 0;
      const priorityB = b.priority ?? 0;
      return priorityB - priorityA; // 降序排序
    });
  }

  /**
   * 清空所有规则
   */
  clear(): void {
    this.rules = [];
  }

  /**
   * 重置为默认规则
   */
  reset(): void {
    this.rules = [...defaultCompletionRules];
    this.sortRules();
  }
}

/**
 * 全局补全规则管理器实例
 */
export const globalCompletionRuleManager = new CompletionRuleManager();

/**
 * 注册自定义补全规则
 *
 * @param rules 补全规则数组
 *
 * @example
 * ```typescript
 * registerCompletionRules([
 *   {
 *     fieldPattern: 'custom_model',
 *     packType: 'assets',
 *     category: 'models/custom',
 *     description: '自定义模型',
 *     priority: 10
 *   }
 * ]);
 * ```
 */
export function registerCompletionRules(rules: CompletionRule[]): void {
  globalCompletionRuleManager.addRules(rules);
}

/**
 * 注册单个补全规则
 */
export function registerCompletionRule(rule: CompletionRule): void {
  globalCompletionRuleManager.addRule(rule);
}

/**
 * 从 Schema 注册表自动注册补全规则
 *
 * @param schemaRegistry Schema 注册表
 *
 * @example
 * ```typescript
 * import { schemaRegistry } from '@/utils/schemaRegistry';
 * registerCompletionRulesFromSchemas(schemaRegistry);
 * ```
 */
export function registerCompletionRulesFromSchemas(
  schemaRegistry: Array<{ schema: any; title?: string }>
): void {
  // 动态导入以避免循环依赖
  import('./schemaCompletionExtractor').then(({ extractCompletionRulesFromSchemaRegistry, deduplicateCompletionRules }) => {
    // Convert to expected format
    const schemasWithTitle = schemaRegistry.map(item => ({
      schema: item.schema,
      title: item.title || 'Untitled Schema'
    }));

    const rules = extractCompletionRulesFromSchemaRegistry(schemasWithTitle);
    const uniqueRules = deduplicateCompletionRules(rules);
    globalCompletionRuleManager.addRules(uniqueRules);

    console.log(`[CompletionRules] 从 Schema 自动注册了 ${uniqueRules.length} 条补全规则`);
  });
}

