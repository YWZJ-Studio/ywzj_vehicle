// 载具实体类型定义
export interface Vehicle {
  id: string; // 载具ID（命名空间ID，如 "ywzj:car"）
  name: string; // 显示名称
  dataFile?: string; // data文件路径
  displayFile?: string; // display文件路径
  textures: string[]; // 关联的贴图文件路径
  models: string[]; // 关联的模型文件路径
}

// 载具文件关联
export interface VehicleFiles {
  data: Map<string, string>; // id -> 文件路径
  display: Map<string, string>; // id -> 文件路径
  textures: Map<string, string[]>; // id -> 贴图路径数组
  models: Map<string, string[]>; // id -> 模型路径数组
}
