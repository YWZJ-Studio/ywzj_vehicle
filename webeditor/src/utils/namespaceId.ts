/**
 * 资源类型配置
 */
export interface ResourceTypeConfig {
  /** 资源类型名称（用于显示） */
  name: string;
  /** 资源包类型：'data' 或 'assets' */
  packType: 'data' | 'assets';
  /** category 路径（支持多级，如 'models/bedrock'） */
  category: string;
  /** 支持的文件扩展名列表 */
  extensions: string[];
  /**
   * 资源路径提取策略
   * - 'filename': 只取文件名（不含扩展名），用于数据包
   * - 'fullpath': 保留完整子路径（不含扩展名），用于资源包
   */
  pathStrategy: 'filename' | 'fullpath';
  /** 可选的描述信息 */
  description?: string;
}

/**
 * 资源类型注册表
 */
class ResourceTypeRegistry {
  private configs: ResourceTypeConfig[] = [];

  /**
   * 注册资源类型
   */
  register(config: ResourceTypeConfig): void {
    this.configs.push(config);
  }

  /**
   * 批量注册资源类型
   */
  registerBatch(configs: ResourceTypeConfig[]): void {
    configs.forEach(config => this.register(config));
  }

  /**
   * 获取所有已注册的资源类型
   */
  getAll(): ResourceTypeConfig[] {
    return [...this.configs];
  }

  /**
   * 根据路径查找匹配的资源类型配置
   */
  findMatchingConfig(normalizedPath: string): {
    config: ResourceTypeConfig;
    namespace: string;
    remainingPath: string;
  } | null {
    for (const config of this.configs) {
      const pattern = new RegExp(
        `(?:^|/)${config.packType}/([^/]+)/${config.category.replace(/\//g, '/')}/(.+)$`
      );

      const match = normalizedPath.match(pattern);
      if (match) {
        const namespace = match[1];
        const remainingPath = match[2];

        // 验证文件扩展名
        const ext = remainingPath.split('.').pop()?.toLowerCase();
        if (ext && config.extensions.includes(ext)) {
          return { config, namespace, remainingPath };
        }
      }
    }

    return null;
  }

  /**
   * 清空注册表（主要用于测试）
   */
  clear(): void {
    this.configs = [];
  }
}

// 全局注册表实例
const registry = new ResourceTypeRegistry();

// ==================== 默认资源类型注册 ====================

/**
 * 注册默认的 Minecraft 资源类型
 */
function registerDefaultTypes(): void {
  registry.registerBatch([
    // ===== 数据包 (Data Pack) =====
    {
      name: '载具数据',
      packType: 'data',
      category: 'vehicles',
      extensions: ['json'],
      pathStrategy: 'filename',
      description: '载具配置数据'
    },
    {
      name: '武器数据',
      packType: 'data',
      category: 'weapons',
      extensions: ['json'],
      pathStrategy: 'filename',
      description: '武器配置数据'
    },
    {
      name: '显示配置',
      packType: 'data',
      category: 'display',
      extensions: ['json'],
      pathStrategy: 'filename',
      description: '显示配置数据'
    },
    {
      name: 'Bedrock 结构模型',
      packType: 'data',
      category: 'models/bedrock',
      extensions: ['json'],
      pathStrategy: 'fullpath',
      description: 'Bedrock 版模型文件'
    },

    // ===== 资源包 (Assets) =====
    {
      name: '纹理',
      packType: 'assets',
      category: 'textures',
      extensions: ['png', 'jpg', 'jpeg', 'gif'],
      pathStrategy: 'fullpath',
      description: '纹理图片资源'
    },
    {
      name: 'Bedrock 模型',
      packType: 'assets',
      category: 'models/bedrock',
      extensions: ['json'],
      pathStrategy: 'fullpath',
      description: 'Bedrock 版模型文件'
    },
    {
      name: '音效',
      packType: 'assets',
      category: 'sounds',
      extensions: ['ogg', 'mp3', 'wav'],
      pathStrategy: 'fullpath',
      description: '音频文件'
    },
    {
      name: '显示配置',
      packType: 'assets',
      category: 'display',
      extensions: ['json'],
      pathStrategy: 'fullpath',
      description: '客户端显示配置'
    },
  ]);
}

// 自动注册默认类型
registerDefaultTypes();

// ==================== 公开 API ====================

/**
 * 将文件路径转换为 Minecraft 命名空间 ID
 *
 * @param path 文件路径，格式如：src/main/resources/data/ywzj_vehicle/vehicles/ah64d.json
 * @returns 命名空间 ID，格式如：ywzj_vehicle:ah64d，如果不匹配则返回 null
 */
export function pathToNamespaceId(path: string): string | null {
  // 标准化路径分隔符
  const normalizedPath = path.replace(/\\/g, '/');

  // 查找匹配的资源类型配置
  const result = registry.findMatchingConfig(normalizedPath);
  if (!result) {
    return null;
  }

  const { config, namespace, remainingPath } = result;

  // 移除文件扩展名
  const pathWithoutExt = remainingPath.replace(/\.[^.]+$/, '');

  // 根据策略提取资源路径
  let resourcePath: string;
  if (config.pathStrategy === 'filename') {
    // 只取最后的文件名
    resourcePath = pathWithoutExt.split('/').pop() || pathWithoutExt;
  } else {
    // 保留完整路径
    resourcePath = pathWithoutExt;
  }

  return `${namespace}:${resourcePath}`;
}

/**
 * 获取文件的资源类型信息
 *
 * @param path 文件路径
 * @returns 资源类型配置，如果不匹配则返回 null
 */
export function getResourceTypeInfo(path: string): ResourceTypeConfig | null {
  const normalizedPath = path.replace(/\\/g, '/');
  const result = registry.findMatchingConfig(normalizedPath);
  return result ? result.config : null;
}

/**
 * 获取文件的资源类型名称（category）
 *
 * @param path 文件路径
 * @returns 资源类型，如 vehicles, weapons, models/bedrock 等，如果不匹配则返回 null
 */
export function getResourceCategory(path: string): string | null {
  const info = getResourceTypeInfo(path);
  return info ? info.category : null;
}

/**
 * 获取文件的命名空间
 *
 * @param path 文件路径
 * @returns 命名空间，如 ywzj_vehicle，如果不匹配则返回 null
 */
export function getNamespace(path: string): string | null {
  const normalizedPath = path.replace(/\\/g, '/');
  const result = registry.findMatchingConfig(normalizedPath);
  return result ? result.namespace : null;
}

/**
 * 获取资源类型注册表（用于扩展）
 */
export function getRegistry(): ResourceTypeRegistry {
  return registry;
}

/**
 * 注册自定义资源类型
 *
 * @param config 资源类型配置
 *
 * @example
 * ```typescript
 * registerResourceType({
 *   name: '自定义资源',
 *   packType: 'data',
 *   category: 'custom/items',
 *   extensions: ['json'],
 *   pathStrategy: 'filename',
 *   description: '自定义物品数据'
 * });
 * ```
 */
export function registerResourceType(config: ResourceTypeConfig): void {
  registry.register(config);
}

/**
 * 批量注册自定义资源类型
 *
 * @param configs 资源类型配置数组
 */
export function registerResourceTypes(configs: ResourceTypeConfig[]): void {
  registry.registerBatch(configs);
}

/**
 * 获取所有已注册的资源类型
 */
export function getAllResourceTypes(): ResourceTypeConfig[] {
  return registry.getAll();
}
