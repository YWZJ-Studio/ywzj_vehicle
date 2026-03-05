import {defineStore} from 'pinia';
import {computed, ref} from 'vue';
import type {Vehicle} from '@/types/vehicle';
import {scanVehicles} from '@/utils/vehicleScanner';
import {useFileSystemStore} from './fileSystem';

export const useVehicleStore = defineStore('vehicle', () => {
  const fileSystemStore = useFileSystemStore();

  const vehicles = ref<Vehicle[]>([]);
  const selectedVehicleId = ref<string | null>(null);

  // 扫描并更新载具列表
  function refreshVehicles() {
    vehicles.value = scanVehicles(fileSystemStore.fileTree);
  }

  // 选中的载具
  const selectedVehicle = computed(() =>
    vehicles.value.find(v => v.id === selectedVehicleId.value)
  );

  // 选择载具
  function selectVehicle(id: string) {
    selectedVehicleId.value = id;
  }

  // 打开载具的所有相关文件
  async function openVehicleFiles(vehicleId: string) {
    const vehicle = vehicles.value.find(v => v.id === vehicleId);
    if (!vehicle) return;

    const filesToOpen = [
      vehicle.dataFile,
      vehicle.displayFile,
    ].filter(Boolean) as string[];

    for (const path of filesToOpen) {
      const node = findFileNode(fileSystemStore.fileTree, path);
      if (node?.handle) {
        await fileSystemStore.openFile(path, node.handle as FileSystemFileHandle);
      }
    }
  }

  // 辅助函数：查找文件节点
  function findFileNode(nodes: any[], targetPath: string, currentPath = ''): any {
    for (const node of nodes) {
      const path = currentPath ? `${currentPath}/${node.name}` : node.name;
      if (path === targetPath) return node;
      if (node.children) {
        const found = findFileNode(node.children, targetPath, path);
        if (found) return found;
      }
    }
    return null;
  }

  return {
    vehicles,
    selectedVehicleId,
    selectedVehicle,
    refreshVehicles,
    selectVehicle,
    openVehicleFiles,
  };
});
