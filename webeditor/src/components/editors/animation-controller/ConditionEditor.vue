<template>
  <div class="condition-editor">
    <div v-if="!condition" class="no-condition">
      <el-button size="small" text :icon="Plus" @click="addCondition">添加条件</el-button>
    </div>
    <template v-else>
      <div class="cond-row">
        <el-select
          :model-value="condition.type ?? 'script'"
          size="small"
          style="width:110px;flex-shrink:0"
          @update:model-value="setType"
        >
          <el-option label="script" value="script" />
          <el-option label="and" value="and" />
          <el-option label="or" value="or" />
          <el-option label="not" value="not" />
        </el-select>

        <el-input
          v-if="condition.type === 'script' || !condition.type"
          :model-value="condition.script ?? ''"
          size="small"
          placeholder="JS 表达式"
          style="flex:1"
          @update:model-value="(v: any) => setField('script', v)"
        />

        <el-button size="small" text type="danger" :icon="Delete" @click="emit('update', null)" />
      </div>

      <!-- and / or: list of sub-conditions (simplified: show count) -->
      <div v-if="condition.type === 'and' || condition.type === 'or'" class="sub-note">
        子条件数量：{{ (condition.conditions ?? []).length }}
        <el-button size="small" text @click="addSubCondition">+</el-button>
      </div>

      <!-- not: single sub-condition script -->
      <div v-if="condition.type === 'not'" class="cond-row sub-row">
        <span class="sub-label">NOT</span>
        <el-input
          :model-value="condition.condition?.script ?? ''"
          size="small"
          placeholder="JS 表达式"
          style="flex:1"
          @update:model-value="(v: string) => setNotScript(v)"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { Delete, Plus } from '@element-plus/icons-vue';
import type { AnimationCondition } from '@/types/animationController';

const props = defineProps<{ condition: AnimationCondition | null }>();
const emit = defineEmits<{ update: [cond: AnimationCondition | null] }>();

function clone(): AnimationCondition {
  return JSON.parse(JSON.stringify(props.condition ?? {}));
}

function addCondition() {
  emit('update', { type: 'script', script: '' });
}

function setType(type: string) {
  const base: AnimationCondition = { type };
  if (type === 'and' || type === 'or') base.conditions = [];
  if (type === 'not') base.condition = { type: 'script', script: '' };
  emit('update', base);
}

function setField(field: string, value: any) {
  const c = clone();
  (c as any)[field] = value;
  emit('update', c);
}

function setNotScript(v: string) {
  const c = clone();
  c.condition = { type: 'script', script: v };
  emit('update', c);
}

function addSubCondition() {
  const c = clone();
  if (!Array.isArray(c.conditions)) c.conditions = [];
  c.conditions.push({ type: 'script', script: '' });
  emit('update', c);
}
</script>

<style scoped>
.condition-editor {
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.no-condition {
  display: flex;
  align-items: center;
}

.cond-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sub-row {
  padding-left: 16px;
}

.sub-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
  width: 32px;
}

.sub-note {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
  padding-left: 4px;
}
</style>
