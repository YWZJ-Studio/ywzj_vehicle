<template>
  <div class="flow-wrap">
    <VueFlow
      v-if="nodes.length"
      :nodes="computedNodes"
      :edges="computedEdges"
      :default-edge-options="{ type: 'default', animated: false }"
      class="flow-canvas"
      @node-click="onNodeClick"
      @edge-click="onEdgeClick"
      @pane-click="onPaneClick"
      @node-drag-stop="onNodeDragStop"
      @node-context-menu="onNodeContextMenu"
      @edge-context-menu="onEdgeContextMenu"
      @pane-context-menu="onPaneContextMenu"
    >
      <Background pattern-color="#aaa" :gap="16" />
      <Controls />

      <template #node-default="{ data, id }">
        <div
          class="state-node"
          :class="{
            'is-start': data.stateName === startStateMap[data.machineName],
            'is-selected': selectedId === id,
          }"
        >
          <div class="node-header">
            <span class="node-title">{{ data.label }}</span>
            <span v-if="data.stateName === startStateMap[data.machineName]" class="start-badge">START</span>
          </div>
          <div class="node-body">
            <div class="handle-container input">
              <Handle type="target" :position="Position.Left" class="ue-handle" />
              <span class="pin-label">In</span>
            </div>
            <div v-if="getTransitions(data).length" class="transition-list">
              <div
                v-for="(trans, idx) in getTransitions(data)"
                :key="idx"
                class="handle-container output"
              >
                <span class="pin-label">To: {{ trans.target }}</span>
                <Handle
                  type="source"
                  :position="Position.Right"
                  :id="`trans-${idx}`"
                  class="ue-handle"
                />
              </div>
            </div>
            <div class="handle-container output add-trans">
              <span class="pin-label">+ Add Transition</span>
              <Handle
                type="source"
                :position="Position.Right"
                id="new-trans"
                class="ue-handle"
              />
            </div>
          </div>
        </div>
      </template>

      <template #edge-transition="edgeProps">
        <TransitionEdge v-bind="edgeProps" />
      </template>
    </VueFlow>

    <div v-else class="empty-graph">
      <el-empty description="该控制器没有状态机定义，点击右上角添加第一个节点" />
    </div>

    <!-- 节点右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @mouseleave="contextMenu.visible = false"
    >
      <div class="context-menu-item" @click="onContextAddTransition">添加转换</div>
      <div class="context-menu-item danger" @click="onContextDeleteState">删除状态</div>
    </div>

    <!-- 连线右键菜单 -->
    <div
      v-if="edgeContextMenu.visible"
      class="context-menu"
      :style="{ left: edgeContextMenu.x + 'px', top: edgeContextMenu.y + 'px' }"
      @mouseleave="edgeContextMenu.visible = false"
    >
      <div class="context-menu-item danger" @click="onContextDeleteEdge">删除转换</div>
    </div>

    <!-- 画布右键菜单 -->
    <div
      v-if="paneContextMenu.visible"
      class="context-menu"
      :style="{ left: paneContextMenu.x + 'px', top: paneContextMenu.y + 'px' }"
      @mouseleave="paneContextMenu.visible = false"
    >
      <div class="context-menu-item" @click="onContextAddState">添加状态</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, reactive } from 'vue';
import { VueFlow, Handle, Position, useVueFlow } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import type { NodeMouseEvent, EdgeMouseEvent, NodeDragEvent, Connection, EdgeChange } from '@vue-flow/core';
import { buildStateGraph } from '@/utils/animationControllerGraph';
import type { AnimationControllerRoot } from '@/types/animationController';
import type { AnimationSelection } from '@/stores/animationControllerEditor';
import TransitionEdge from './TransitionEdge.vue';

const props = defineProps<{
  root: AnimationControllerRoot;
  selectedId: string | null;
}>();

const emit = defineEmits<{
  select: [sel: AnimationSelection | null];
  moveNode: [machineName: string, stateName: string, x: number, y: number];
  addTransition: [machineName: string, stateName: string];
  deleteState: [machineName: string, stateName: string];
  createTransition: [machineName: string, fromState: string, toState: string];
  deleteTransition: [machineName: string, fromState: string, transitionIndex: number];
  addStateAtPosition: [machineName: string, x: number, y: number];
}>();

const { onConnect, onEdgesChange, applyEdgeChanges, project } = useVueFlow();

const graphRef = ref(buildStateGraph(props.root));
watch(() => props.root, () => { graphRef.value = buildStateGraph(props.root); }, { deep: true });

const nodes = computed(() => graphRef.value.nodes);
const edges = computed(() => graphRef.value.edges);

// highlight related edges/nodes based on selection
const computedNodes = computed(() => {
  const sel = props.selectedId;
  if (!sel) return nodes.value;

  const selectedEdge = edges.value.find(e => e.id === sel);
  if (!selectedEdge) return nodes.value;

  return nodes.value.map(n => ({
    ...n,
    class: (n.id === selectedEdge.source || n.id === selectedEdge.target) ? 'highlighted' : '',
  }));
});

const computedEdges = computed(() => {
  const sel = props.selectedId;

  return edges.value.map(e => {
    let isHighlighted = false;

    if (sel) {
      const selectedNode = nodes.value.find(n => n.id === sel);
      if (selectedNode) {
        isHighlighted = e.source === sel || e.target === sel;
      } else {
        isHighlighted = e.id === sel;
      }
    }

    return {
      ...e,
      type: 'transition',
      style: isHighlighted
        ? { stroke: '#ffd700', strokeWidth: 3 }
        : { stroke: 'rgba(150, 150, 170, 0.4)', strokeWidth: 1.5 },
      animated: isHighlighted,
      data: {
        ...e.data,
        onDelete: () => handleDeleteEdge(e.id),
      },
    };
  });
});

const startStateMap = computed(() => {
  const map: Record<string, string> = {};
  for (const [name, machine] of Object.entries(props.root.state_machines ?? {})) {
    if (machine.start_state) map[name] = machine.start_state;
  }
  return map;
});

function getTransitions(data: any) {
  const state = props.root.state_machines?.[data.machineName]?.states?.[data.stateName];
  return state?.transitions ?? [];
}

// ── context menu ──────────────────────────────────────────────────────────────

const contextMenu = reactive({ visible: false, x: 0, y: 0, machineName: '', stateName: '' });
const edgeContextMenu = reactive({ visible: false, x: 0, y: 0, edgeId: '' });
const paneContextMenu = reactive({ visible: false, x: 0, y: 0, flowX: 0, flowY: 0 });

function onNodeContextMenu(e: NodeMouseEvent) {
  e.event.preventDefault();
  edgeContextMenu.visible = false;
  paneContextMenu.visible = false;
  const data = e.node.data;
  contextMenu.machineName = data.machineName;
  contextMenu.stateName = data.stateName;
  contextMenu.x = (e.event as MouseEvent).clientX;
  contextMenu.y = (e.event as MouseEvent).clientY;
  contextMenu.visible = true;
}

function onEdgeContextMenu(e: EdgeMouseEvent) {
  e.event.preventDefault();
  contextMenu.visible = false;
  paneContextMenu.visible = false;
  edgeContextMenu.edgeId = e.edge.id;
  edgeContextMenu.x = (e.event as MouseEvent).clientX;
  edgeContextMenu.y = (e.event as MouseEvent).clientY;
  edgeContextMenu.visible = true;
}

function onPaneContextMenu(e: MouseEvent) {
  e.preventDefault();
  contextMenu.visible = false;
  edgeContextMenu.visible = false;
  const target = e.currentTarget as HTMLElement;
  const rect = target.getBoundingClientRect();
  const flowCoords = project({ x: e.clientX - rect.left, y: e.clientY - rect.top });
  paneContextMenu.flowX = flowCoords.x;
  paneContextMenu.flowY = flowCoords.y;
  paneContextMenu.x = e.clientX;
  paneContextMenu.y = e.clientY;
  paneContextMenu.visible = true;
}

function onContextAddTransition() {
  contextMenu.visible = false;
  emit('addTransition', contextMenu.machineName, contextMenu.stateName);
}

function onContextDeleteState() {
  contextMenu.visible = false;
  emit('deleteState', contextMenu.machineName, contextMenu.stateName);
}

function onContextDeleteEdge() {
  edgeContextMenu.visible = false;
  handleDeleteEdge(edgeContextMenu.edgeId);
}

function onContextAddState() {
  paneContextMenu.visible = false;
  const machineNames = Object.keys(props.root.state_machines ?? {});
  const machineName = machineNames[0] || 'default';
  emit('addStateAtPosition', machineName, paneContextMenu.flowX, paneContextMenu.flowY);
}

function handleDeleteEdge(edgeId: string) {
  const edge = edges.value.find(e => e.id === edgeId);
  if (edge?.data) {
    emit('deleteTransition', edge.data.machineName, edge.data.fromState, edge.data.transitionIndex);
  }
}

// ── existing handlers ─────────────────────────────────────────────────────────

function onNodeClick(e: NodeMouseEvent) {
  contextMenu.visible = false;
  edgeContextMenu.visible = false;
  paneContextMenu.visible = false;
  const data = e.node.data;
  emit('select', {
    kind: 'state-node',
    nodeId: e.node.id,
    machineName: data.machineName,
    stateName: data.stateName,
    jsonPath: data.jsonPath,
  });
}

function onEdgeClick(e: EdgeMouseEvent) {
  contextMenu.visible = false;
  edgeContextMenu.visible = false;
  paneContextMenu.visible = false;
  const data = e.edge.data;
  emit('select', {
    kind: 'transition-edge',
    edgeId: e.edge.id,
    machineName: data.machineName,
    fromState: data.fromState,
    toState: data.toState,
    transitionIndex: data.transitionIndex,
    jsonPath: data.jsonPath,
  });
}

function onPaneClick() {
  contextMenu.visible = false;
  edgeContextMenu.visible = false;
  paneContextMenu.visible = false;
  emit('select', null);
}

function onNodeDragStop(e: NodeDragEvent) {
  const data = e.node.data;
  emit('moveNode', data.machineName, data.stateName, e.node.position.x, e.node.position.y);
}

onConnect((connection: Connection) => {
  if (!connection.source || !connection.target) return;

  const sourceNode = nodes.value.find(n => n.id === connection.source);
  if (!sourceNode) return;

  const targetNode = nodes.value.find(n => n.id === connection.target);
  if (!targetNode) return;

  const sourceData = sourceNode.data;
  const targetData = targetNode.data;

  if (sourceData.machineName !== targetData.machineName) return;

  emit('createTransition', sourceData.machineName, sourceData.stateName, targetData.stateName);
});

onEdgesChange((changes: EdgeChange[]) => {
  const deletions = changes.filter(c => c.type === 'remove');
  if (deletions.length > 0) {
    deletions.forEach(d => {
      const edge = edges.value.find(e => e.id === d.id);
      if (edge?.data) {
        emit('deleteTransition', edge.data.machineName, edge.data.fromState, edge.data.transitionIndex);
      }
    });
  }
  applyEdgeChanges(changes);
});
</script>

<style scoped>
.flow-wrap {
  width: 100%;
  height: 100%;
  position: relative;
}

.flow-canvas {
  width: 100%;
  height: 100%;
}

.empty-graph {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.state-node {
  min-width: 200px;
  background: rgba(30, 30, 35, 0.95);
  border: 1px solid rgba(100, 100, 120, 0.3);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
  color: #e8e8e8;
  overflow: hidden;
  transition: all 0.2s;
}

.state-node.is-start {
  border-color: rgba(76, 175, 80, 0.6);
  background: rgba(30, 35, 30, 0.95);
}

.state-node.is-selected {
  border-color: rgba(64, 158, 255, 0.8);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.25);
}

.node-header {
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  gap: 8px;
}

.state-node.is-start .node-header {
  background: rgba(76, 175, 80, 0.12);
  border-bottom-color: rgba(76, 175, 80, 0.2);
}

.node-title {
  font-weight: 600;
  font-size: 13px;
  color: #e8e8e8;
  flex: 1;
}

.start-badge {
  font-size: 9px;
  background: rgba(76, 175, 80, 0.9);
  color: #fff;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 600;
  text-transform: uppercase;
}

.node-body {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.handle-container {
  display: flex;
  align-items: center;
  position: relative;
  height: 22px;
  padding: 0 12px;
}

.handle-container.input {
  justify-content: flex-start;
}

.handle-container.output {
  justify-content: flex-end;
}

.handle-container.add-trans {
  opacity: 0.5;
  margin-top: 3px;
}

.handle-container.add-trans:hover {
  opacity: 0.9;
}

.pin-label {
  font-size: 11px;
  color: #b0b0b0;
  margin: 0 8px;
}

.transition-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-top: 6px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.ue-handle {
  width: 10px !important;
  height: 10px !important;
  background: rgba(100, 100, 120, 0.4) !important;
  border: 2px solid rgba(180, 180, 200, 0.6) !important;
  border-radius: 50%;
  position: absolute;
  top: 50% !important;
  transform: translateY(-50%);
}

.handle-container.input .ue-handle {
  left: -5px !important;
}

.handle-container.output .ue-handle {
  right: -5px !important;
}

.ue-handle:hover {
  background: rgba(64, 158, 255, 0.6) !important;
  border-color: rgba(64, 158, 255, 0.9) !important;
  transform: translateY(-50%) scale(1.2);
}

.context-menu {
  position: fixed;
  z-index: 9999;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  box-shadow: var(--el-box-shadow-light);
  min-width: 130px;
  overflow: hidden;
}

.context-menu-item {
  padding: 8px 16px;
  font-size: 13px;
  cursor: pointer;
  color: var(--el-text-color-primary);
  transition: background 0.1s;
}

.context-menu-item:hover {
  background: var(--el-fill-color-light);
}

.context-menu-item.danger {
  color: var(--el-color-danger);
}

.context-menu-item.danger:hover {
  background: var(--el-color-danger-light-9);
}

/* Highlighting for selected relationships */
:deep(.vue-flow__node.highlighted .state-node) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
}

/* Remove default node frame */
:deep(.vue-flow__node) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}


</style>
