<template>
  <div class="node-props">
    <div v-if="!selection" class="empty-hint">
      <el-icon size="32" color="var(--el-text-color-placeholder)"><Connection /></el-icon>
      <p>在画布中选择节点或连线以编辑属性</p>
    </div>

    <!-- State node -->
    <template v-else-if="selection.kind === 'state-node'">
      <div class="props-section">
        <div class="section-title">状态：{{ selection.stateName }}</div>
        <div class="field-row">
          <span class="field-label">所属状态机</span>
          <span class="field-value mono">{{ selection.machineName }}</span>
        </div>
        <div class="field-row">
          <span class="field-label">是否起始状态</span>
          <el-switch :model-value="isStartState" @update:model-value="toggleStartState" />
        </div>
      </div>

      <div class="props-section">
        <div class="section-title">evaluate</div>
        <div class="field-row">
          <span class="field-label">类型</span>
          <el-select
            :model-value="stateData?.evaluate?.type ?? ''"
            size="small"
            style="width:100%"
            clearable
            placeholder="(无)"
            @update:model-value="setEvaluateType"
          >
            <el-option label="track" value="track" />
            <el-option label="script" value="script" />
          </el-select>
        </div>
        <div v-if="stateData?.evaluate?.type === 'track'" class="field-row">
          <span class="field-label">track</span>
          <el-input
            :model-value="stateData?.evaluate?.track ?? ''"
            size="small"
            @update:model-value="v => setEvaluateField('track', v)"
          />
        </div>
        <div v-if="stateData?.evaluate?.type === 'script'" class="field-row">
          <span class="field-label">script</span>
          <el-input
            :model-value="stateData?.evaluate?.script ?? ''"
            size="small"
            type="textarea"
            :rows="3"
            @update:model-value="v => setEvaluateField('script', v)"
          />
        </div>
      </div>

      <div class="props-section">
        <div class="section-title">注释</div>
        <el-input
          :model-value="stateData?.editor?.comment ?? ''"
          size="small"
          type="textarea"
          :rows="2"
          placeholder="可选注释"
          @update:model-value="v => setEditorMeta('comment', v)"
        />
      </div>

      <ActionListEditor
        title="on_enter"
        :actions="stateData?.on_enter ?? []"
        @update="v => setStateField('on_enter', v)"
      />
      <ActionListEditor
        title="on_update"
        :actions="stateData?.on_update ?? []"
        @update="v => setStateField('on_update', v)"
      />
      <ActionListEditor
        title="on_exit"
        :actions="stateData?.on_exit ?? []"
        @update="v => setStateField('on_exit', v)"
      />
    </template>

    <!-- Transition edge -->
    <template v-else-if="selection.kind === 'transition-edge'">
      <div class="props-section">
        <div class="section-title">
          过渡：{{ selection.fromState }} → {{ selection.toState }}
        </div>
        <div class="field-row">
          <span class="field-label">目标状态</span>
          <el-select
            :model-value="transitionData?.target ?? ''"
            size="small"
            style="width:100%"
            @update:model-value="v => setTransitionField('target', v)"
          >
            <el-option
              v-for="s in siblingStateNames"
              :key="s"
              :label="s"
              :value="s"
            />
          </el-select>
        </div>
        <div class="field-row">
          <span class="field-label">持续时间(s)</span>
          <el-input-number
            :model-value="transitionData?.duration ?? 0"
            size="small"
            :min="0"
            :step="0.05"
            :precision="3"
            style="width:100%"
            @update:model-value="v => setTransitionField('duration', v)"
          />
        </div>
        <div class="field-row">
          <span class="field-label">混合曲线</span>
          <el-select
            :model-value="transitionData?.blend_curve ?? ''"
            size="small"
            style="width:100%"
            clearable
            placeholder="linear"
            @update:model-value="v => setTransitionField('blend_curve', v || undefined)"
          >
            <el-option label="linear" value="linear" />
            <el-option label="ease_in" value="ease_in" />
            <el-option label="ease_out" value="ease_out" />
            <el-option label="ease_in_out" value="ease_in_out" />
          </el-select>
        </div>
      </div>

      <div class="props-section">
        <div class="section-title">条件</div>
        <ConditionEditor
          :condition="transitionData?.condition ?? null"
          @update="v => setTransitionField('condition', v)"
        />
      </div>

      <ActionListEditor
        title="after_trigger"
        :actions="transitionData?.after_trigger ?? []"
        @update="v => setTransitionField('after_trigger', v)"
      />
    </template>

    <!-- Blend node -->
    <template v-else-if="selection.kind === 'blend-node'">
      <div class="props-section">
        <div class="section-title">混合节点</div>
        <div class="field-row">
          <span class="field-label">类型</span>
          <el-select
            :model-value="blendData?.type ?? ''"
            size="small"
            style="width:100%"
            @update:model-value="v => setBlendField('type', v)"
          >
            <el-option v-for="t in BLEND_NODE_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </div>

        <!-- ref field for leaf nodes -->
        <div v-if="isLeafBlendNode" class="field-row">
          <span class="field-label">ref</span>
          <el-input
            :model-value="blendData?.ref ?? ''"
            size="small"
            @update:model-value="v => setBlendField('ref', v)"
          />
        </div>

        <!-- weight for blend node -->
        <div v-if="blendData?.type === 'blend'" class="field-row">
          <span class="field-label">weight</span>
          <el-input
            :model-value="String(blendData?.weight ?? '')"
            size="small"
            placeholder="数值或脚本表达式"
            @update:model-value="v => setBlendField('weight', v)"
          />
        </div>

        <div class="field-row">
          <span class="field-label">注释</span>
          <el-input
            :model-value="blendData?.editor?.comment ?? ''"
            size="small"
            @update:model-value="v => setBlendEditorMeta('comment', v)"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Connection } from '@element-plus/icons-vue';
import type { AnimationSelection } from '@/stores/animationControllerEditor';
import type {
  AnimationControllerRoot,
  AnimationAction,
  AnimationCondition,
} from '@/types/animationController';
import { getValueAtPath } from '@/utils/animationControllerGraph';
import ActionListEditor from './ActionListEditor.vue';
import ConditionEditor from './ConditionEditor.vue';

const BLEND_NODE_TYPES = [
  'state_machine', 'switchable_animation', 'loop_animation',
  'track_animation', 'script', 'blend', 'additive',
  'layered_blend', 'merge', 'bone_binding',
];

const LEAF_BLEND_TYPES = new Set([
  'state_machine', 'switchable_animation', 'loop_animation', 'track_animation',
]);

interface Props {
  selection: AnimationSelection | null;
  root: AnimationControllerRoot;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  patch: [updates: Record<string, any>];
}>();

// ── helpers ──────────────────────────────────────────────────────────────────

function deepClone<T>(v: T): T {
  return JSON.parse(JSON.stringify(v));
}

function patchRoot(path: Array<string | number>, value: any) {
  // Build a minimal nested update object that mirrors the path
  const rootClone = deepClone(props.root);
  let cur: any = rootClone;
  for (let i = 0; i < path.length - 1; i++) {
    const k = path[i];
    if (cur[k] == null || typeof cur[k] !== 'object') {
      cur[k] = typeof path[i + 1] === 'number' ? [] : {};
    }
    cur = cur[k];
  }
  const last = path[path.length - 1];
  if (value === undefined) {
    delete cur[last];
  } else {
    cur[last] = value;
  }
  // Emit only the top-level key that changed
  const topKey = path[0] as string;
  emit('patch', { [topKey]: (rootClone as any)[topKey] });
}

// ── state node ───────────────────────────────────────────────────────────────

const stateData = computed(() => {
  if (props.selection?.kind !== 'state-node') return null;
  return getValueAtPath(props.root, props.selection.jsonPath) ?? null;
});

const isStartState = computed(() => {
  if (props.selection?.kind !== 'state-node') return false;
  const machine = props.root.state_machines?.[props.selection.machineName];
  return machine?.start_state === props.selection.stateName;
});

function toggleStartState(val: boolean) {
  if (props.selection?.kind !== 'state-node') return;
  const { machineName, stateName } = props.selection;
  const machine = deepClone(props.root.state_machines?.[machineName] ?? {});
  machine.start_state = val ? stateName : '';
  emit('patch', { state_machines: { ...deepClone(props.root.state_machines), [machineName]: machine } });
}

function setStateField(field: string, value: any) {
  if (props.selection?.kind !== 'state-node') return;
  patchRoot([...props.selection.jsonPath, field], value);
}

function setEvaluateType(type: string) {
  if (props.selection?.kind !== 'state-node') return;
  const cur = deepClone(stateData.value?.evaluate ?? {});
  cur.type = type || undefined;
  patchRoot([...props.selection.jsonPath, 'evaluate'], type ? cur : undefined);
}

function setEvaluateField(field: string, value: string) {
  if (props.selection?.kind !== 'state-node') return;
  patchRoot([...props.selection.jsonPath, 'evaluate', field], value);
}

function setEditorMeta(field: string, value: string) {
  if (props.selection?.kind !== 'state-node') return;
  patchRoot([...props.selection.jsonPath, 'editor', field], value || undefined);
}

// ── transition edge ───────────────────────────────────────────────────────────

const transitionData = computed(() => {
  if (props.selection?.kind !== 'transition-edge') return null;
  return getValueAtPath(props.root, props.selection.jsonPath) ?? null;
});

const siblingStateNames = computed(() => {
  if (props.selection?.kind !== 'transition-edge') return [];
  return Object.keys(props.root.state_machines?.[props.selection.machineName]?.states ?? {});
});

function setTransitionField(field: string, value: any) {
  if (props.selection?.kind !== 'transition-edge') return;
  patchRoot([...props.selection.jsonPath, field], value);
}

// ── blend node ────────────────────────────────────────────────────────────────

const blendData = computed(() => {
  if (props.selection?.kind !== 'blend-node') return null;
  return getValueAtPath(props.root, props.selection.jsonPath) ?? null;
});

const isLeafBlendNode = computed(() =>
  LEAF_BLEND_TYPES.has(blendData.value?.type ?? ''),
);

function setBlendField(field: string, value: any) {
  if (props.selection?.kind !== 'blend-node') return;
  patchRoot([...props.selection.jsonPath, field], value);
}

function setBlendEditorMeta(field: string, value: string) {
  if (props.selection?.kind !== 'blend-node') return;
  patchRoot([...props.selection.jsonPath, 'editor', field], value || undefined);
}
</script>

<style scoped>
.node-props {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty-hint {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  text-align: center;
}

.props-section {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
}

.section-title {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-primary);
}

.field-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.field-row:last-child {
  border-bottom: none;
}

.field-label {
  flex-shrink: 0;
  width: 80px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.field-value {
  flex: 1;
  font-size: 12px;
  color: var(--el-text-color-primary);
  word-break: break-all;
}

.mono {
  font-family: 'Consolas', 'Monaco', monospace;
}
</style>
