<template>
  <div class="vehicle-explorer">
    <div class="explorer-header">
      <h3>载具管理</h3>
      <el-button :icon="Plus" size="small" @click="handleAddVehicle">
        新建载具
      </el-button>
    </div>

    <div class="vehicle-list">
      <div
        v-for="vehicle in vehicles"
        :key="vehicle.id"
        class="vehicle-item"
        :class="{ active: selectedVehicleId === vehicle.id }"
        @click="handleSelectVehicle(vehicle)"
      >
        <div class="vehicle-info">
          <el-icon class="vehicle-icon"><Van /></el-icon>
          <div class="vehicle-details">
            <div class="vehicle-name">{{ vehicle.name }}</div>
            <div class="vehicle-id">{{ vehicle.id }}</div>
          </div>
        </div>
        <div class="vehicle-files">
          <el-tag v-if="vehicle.dataFile" size="small" type="success">Data</el-tag>
          <el-tag v-if="vehicle.displayFile" size="small" type="info">Display</el-tag>
          <el-tag v-if="vehicle.textures.length" size="small">{{ vehicle.textures.length }} 贴图</el-tag>
        </div>
      </div>

      <el-empty v-if="vehicles.length === 0" description="暂无载具" />
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {Plus, Van} from '@element-plus/icons-vue';
import {useVehicleStore} from '@/stores/vehicle';
import {useFileSystemStore} from '@/stores/fileSystem';
import type {Vehicle} from '@/types/vehicle';

const vehicleStore = useVehicleStore();
const fileSystemStore = useFileSystemStore();

const vehicles = computed(() => vehicleStore.vehicles);
const selectedVehicleId = computed(() => vehicleStore.selectedVehicleId);

function handleSelectVehicle(vehicle: Vehicle) {
  vehicleStore.selectVehicle(vehicle.id);
  // 打开虚拟 tab 显示载具详情
  fileSystemStore.openVehicleTab(vehicle.id);
}

function handleAddVehicle() {
  // TODO: 实现新建载具对话框
  console.log('Add new vehicle');
}
</script>

<style scoped>
.vehicle-explorer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}

.explorer-header {
  padding: 12px;
  border-bottom: 1px solid var(--el-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.explorer-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.vehicle-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.vehicle-item {
  padding: 12px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 4px;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.vehicle-item:hover {
  background: var(--el-fill-color-light);
}

.vehicle-item.active {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
}

.vehicle-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.vehicle-icon {
  font-size: 20px;
  color: var(--el-color-primary);
}

.vehicle-details {
  flex: 1;
  min-width: 0;
}

.vehicle-name {
  font-weight: 500;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vehicle-id {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vehicle-files {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
</style>
