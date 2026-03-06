<template>
  <div class="action-list-editor">
    <div class="section-title">
      {{ title }}
      <el-button size="small" text :icon="Plus" @click="addAction">添加</el-button>
    </div>
    <div v-if="!actions.length" class="empty-actions">无动作</div>
    <div
      v-for="(action, idx) in actions"
      :key="idx"
      class="action-item"
    >
      <div class="action-header">
        <el-select
          :model-value="action.type ?? ''"
          size="small"
          style="flex:1"
          @update:model-value="v => setActionType(idx, v)"
        >
          <el-option v-for="t in ACTION_TYPES" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button size="small" text type="danger" :icon="Delete" @click="removeAction(idx)" />
      </div>

      <!-- play_animation -->
      <template v-if="action.type === 'play_animation'">
        <div class="action-field">
          <span class="field-label">animation</span>
          <el-input size="small" :model-value="action.animation ?? ''" @update:model-value="v => setField(idx, 'animation', v)" />
        </div>
        <div class="action-field">
          <span class="field-label">track</span>
          <el-input size="small" :model-value="action.track ?? ''" @update:model-value="v => setField(idx, 'track', v)" />
        </div>
        <div class="action-field">
          <span class="field-label">play_type</span>
          <el-select size="small" :model-value="action.play_type ?? ''" style="width:100%" @update:model-value="v => setField(idx, 'play_type', v)">
            <el-option label="PLAY_ONCE_STOP" value="PLAY_ONCE_STOP" />
            <el-option label="PLAY_ONCE_FREEZE" value="PLAY_ONCE_FREEZE" />
            <el-option label="LOOP" value="LOOP" />
          </el-select>
        </div>
      </template>

      <!-- stop_animation -->
      <template v-else-if="action.type === 'stop_animation'">
        <div class="action-field">
          <span class="field-label">track</span>
          <el-input size="small" :model-value="action.track ?? ''" @update:model-value="v => setField(idx, 'track', v)" />
        </div>
      </template>

      <!-- set_variable -->
      <template v-else-if="action.type === 'set_variable'">
        <div class="action-field">
          <span class="field-label">name</span>
          <el-input size="small" :model-value="action.name ?? ''" @update:model-value="v => setField(idx, 'name', v)" />
        </div>
        <div class="action-field">
          <span class="field-label">value</span>
          <el-input size="small" :model-value="String(action.value ?? '')" @update:model-value="v => setField(idx, 'value', v)" />
        </div>
      </template>

      <!-- play_sound -->
      <template v-else-if="action.type === 'play_sound'">
        <div class="action-field">
          <span class="field-label">sound</span>
          <el-input size="small" :model-value="action.sound ?? ''" @update:model-value="v => setField(idx, 'sound', v)" />
        </div>
      </template>

      <!-- script -->
      <template v-else-if="action.type === 'script'">
        <div class="action-field">
          <span class="field-label">script</span>
          <el-input size="small" type="textarea" :rows="2" :model-value="action.script ?? ''" @update:model-value="v => setField(idx, 'script', v)" />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Delete, Plus } from '@element-plus/icons-vue';
import type { AnimationAction } from '@/types/animationController';

const ACTION_TYPES = ['play_animation', 'stop_animation', 'set_variable', 'play_sound', 'script'];

const props = defineProps<{
  title: string;
  actions: AnimationAction[];
}>();

const emit = defineEmits<{ update: [actions: AnimationAction[]] }>();

function clone(): AnimationAction[] {
  return JSON.parse(JSON.stringify(props.actions));
}

function addAction() {
  const list = clone();
  list.push({ type: 'script', script: '' });
  emit('update', list);
}

function removeAction(idx: number) {
  const list = clone();
  list.splice(idx, 1);
  emit('update', list);
}

function setActionType(idx: number, type: string) {
  const list = clone();
  list[idx] = { type };
  emit('update', list);
}

function setField(idx: number, field: string, value: any) {
  const list = clone();
  (list[idx] as any)[field] = value;
  emit('update', list);
}
</script>

<style scoped>
.action-list-editor {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-primary);
}

.empty-actions {
  padding: 8px 12px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.action-item {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-item:last-child {
  border-bottom: none;
}

.action-header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field-label {
  flex-shrink: 0;
  width: 72px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
</style>
