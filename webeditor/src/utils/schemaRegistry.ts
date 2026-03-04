import vehiclePackMetaSchema from '@/schemas/vehiclePackMeta.schema.json';
import vehicleDisplaySchema from '@/schemas/vehicleDisplay.schema.json';
import vehicleDataSchema from '@/schemas/vehicleData.schema.json';

export interface SchemaInfo {
  schema: any;
  fileMatch: string[];
  uri: string;
  title: string;
}

/**
 * Schema 注册表
 * 将文件路径模式映射到对应的 JSON Schema
 */
export const schemaRegistry: SchemaInfo[] = [
  {
    schema: vehiclePackMetaSchema,
    fileMatch: ['**/vehicle_pack.meta.json', '**/pack.meta.json'],
    uri: 'https://ywzj-vehicle.local/schemas/vehiclePackMeta.schema.json',
    title: 'Vehicle Pack Metadata'
  },
  {
    schema: vehicleDisplaySchema,
    fileMatch: ['**/display/vehicle/*.json'],
    uri: 'https://ywzj-vehicle.local/schemas/vehicleDisplay.schema.json',
    title: 'Vehicle Display Configuration'
  },
  {
    schema: vehicleDataSchema,
    fileMatch: ['**/vehicles/*.json', '**/data/**/vehicles/*.json'],
    uri: 'https://ywzj-vehicle.local/schemas/vehicleData.schema.json',
    title: 'Vehicle Data Configuration'
  }
];

/**
 * 根据文件路径查找匹配的 Schema
 */
export function findSchemaByPath(filePath: string): SchemaInfo | null {
  const normalizedPath = filePath.replace(/\\/g, '/');

  for (const schemaInfo of schemaRegistry) {
    for (const pattern of schemaInfo.fileMatch) {
      if (matchPattern(normalizedPath, pattern)) {
        return schemaInfo;
      }
    }
  }

  return null;
}

/**
 * 简单的路径模式匹配
 * 支持 ** 和 * 通配符
 */
function matchPattern(path: string, pattern: string): boolean {
  // 如果模式以 **/ 开头，则也匹配没有路径前缀的情况
  if (pattern.startsWith('**/')) {
    const fileName = pattern.substring(3); // 去掉 **/
    const pathFileName = path.split('/').pop() || path;

    // 如果路径只是文件名（没有目录），直接比较文件名
    if (!path.includes('/') && fileName === pathFileName) {
      return true;
    }
  }

  // 转换为正则表达式
  const regexPattern = pattern
    .replace(/\./g, '\\.') // 转义点号
    .replace(/\*\*/g, '###DOUBLESTAR###') // 临时标记 **
    .replace(/\*/g, '[^/]*') // * 匹配除 / 外的任意字符
    .replace(/###DOUBLESTAR###/g, '.*'); // ** 匹配任意字符包括 /

  const regex = new RegExp(`^${regexPattern}$`);
  return regex.test(path);
}

/**
 * 获取所有已注册的 Schema URI
 */
export function getAllSchemaUris(): string[] {
  return schemaRegistry.map(info => info.uri);
}

/**
 * 根据 URI 获取 Schema
 */
export function getSchemaByUri(uri: string): any | null {
  const schemaInfo = schemaRegistry.find(info => info.uri === uri);
  return schemaInfo ? schemaInfo.schema : null;
}
