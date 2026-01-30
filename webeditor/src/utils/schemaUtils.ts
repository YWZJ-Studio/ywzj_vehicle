/**
 * JSON Schema 解析工具
 */

/**
 * 解析 $ref 引用
 * @param ref $ref 字符串，如 "#/definitions/partUnit"
 * @param rootSchema 根 Schema 对象
 * @returns 解析后的 Schema 对象
 */
export function resolveRef(ref: string, rootSchema: any): any {
  if (!ref.startsWith('#/')) {
    console.warn('Only internal references (#/) are supported');
    return {};
  }

  const path = ref.slice(2).split('/'); // 移除 '#/' 并分割路径
  let result = rootSchema;

  for (const segment of path) {
    if (result && typeof result === 'object' && segment in result) {
      result = result[segment];
    } else {
      console.warn(`Failed to resolve $ref: ${ref}`);
      return {};
    }
  }

  return result;
}

/**
 * 展开 Schema 中的所有 $ref 引用
 * @param schema 要展开的 Schema
 * @param rootSchema 根 Schema（用于解析引用）
 * @returns 展开后的 Schema
 */
export function expandSchema(schema: any, rootSchema?: any): any {
  const root = rootSchema || schema;

  if (!schema || typeof schema !== 'object') {
    return schema;
  }

  // 如果有 $ref，解析它
  if (schema.$ref) {
    const resolved = resolveRef(schema.$ref, root);
    return expandSchema(resolved, root);
  }

  // 递归展开对象中的所有属性
  const expanded: any = Array.isArray(schema) ? [] : {};

  for (const [key, value] of Object.entries(schema)) {
    if (typeof value === 'object' && value !== null) {
      expanded[key] = expandSchema(value, root);
    } else {
      expanded[key] = value;
    }
  }

  return expanded;
}

/**
 * 获取属性的完整 Schema（包括展开 $ref）
 * @param propSchema 属性的 Schema
 * @param rootSchema 根 Schema
 * @returns 完整的属性 Schema
 */
export function getPropertySchema(propSchema: any, rootSchema: any): any {
  return expandSchema(propSchema, rootSchema);
}

/**
 * 为字段生成友好的标签名
 * @param fieldName 字段名
 * @returns 友好的标签
 */
export function generateLabel(fieldName: string): string {
  return fieldName
    .replace(/_/g, ' ')
    .replace(/\b\w/g, c => c.toUpperCase());
}

/**
 * 获取字段的默认值
 * @param schema 字段的 Schema
 * @returns 默认值
 */
export function getDefaultValue(schema: any): any {
  if (schema.default !== undefined) {
    return schema.default;
  }

  switch (schema.type) {
    case 'string':
      return '';
    case 'number':
      return schema.minimum || 0;
    case 'boolean':
      return false;
    case 'array':
      return [];
    case 'object':
      return {};
    default:
      return null;
  }
}
