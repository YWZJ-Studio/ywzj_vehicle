/**
 * 从 JSON Schema 提取补全配置
 *
 * 支持从 Schema 中提取 x-completion 自定义属性，自动生成补全规则
 */

import type {CompletionRule} from './completionRules';

/**
 * Schema 中的补全配置（使用 x-completion 自定义属性）
 */
export interface SchemaCompletionConfig {
  /** 资源包类型 */
  packType: 'data' | 'assets';
  /** 资源类别 */
  category: string;
  /** 可选的优先级 */
  priority?: number;
}

/**
 * 从 JSON Schema 属性中提取补全配置
 */
export function extractCompletionFromProperty(
  propertyName: string,
  propertySchema: any
): CompletionRule | null {
  // 检查是否有 x-completion 配置
  const completionConfig = propertySchema['x-completion'] as SchemaCompletionConfig | undefined;

  if (!completionConfig || !completionConfig.packType || !completionConfig.category) {
    return null;
  }

  const rule = {
    fieldPattern: propertyName,
    packType: completionConfig.packType,
    category: completionConfig.category,
    description: propertySchema.description || `${propertyName} 字段`,
    priority: completionConfig.priority || 10
  };

  console.log(`[SchemaExtractor] 从属性 "${propertyName}" 提取规则:`, rule);
  return rule;
}

/**
 * 递归遍历 Schema，提取所有补全配置
 */
export function extractCompletionRulesFromSchema(
  schema: any,
  path: string[] = []
): CompletionRule[] {
  const rules: CompletionRule[] = [];

  if (!schema || typeof schema !== 'object') {
    return rules;
  }

  // 处理当前层级的 properties
  if (schema.properties && typeof schema.properties === 'object') {
    for (const [propName, propSchema] of Object.entries(schema.properties)) {
      // 提取当前属性的补全配置
      const rule = extractCompletionFromProperty(propName, propSchema);
      if (rule) {
        rules.push(rule);
      }

      // 递归处理嵌套对象
      if (typeof propSchema === 'object' && propSchema !== null) {
        const nestedRules = extractCompletionRulesFromSchema(
          propSchema,
          [...path, propName]
        );
        rules.push(...nestedRules);
      }
    }
  }

  // 处理 additionalProperties
  if (schema.additionalProperties && typeof schema.additionalProperties === 'object') {
    // 对于动态属性，使用通配符匹配
    const completionConfig = schema.additionalProperties['x-completion'] as SchemaCompletionConfig | undefined;
    if (completionConfig) {
      rules.push({
        fieldPattern: /.*/,  // 匹配所有字段
        packType: completionConfig.packType,
        category: completionConfig.category,
        description: schema.additionalProperties.description || '动态属性',
        priority: completionConfig.priority || 5  // 动态属性优先级较低
      });
    }
  }

  // 处理 items（数组项）
  if (schema.items && typeof schema.items === 'object') {
    const nestedRules = extractCompletionRulesFromSchema(schema.items, [...path, 'items']);
    rules.push(...nestedRules);
  }

  // 处理 anyOf/oneOf/allOf
  for (const keyword of ['anyOf', 'oneOf', 'allOf']) {
    if (Array.isArray(schema[keyword])) {
      for (const subSchema of schema[keyword]) {
        const nestedRules = extractCompletionRulesFromSchema(subSchema, path);
        rules.push(...nestedRules);
      }
    }
  }

  return rules;
}

/**
 * 从多个 Schema 提取补全规则
 */
export function extractCompletionRulesFromSchemas(schemas: any[]): CompletionRule[] {
  const allRules: CompletionRule[] = [];

  for (const schema of schemas) {
    const rules = extractCompletionRulesFromSchema(schema);
    allRules.push(...rules);
  }

  return allRules;
}

/**
 * 从 Schema 注册表提取补全规则
 */
export function extractCompletionRulesFromSchemaRegistry(
  schemaRegistry: Array<{ schema: any; title: string }>
): CompletionRule[] {
  const schemas = schemaRegistry.map(info => info.schema);
  return extractCompletionRulesFromSchemas(schemas);
}

/**
 * 为 Schema 属性添加补全提示的辅助函数
 *
 * @example
 * ```typescript
 * const propertySchema = {
 *   type: "string",
 *   description: "模型资源位置",
 *   ...withCompletion('assets', 'models/bedrock')
 * };
 * ```
 */
export function withCompletion(
  packType: 'data' | 'assets',
  category: string,
  priority?: number
): { 'x-completion': SchemaCompletionConfig } {
  return {
    'x-completion': {
      packType,
      category,
      priority
    }
  };
}

/**
 * 去重补全规则（相同字段名和资源类型的规则）
 */
export function deduplicateCompletionRules(rules: CompletionRule[]): CompletionRule[] {
  const seen = new Map<string, CompletionRule>();

  for (const rule of rules) {
    const key = `${typeof rule.fieldPattern === 'string' ? rule.fieldPattern : rule.fieldPattern.source}:${rule.packType}:${rule.category}`;

    // 如果已存在，保留优先级更高的
    if (seen.has(key)) {
      const existing = seen.get(key)!;
      if ((rule.priority || 0) > (existing.priority || 0)) {
        seen.set(key, rule);
      }
    } else {
      seen.set(key, rule);
    }
  }

  return Array.from(seen.values());
}
