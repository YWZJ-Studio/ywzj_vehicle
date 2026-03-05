import type {FileNode} from '@/types/fileSystem';
import type {Vehicle, VehicleFiles} from '@/types/vehicle';

// 从文件路径提取载具ID
export function extractVehicleId(filePath: string): string | null {
  // data/namespace/vehicles/xxx.json -> xxx
  let match = filePath.match(/data\/[^\/]+\/vehicles\/([^\/]+)\.json$/);
  if (match) return match[1];

  // assets/namespace/display/vehicle/xxx.json -> xxx
  match = filePath.match(/assets\/[^\/]+\/display\/vehicle\/([^\/]+)\.json$/);
  if (match) return match[1];

  return null;
}

// 扫描文件树，构建载具列表
export function scanVehicles(fileTree: FileNode[]): Vehicle[] {
  const files: VehicleFiles = {
    data: new Map(),
    display: new Map(),
    textures: new Map(),
    models: new Map(),
  };

  // 递归扫描文件树
  function scan(nodes: FileNode[], currentPath = '') {
    for (const node of nodes) {
      const path = currentPath ? `${currentPath}/${node.name}` : node.name;

      if (node.type === 'file') {
        // 识别 data 文件: data/namespace/vehicles/xxx.json
        if (path.match(/data\/[^\/]+\/vehicles\/[^\/]+\.json$/)) {
          const id = extractVehicleId(path);
          if (id) files.data.set(id, path);
        }
        // 识别 display 文件: assets/namespace/display/vehicle/xxx.json
        else if (path.match(/assets\/[^\/]+\/display\/vehicle\/[^\/]+\.json$/)) {
          const id = extractVehicleId(path);
          if (id) files.display.set(id, path);
        }
        // 识别模型文件: data/namespace/models/bedrock/vehicle/xxx.json
        else if (path.match(/data\/[^\/]+\/models\/bedrock\/vehicle\/([^\/]+)\.json$/)) {
          const match = path.match(/data\/[^\/]+\/models\/bedrock\/vehicle\/([^\/]+)\.json$/);
          if (match) {
            const id = match[1];
            if (!files.models.has(id)) files.models.set(id, []);
            files.models.get(id)!.push(path);
          }
        }
        // 识别贴图文件: assets/namespace/textures/**/*.png
        else if (path.match(/assets\/[^\/]+\/textures\/.+\.(png|jpg|jpeg)$/)) {
          // 尝试从路径中提取载具ID（假设贴图文件名包含载具ID）
          const match = path.match(/\/([^\/]+)\.(png|jpg|jpeg)$/);
          if (match) {
            const fileName = match[1];
            // 尝试匹配已知的载具ID
            for (const id of files.data.keys()) {
              if (fileName.includes(id)) {
                if (!files.textures.has(id)) files.textures.set(id, []);
                files.textures.get(id)!.push(path);
                break;
              }
            }
          }
        }
      } else if (node.children) {
        scan(node.children, path);
      }
    }
  }

  scan(fileTree);

  // 构建载具列表
  const vehicleIds = new Set([...files.data.keys(), ...files.display.keys()]);
  const vehicles: Vehicle[] = [];

  for (const id of vehicleIds) {
    vehicles.push({
      id,
      name: id.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase()),
      dataFile: files.data.get(id),
      displayFile: files.display.get(id),
      textures: files.textures.get(id) || [],
      models: files.models.get(id) || [],
    });
  }

  return vehicles.sort((a, b) => a.id.localeCompare(b.id));
}
