import Ajv from 'ajv';
import addFormats from 'ajv-formats';
import vehiclePackMetaSchema from '@/schemas/vehiclePackMeta.schema.json';
import {findSchemaByPath, schemaRegistry} from './schemaRegistry';
import {parseJsonWithComments} from './jsonParser';

// 创建 Ajv 实例并添加格式验证
const ajv = new Ajv({ allErrors: true, strict: false });
addFormats(ajv);

// 编译 Schema
const validatePackMeta = ajv.compile(vehiclePackMetaSchema);

// 编译所有已注册的 Schema
const compiledSchemas = new Map<string, any>();
schemaRegistry.forEach(schemaInfo => {
  try {
    compiledSchemas.set(schemaInfo.uri, ajv.compile(schemaInfo.schema));
  } catch (err) {
    console.error(`Failed to compile schema ${schemaInfo.title}:`, err);
  }
});

export interface ValidationError {
  field: string;
  message: string;
  value?: any;
}

export interface ValidationResult {
  valid: boolean;
  errors?: ValidationError[];
}

/**
 * 验证载具包元数据
 */
export function validateVehiclePackMeta(data: any): ValidationResult {
  const valid = validatePackMeta(data);

  if (!valid && validatePackMeta.errors) {
    return {
      valid: false,
      errors: validatePackMeta.errors.map(err => ({
        field: err.instancePath || err.params.missingProperty || 'root',
        message: getErrorMessage(err),
        value: err.data
      }))
    };
  }

  return { valid: true };
}

/**
 * 根据文件路径验证 JSON 数据
 * 自动选择匹配的 Schema 进行验证
 */
export function validateByPath(filePath: string, data: any): ValidationResult {
  const schemaInfo = findSchemaByPath(filePath);

  if (!schemaInfo) {
    return {
      valid: true // 没有匹配的 Schema，跳过验证
    };
  }

  const validator = compiledSchemas.get(schemaInfo.uri);
  if (!validator) {
    return {
      valid: true // Schema 未编译，跳过验证
    };
  }

  const valid = validator(data);

  if (!valid && validator.errors) {
    return {
      valid: false,
      errors: validator.errors.map((err: { instancePath: any; params: { missingProperty: any; }; data: any; }) => ({
        field: err.instancePath || err.params.missingProperty || 'root',
        message: getErrorMessage(err),
        value: err.data
      }))
    };
  }

  return { valid: true };
}

/**
 * 验证 JSON 字符串
 */
export function validateJsonString(filePath: string, jsonString: string): ValidationResult {
  try {
    const data = parseJsonWithComments(jsonString);
    return validateByPath(filePath, data);
  } catch (err: any) {
    return {
      valid: false,
      errors: [{
        field: 'root',
        message: `JSON 解析错误: ${err.message}`,
        value: jsonString
      }]
    };
  }
}

/**
 * 获取友好的错误消息
 */
function getErrorMessage(error: any): string {
  const { keyword, params, message } = error;

  switch (keyword) {
    case 'required':
      return `缺少必填字段: ${params.missingProperty}`;

    case 'pattern':
      if (params.pattern === '^[a-z0-9_]+$') {
        return '命名空间只能包含小写字母、数字和下划线';
      }
      if (params.pattern === '^\\d+\\.\\d+\\.\\d+$') {
        return '版本号格式应为: 主版本.次版本.修订号 (例如: 1.0.0)';
      }
      return `格式不正确: ${message}`;

    case 'type':
      return `类型错误: 应为 ${params.type}`;

    case 'format':
      if (params.format === 'uri') {
        return '请输入有效的 URL 地址';
      }
      if (params.format === 'date') {
        return '请输入有效的日期格式 (YYYY-MM-DD)';
      }
      return `格式错误: ${message}`;

    default:
      return message || '验证失败';
  }
}

/**
 * 验证命名空间
 */
export function validateNamespace(namespace: string): boolean {
  return /^[a-z0-9_]+$/.test(namespace);
}

/**
 * 验证版本号
 */
export function validateVersion(version: string): boolean {
  return /^\d+\.\d+\.\d+$/.test(version);
}

/**
 * 验证 URL
 */
export function validateUrl(url: string): boolean {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
}

/**
 * 为 Monaco Editor 配置 JSON Schema
 */
export function configureMonacoJsonSchema(monaco: any) {
  const schemas = schemaRegistry.map(schemaInfo => ({
    uri: schemaInfo.uri,
    fileMatch: schemaInfo.fileMatch,
    schema: schemaInfo.schema
  }));

  monaco.languages.json.jsonDefaults.setDiagnosticsOptions({
    validate: true,
    schemas: schemas,
    enableSchemaRequest: true
  });
}

export { vehiclePackMetaSchema, schemaRegistry, findSchemaByPath };
