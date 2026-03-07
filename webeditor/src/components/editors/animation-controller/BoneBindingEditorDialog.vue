<template>
  <el-dialog
    :model-value="visible"
    class="bone-binding-dialog"
    :lock-scroll="true"
    :body-style="{ maxHeight: '70vh', overflowY: 'auto' }"
    title="骨骼绑定编辑器"
    width="960px"
    top="5vh"
    destroy-on-close
    @update:model-value="onVisibleChange"
  >
    <div class="bone-binding-editor">
      <!-- Special Bindings -->
      <div class="binding-section">
        <div class="section-header">
          <span class="section-title">特殊绑定 (special_bindings)</span>
          <el-button size="small" :icon="Plus" @click="addSpecialBinding">添加</el-button>
        </div>
        <el-table
          v-if="localSpecialBindings.length"
          :data="localSpecialBindings"
          border
          size="small"
          class="binding-table"
        >
          <el-table-column label="bones" min-width="200">
            <template #default="{ row }">
              <el-input
                :model-value="(row.bones ?? []).join(', ')"
                size="small"
                placeholder="逗号分隔骨骼名"
                @change="(v: string) => row.bones = parseBonesList(v)"
              />
            </template>
          </el-table-column>
          <el-table-column label="source" width="180">
            <template #default="{ row }">
              <el-input v-model="row.source" size="small" placeholder="例如: left_wheel_rotation" />
            </template>
          </el-table-column>
          <el-table-column label="axis" width="90">
            <template #default="{ row }">
              <el-select v-model="row.axis" size="small" style="width:100%">
                <el-option label="x" value="x" />
                <el-option label="y" value="y" />
                <el-option label="z" value="z" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="param" width="100">
            <template #default="{ row }">
              <el-input-number
                v-model="row.param"
                size="small"
                :step="0.01"
                :precision="4"
                controls-position="right"
                style="width:100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button
                :icon="Delete"
                size="small"
                type="danger"
                link
                @click="localSpecialBindings.splice($index, 1)"
              />
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-hint">暂无特殊绑定</div>
      </div>

      <!-- Part Bindings -->
      <div class="binding-section">
        <div class="section-header">
          <span class="section-title">部件绑定 (part_bindings)</span>
          <el-button size="small" :icon="Plus" @click="addPartBinding">添加</el-button>
        </div>
        <el-table
          v-if="localPartBindings.length"
          :data="localPartBindings"
          border
          size="small"
          class="binding-table"
        >
          <el-table-column label="bone" width="150">
            <template #default="{ row }">
              <el-input v-model="row.bone" size="small" placeholder="骨骼名" />
            </template>
          </el-table-column>
          <el-table-column label="part" width="180">
            <template #default="{ row }">
              <el-input v-model="row.part" size="small" placeholder="部件名" />
            </template>
          </el-table-column>
          <el-table-column label="rotation_type" width="120">
            <template #default="{ row }">
              <el-select v-model="row.rotation_type" size="small" style="width:100%">
                <el-option label="x" value="x" />
                <el-option label="y" value="y" />
                <el-option label="z" value="z" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="axis" width="90">
            <template #default="{ row }">
              <el-select v-model="row.axis" size="small" style="width:100%">
                <el-option label="x" value="x" />
                <el-option label="y" value="y" />
                <el-option label="z" value="z" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="invert" width="80" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.invert" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button
                :icon="Delete"
                size="small"
                type="danger"
                link
                @click="localPartBindings.splice($index, 1)"
              />
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-hint">暂无部件绑定</div>
      </div>
    </div>

    <template #footer>
      <el-button @click="onVisibleChange(false)">取消</el-button>
      <el-button type="primary" @click="confirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Plus, Delete } from '@element-plus/icons-vue';
import type {
  BoneBindingSpecialBinding,
  BoneBindingPartBinding,
} from '@/types/animationController';

interface Props {
  visible: boolean;
  specialBindings?: BoneBindingSpecialBinding[];
  partBindings?: BoneBindingPartBinding[];
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:visible': [value: boolean];
  confirm: [special: BoneBindingSpecialBinding[], part: BoneBindingPartBinding[]];
}>();

const localSpecialBindings = ref<BoneBindingSpecialBinding[]>([]);
const localPartBindings = ref<BoneBindingPartBinding[]>([]);

watch(() => props.visible, (v) => {
  if (v) {
    localSpecialBindings.value = JSON.parse(JSON.stringify(props.specialBindings ?? []));
    localPartBindings.value = JSON.parse(JSON.stringify(props.partBindings ?? []));
  }
});

function parseBonesList(value: string): string[] {
  return value.split(/[,，\s]+/).map(s => s.trim()).filter(Boolean);
}

function addSpecialBinding() {
  localSpecialBindings.value.push({ bones: [], source: '', axis: 'x', param: 0 });
}

function addPartBinding() {
  localPartBindings.value.push({ bone: '', part: '', rotation_type: 'y', axis: 'y', invert: false });
}

function confirm() {
  emit('confirm', localSpecialBindings.value, localPartBindings.value);
  onVisibleChange(false);
}

function onVisibleChange(v: boolean) {
  emit('update:visible', v);
}
</script>

<style scoped>
.bone-binding-editor {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.binding-section {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.binding-table {
  width: 100%;
}

.empty-hint {
  padding: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--el-text-color-placeholder);
}

</style>
