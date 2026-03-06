<template>
  <div class="ac-editor">
    <!-- toolbar -->
    <div class="ac-toolbar">
      <el-segmented
        v-model="viewMode"
        :options="[
          { label: '混合控制器', value: 'blend' },
          { label: '状态机', value: 'state' },
          { label: '源代码', value: 'source' },
        ]"
        size="small"
      />
      <span class="toolbar-name">{{ controllerName }}</span>

      <div class="toolbar-actions">
        <!-- 撤销/重做 -->
        <el-tooltip content="撤销 (Ctrl+Z)">
          <el-button :icon="RefreshLeft" size="small" :disabled="!canUndo" @click="undo" />
        </el-tooltip>
        <el-tooltip content="重做 (Ctrl+Y)">
          <el-button :icon="RefreshRight" size="small" :disabled="!canRedo" @click="redo" />
        </el-tooltip>

        <!-- 状态机操作 -->
        <template v-if="viewMode === 'state'">
          <el-divider direction="vertical" />
          <el-tooltip content="添加状态">
            <el-button :icon="Plus" size="small" type="primary" @click="showAddStateDialog = true" />
          </el-tooltip>
          <el-tooltip v-if="selectedStateInfo" content="删除选中状态">
            <el-button :icon="Delete" size="small" type="danger" @click="deleteSelectedState" />
          </el-tooltip>
          <el-tooltip v-if="selectedEdgeInfo" content="删除选中转换">
            <el-button :icon="Delete" size="small" type="danger" @click="deleteSelectedEdge" />
          </el-tooltip>
        </template>
      </div>
    </div>

    <!-- source code view -->
    <MonacoEditor
      v-if="viewMode === 'source'"
      :content="content"
      :path="path"
      @update="onSourceUpdate"
    />

    <!-- canvas -->
    <div v-else class="ac-canvas">
      <BlendControllerFlow
        v-if="viewMode === 'blend'"
        :root="root"
        :selected-id="selectedNodeId"
        @select="handleSelect"
        @move-node="handleBlendNodeMove"
      />
      <StateMachineFlow
        v-else
        :root="root"
        :selected-id="selectedNodeId"
        @select="handleSelect"
        @move-node="handleStateNodeMove"
        @add-transition="handleAddTransition"
        @delete-state="handleDeleteState"
        @create-transition="handleCreateTransition"
        @delete-transition="handleDeleteTransition"
        @add-state-at-position="handleAddStateAtPosition"
      />
    </div>

    <!-- 添加状态对话框 -->
    <el-dialog v-model="showAddStateDialog" title="添加状态" width="360px" @closed="onStateDialogClosed">
      <el-form @submit.prevent="confirmAddState">
        <el-form-item label="状态名称">
          <el-input v-model="newStateName" placeholder="例如: idle" autofocus @keyup.enter="confirmAddState" />
        </el-form-item>
        <el-form-item label="所属状态机">
          <el-select v-model="newStateMachine" filterable allow-create style="width: 100%">
            <el-option
              v-for="name in stateMachineNames"
              :key="name"
              :label="name"
              :value="name"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddStateDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!newStateName.trim()" @click="confirmAddState">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加转换对话框 -->
    <el-dialog v-model="showAddTransitionDialog" title="添加转换" width="360px" @closed="resetTransitionForm">
      <el-form>
        <el-form-item label="目标状态">
          <el-select v-model="newTransitionTarget" style="width: 100%">
            <el-option
              v-for="name in transitionTargetOptions"
              :key="name"
              :label="name"
              :value="name"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddTransitionDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!newTransitionTarget" @click="confirmAddTransition">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted } from 'vue';
import { Plus, Delete, RefreshLeft, RefreshRight } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useAnimationControllerEditorStore } from '@/stores/animationControllerEditor';
import type { AnimationSelection } from '@/stores/animationControllerEditor';
import {
  parseAnimationControllerContent,
  patchAnimationControllerSource,
  setValueAtPath,
} from '@/utils/animationControllerGraph';
import type { AnimationControllerRoot } from '@/types/animationController';
import BlendControllerFlow from './animation-controller/BlendControllerFlow.vue';
import StateMachineFlow from './animation-controller/StateMachineFlow.vue';
import MonacoEditor from './MonacoEditor.vue';

interface Props {
  content: string;
  path: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{ update: [content: string] }>();

const editorStore = useAnimationControllerEditorStore();
const ctx = computed(() => editorStore.getContext(props.path));

type ViewMode = 'blend' | 'state' | 'source';
const viewMode = ref<ViewMode>('blend');

// keep store mode in sync when switching between blend/state
watch(viewMode, (v) => {
  if (v === 'blend' || v === 'state') editorStore.setMode(props.path, v);
});

const root = computed<AnimationControllerRoot>(() => {
  try {
    return parseAnimationControllerContent(props.content);
  } catch {
    return {};
  }
});

const controllerName = computed(() => root.value.name ?? props.path.split('/').pop() ?? '');

const selectedNodeId = computed(() => {
  const sel = ctx.value.selection;
  if (!sel) return null;
  if (sel.kind === 'state-node') return sel.nodeId;
  if (sel.kind === 'blend-node') return sel.nodeId;
  if (sel.kind === 'transition-edge') return sel.edgeId;
  return null;
});

const selectedStateInfo = computed(() => {
  const sel = ctx.value.selection;
  if (sel?.kind === 'state-node') return sel;
  return null;
});

const selectedEdgeInfo = computed(() => {
  const sel = ctx.value.selection;
  if (sel?.kind === 'transition-edge') return sel;
  return null;
});

function handleSelect(sel: AnimationSelection | null) {
  editorStore.setSelection(props.path, sel);
}

// ── undo / redo ───────────────────────────────────────────────────────────────

const undoStack = ref<string[]>([]);
const redoStack = ref<string[]>([]);

const canUndo = computed(() => undoStack.value.length > 0);
const canRedo = computed(() => redoStack.value.length > 0);

function pushHistory(before: string) {
  undoStack.value.push(before);
  redoStack.value = [];
}

function undo() {
  if (!canUndo.value) return;
  const prev = undoStack.value.pop()!;
  redoStack.value.push(props.content);
  emit('update', prev);
}

function redo() {
  if (!canRedo.value) return;
  const next = redoStack.value.pop()!;
  undoStack.value.push(props.content);
  emit('update', next);
}

function onKeyDown(e: KeyboardEvent) {
  if (!e.ctrlKey && !e.metaKey) return;
  if (e.key === 'z' || e.key === 'Z') {
    if (e.shiftKey) { e.preventDefault(); redo(); }
    else { e.preventDefault(); undo(); }
  }
  if (e.key === 'y' || e.key === 'Y') { e.preventDefault(); redo(); }
}

onMounted(() => window.addEventListener('keydown', onKeyDown));
onUnmounted(() => window.removeEventListener('keydown', onKeyDown));

// reset history when file changes
watch(() => props.path, () => {
  undoStack.value = [];
  redoStack.value = [];
  editorStore.ensure(props.path);
});

// ── node position persistence ─────────────────────────────────────────────────

function handleStateNodeMove(machineName: string, stateName: string, x: number, y: number) {
  pushHistory(props.content);
  const editorPath = ['state_machines', machineName, 'states', stateName, 'editor'];
  const cur = getNestedValue(root.value, editorPath) ?? {};
  const updated = { ...cur, x: Math.round(x), y: Math.round(y) };
  const smClone = JSON.parse(JSON.stringify(root.value.state_machines ?? {}));
  setValueAtPath(smClone, [machineName, 'states', stateName, 'editor'], updated);
  const patched = patchAnimationControllerSource(props.content, { state_machines: smClone });
  emit('update', patched);
}

function handleBlendNodeMove(jsonPath: Array<string | number>, x: number, y: number) {
  pushHistory(props.content);
  const editorPath = [...jsonPath, 'editor'];
  const cur = getNestedValue(root.value, editorPath) ?? {};
  const updated = { ...cur, x: Math.round(x), y: Math.round(y) };
  const topKey = jsonPath[0] as string;
  const clone = JSON.parse(JSON.stringify((root.value as any)[topKey]));
  setValueAtPath(clone, [...jsonPath.slice(1), 'editor'], updated);
  const patched = patchAnimationControllerSource(props.content, { [topKey]: clone });
  emit('update', patched);
}

// ── add / delete state ────────────────────────────────────────────────────────

const showAddStateDialog = ref(false);
const newStateName = ref('');
const newStateMachine = ref('');
const pendingStatePosition = ref<{ x: number; y: number } | null>(null);

const stateMachineNames = computed(() => Object.keys(root.value.state_machines ?? {}));

watch(showAddStateDialog, (v) => {
  if (v && stateMachineNames.value.length > 0) {
    newStateMachine.value = newStateMachine.value || stateMachineNames.value[0];
  }
});

function onStateDialogClosed() {
  newStateName.value = '';
  pendingStatePosition.value = null;
}

function handleAddStateAtPosition(machineName: string, x: number, y: number) {
  pendingStatePosition.value = { x, y };
  newStateMachine.value = machineName;
  showAddStateDialog.value = true;
}

function confirmAddState() {
  const name = newStateName.value.trim();
  if (!name) return;
  const machineName = newStateMachine.value.trim() || stateMachineNames.value[0];
  if (!machineName) { ElMessage.warning('没有可用的状态机'); return; }

  pushHistory(props.content);
  const smClone = JSON.parse(JSON.stringify(root.value.state_machines ?? {}));

  // 创建状态机（如果不存在）
  if (!smClone[machineName]) {
    smClone[machineName] = { states: {} };
  }
  if (!smClone[machineName].states) {
    smClone[machineName].states = {};
  }

  // 检查状态是否已存在
  if (smClone[machineName].states[name]) {
    ElMessage.warning(`状态 "${name}" 已存在`);
    return;
  }

  // 创建状态，如果有位置信息则设置
  smClone[machineName].states[name] = pendingStatePosition.value
    ? { editor: { x: Math.round(pendingStatePosition.value.x), y: Math.round(pendingStatePosition.value.y) } }
    : {};

  emitPatch({ state_machines: smClone });

  showAddStateDialog.value = false;
  newStateName.value = '';
  pendingStatePosition.value = null;
}

function deleteSelectedState() {
  const sel = selectedStateInfo.value;
  if (!sel) return;
  handleDeleteState(sel.machineName, sel.stateName);
}

function deleteSelectedEdge() {
  const sel = selectedEdgeInfo.value;
  if (!sel) return;
  handleDeleteTransition(sel.machineName, sel.fromState, sel.transitionIndex);
}

function handleDeleteState(machineName: string, stateName: string) {
  pushHistory(props.content);
  const smClone = JSON.parse(JSON.stringify(root.value.state_machines ?? {}));
  delete smClone[machineName]?.states?.[stateName];
  // remove transitions pointing to this state
  for (const machine of Object.values(smClone) as any[]) {
    for (const state of Object.values(machine.states ?? {}) as any[]) {
      if (Array.isArray(state.transitions)) {
        state.transitions = state.transitions.filter((t: any) => t.target !== stateName);
      }
    }
  }
  // remove empty state machine
  if (smClone[machineName] && Object.keys(smClone[machineName].states ?? {}).length === 0) {
    delete smClone[machineName];
  }
  editorStore.setSelection(props.path, null);
  emitPatch({ state_machines: smClone });
}

// ── add transition ────────────────────────────────────────────────────────────

const showAddTransitionDialog = ref(false);
const newTransitionTarget = ref('');
const pendingTransitionFrom = ref<{ machineName: string; stateName: string } | null>(null);

const transitionTargetOptions = computed(() => {
  if (!pendingTransitionFrom.value) return [];
  const { machineName, stateName } = pendingTransitionFrom.value;
  return Object.keys(root.value.state_machines?.[machineName]?.states ?? {})
    .filter(n => n !== stateName);
});

function handleAddTransition(machineName: string, stateName: string) {
  pendingTransitionFrom.value = { machineName, stateName };
  newTransitionTarget.value = '';
  showAddTransitionDialog.value = true;
}

function resetTransitionForm() {
  pendingTransitionFrom.value = null;
  newTransitionTarget.value = '';
}

function confirmAddTransition() {
  const from = pendingTransitionFrom.value;
  if (!from || !newTransitionTarget.value) return;

  pushHistory(props.content);
  const smClone = JSON.parse(JSON.stringify(root.value.state_machines ?? {}));
  const states = smClone[from.machineName]?.states;
  if (!states?.[from.stateName]) return;
  if (!states[from.stateName].transitions) states[from.stateName].transitions = [];
  states[from.stateName].transitions.push({ target: newTransitionTarget.value });
  emitPatch({ state_machines: smClone });

  showAddTransitionDialog.value = false;
}

function handleCreateTransition(machineName: string, fromState: string, toState: string) {
  pushHistory(props.content);
  const smClone = JSON.parse(JSON.stringify(root.value.state_machines ?? {}));
  const states = smClone[machineName]?.states;
  if (!states?.[fromState]) return;
  if (!states[fromState].transitions) states[fromState].transitions = [];
  states[fromState].transitions.push({ target: toState });
  emitPatch({ state_machines: smClone });
}

function handleDeleteTransition(machineName: string, fromState: string, transitionIndex: number) {
  pushHistory(props.content);
  const smClone = JSON.parse(JSON.stringify(root.value.state_machines ?? {}));
  const states = smClone[machineName]?.states;
  if (!states?.[fromState]?.transitions) return;
  states[fromState].transitions.splice(transitionIndex, 1);
  emitPatch({ state_machines: smClone });
}

// ── source view ───────────────────────────────────────────────────────────────

function onSourceUpdate(newContent: string) {
  pushHistory(props.content);
  emit('update', newContent);
}

// ── helpers ───────────────────────────────────────────────────────────────────

function emitPatch(updates: Record<string, any>) {
  const patched = patchAnimationControllerSource(props.content, updates);
  emit('update', patched);
}

function getNestedValue(obj: any, path: Array<string | number>): any {
  let cur = obj;
  for (const k of path) {
    if (cur == null) return undefined;
    cur = cur[k];
  }
  return cur;
}
</script>

<style scoped>
.ac-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.ac-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 16px;
  border-bottom: 1px solid var(--el-border-color);
  flex-shrink: 0;
  background: var(--el-bg-color);
}

.toolbar-name {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-family: 'Consolas', monospace;
  flex: 1;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.ac-canvas {
  flex: 1;
  overflow: hidden;
  position: relative;
}
</style>
