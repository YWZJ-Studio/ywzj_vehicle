<template>
  <div class="flow-wrap">
    <VueFlow
      v-if="nodes.length"
      :nodes="computedNodes"
      :edges="computedEdges"
      :default-edge-options="{ type: 'default', animated: false }"
      fit-view-on-init
      class="flow-canvas"
      @node-click="onNodeClick"
      @edge-click="onEdgeClick"
      @pane-click="onPaneClick"
      @node-drag-stop="onNodeDragStop"
      @node-context-menu="onNodeContextMenu"
      @edge-context-menu="onEdgeContextMenu"
      @pane-context-menu="onPaneContextMenu"
      @connect="onConnect"
    >
      <Background pattern-color="#aaa" :gap="16" />
      <Controls />

      <template #node-root="{ id }">
        <div class="blend-node root-node" :class="{ 'is-selected': selectedId === id }">
          <div class="node-header">
            <span class="node-title">OUTPUT</span>
          </div>
          <div class="node-body">
            <div class="handle-container input">
              <Handle type="target" :position="Position.Left" class="ue-handle" />
              <span class="pin-label">Result</span>
            </div>
          </div>
        </div>
      </template>

      <template #node-default="{ data, id }">
        <div
          class="blend-node"
          :class="[`type-${data.rawType}`, { 'is-selected': selectedId === id }]"
        >
          <div class="node-header">
            <span class="node-title">{{ data.rawType }}</span>
          </div>
          <div class="node-body">
            <div v-if="hasInputs(data.rawType)" class="input-list">
              <div
                v-for="input in getInputs(data.rawType)"
                :key="input"
                class="handle-container input"
              >
                <Handle
                  type="target"
                  :position="Position.Left"
                  :id="input"
                  class="ue-handle"
                />
                <span class="pin-label">{{ input }}</span>
              </div>
            </div>
            <div class="handle-container output">
              <span class="pin-label">Out</span>
              <Handle type="source" :position="Position.Right" class="ue-handle" />
            </div>
            <div v-if="data.ref" class="node-ref">{{ data.ref }}</div>
          </div>
        </div>
      </template>
    </VueFlow>

    <div v-else class="empty-graph">
      <el-empty description="该控制器没有混合图定义" />
    </div>

    <!-- 节点右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @mouseleave="contextMenu.visible = false"
    >
      <div class="context-menu-item danger" @click="onContextDeleteNode">删除节点</div>
    </div>

    <!-- 连线右键菜单 -->
    <div
      v-if="edgeContextMenu.visible"
      class="context-menu"
      :style="{ left: edgeContextMenu.x + 'px', top: edgeContextMenu.y + 'px' }"
      @mouseleave="edgeContextMenu.visible = false"
    >
      <div class="context-menu-item danger" @click="onContextDeleteEdge">删除连线</div>
    </div>

    <!-- 画布右键菜单 -->
    <div
      v-if="paneContextMenu.visible"
      class="context-menu"
      :style="{ left: paneContextMenu.x + 'px', top: paneContextMenu.y + 'px' }"
      @mouseleave="paneContextMenu.visible = false"
    >
      <div class="context-menu-item" @click="onContextAddNode">添加节点</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, reactive } from 'vue';
import { VueFlow, Handle, Position, useVueFlow } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import type { NodeMouseEvent, NodeDragEvent, EdgeMouseEvent, Connection } from '@vue-flow/core';
import { getValueAtPath } from '@/utils/animationControllerGraph';
import type { AnimationControllerRoot } from '@/types/animationController';
import type { AnimationSelection } from '@/stores/animationControllerEditor';

const props = defineProps<{
  root: AnimationControllerRoot;
  selectedId: string | null;
}>();

const emit = defineEmits<{
  select: [sel: AnimationSelection | null];
  moveNode: [jsonPath: Array<string | number>, x: number, y: number];
  moveOutputNode: [x: number, y: number];
  deleteNode: [jsonPath: Array<string | number>];
  deleteEdge: [jsonPath: Array<string | number>];
  connectEdge: [sourceJsonPath: Array<string | number>, targetJsonPath: Array<string | number>, targetHandle: string];
  addNodeAtPosition: [x: number, y: number];
}>();

const { project } = useVueFlow();

const graphRef = ref(buildGraph(props.root));
watch(() => props.root, () => { graphRef.value = buildGraph(props.root); }, { deep: true });

const nodes = computed(() => graphRef.value.nodes);
const edges = computed(() => graphRef.value.edges);

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
  return edges.value.map(e => ({
    ...e,
    style: e.id === sel || e.source === sel || e.target === sel
      ? { stroke: '#ffd700', strokeWidth: 3 }
      : { stroke: 'rgba(150, 150, 170, 0.4)', strokeWidth: 1.5 },
    animated: e.id === sel || e.source === sel || e.target === sel,
  }));
});

function buildGraph(root: AnimationControllerRoot) {
  const nodes: any[] = [];
  const edges: any[] = [];

  let idCounter = 0;
  let maxY = 0;
  let maxX = 0;

  const walk = (node: any, path: Array<string | number>, depth: number): string => {
    const id = `blend_${idCounter++}`;
    const type = typeof node.type === 'string' ? node.type : 'unknown';

    const fallbackX = depth * 280;
    const fallbackY = idCounter * 140;
    const posX = Number(node.editor?.x ?? fallbackX);
    maxY = Math.max(maxY, fallbackY);
    maxX = Math.max(maxX, posX);

    nodes.push({
      id,
      position: {
        x: posX,
        y: Number(node.editor?.y ?? fallbackY),
      },
      data: {
        label: type,
        kind: 'blend',
        jsonPath: path,
        rawType: type,
        ref: node.ref,
      },
      type: 'default',
    });

    const linkChild = (value: any, key: string) => {
      if (value && typeof value === 'object') {
        const childId = walk(value, [...path, key], depth + 1);
        edges.push({
          id: `edge_${childId}_${id}_${key}`,
          source: childId,
          target: id,
          targetHandle: key,
          label: key,
        });
      }
    };

    const linkChildrenArray = (value: any[], key: string) => {
      value.forEach((item, idx) => {
        if (item && typeof item === 'object') {
          const childId = walk(item, [...path, key, idx], depth + 1);
          edges.push({
            id: `edge_${childId}_${id}_${key}_${idx}`,
            source: childId,
            target: id,
            targetHandle: key,
            label: `${key}[${idx}]`,
          });
        }
      });
    };

    linkChild(node.a, 'a');
    linkChild(node.b, 'b');
    linkChild(node.base, 'base');
    linkChild(node.add, 'add');

    if (Array.isArray(node.inputs)) linkChildrenArray(node.inputs, 'inputs');
    if (Array.isArray(node.layers)) linkChildrenArray(node.layers, 'layers');

    return id;
  };

  // Process graph if it exists and has a type
  if (root.graph && typeof root.graph === 'object' && root.graph.type) {
    const rootId = walk(root.graph, ['graph'], 0);
    edges.push({
      id: `edge_${rootId}_root`,
      source: rootId,
      target: 'root',
    });
  }

  // Add detached nodes
  if (Array.isArray(root.detached)) {
    root.detached.forEach((detachedNode, idx) => {
      walk(detachedNode, ['detached', idx], 0);
    });
  }

  // Always add virtual OUTPUT node
  const outputX = Number(root.editor?.output_x ?? maxX + 300);
  const outputY = Number(root.editor?.output_y ?? maxY / 2);
  nodes.push({
    id: 'root',
    type: 'root',
    position: { x: outputX, y: outputY },
    data: { kind: 'root' },
  });

  return { nodes, edges };
}

function hasInputs(type: string): boolean {
  return ['blend', 'additive', 'layered_blend', 'merge'].includes(type);
}

function getInputs(type: string): string[] {
  if (type === 'blend') return ['a', 'b'];
  if (type === 'additive') return ['base', 'add'];
  if (type === 'layered_blend') return ['base', 'layers'];
  if (type === 'merge') return ['inputs'];
  return [];
}

function onNodeClick(e: NodeMouseEvent) {
  if (e.node.data.kind === 'root') {
    emit('select', null);
    return;
  }
  const data = e.node.data;
  const nodeData = getValueAtPath(props.root, data.jsonPath);
  emit('select', {
    kind: 'blend-node',
    nodeId: e.node.id,
    jsonPath: data.jsonPath,
    type: data.rawType,
    label: nodeData?.ref ?? data.rawType,
  });
}

function onPaneClick() {
  contextMenu.visible = false;
  edgeContextMenu.visible = false;
  paneContextMenu.visible = false;
  emit('select', null);
}

function onNodeDragStop(e: NodeDragEvent) {
  if (e.node.data.kind === 'root') {
    emit('moveOutputNode', e.node.position.x, e.node.position.y);
    return;
  }
  emit('moveNode', e.node.data.jsonPath, e.node.position.x, e.node.position.y);
}

const contextMenu = reactive({ visible: false, x: 0, y: 0, jsonPath: [] as Array<string | number> });
const edgeContextMenu = reactive({ visible: false, x: 0, y: 0, jsonPath: [] as Array<string | number> });
const paneContextMenu = reactive({ visible: false, x: 0, y: 0, flowX: 0, flowY: 0 });

function onNodeContextMenu(e: NodeMouseEvent) {
  if (e.node.data.kind === 'root') return;
  e.event.preventDefault();
  edgeContextMenu.visible = false;
  paneContextMenu.visible = false;
  contextMenu.jsonPath = e.node.data.jsonPath;
  contextMenu.x = (e.event as MouseEvent).clientX;
  contextMenu.y = (e.event as MouseEvent).clientY;
  contextMenu.visible = true;
}

function onEdgeClick(e: EdgeMouseEvent) {
  contextMenu.visible = false;
  edgeContextMenu.visible = false;
  const sourceNode = nodes.value.find(n => n.id === e.edge.source);
  if (!sourceNode) return;
  emit('select', {
    kind: 'blend-edge',
    edgeId: e.edge.id,
    jsonPath: sourceNode.data.jsonPath,
  });
}

function onEdgeContextMenu(e: EdgeMouseEvent) {
  e.event.preventDefault();
  contextMenu.visible = false;
  paneContextMenu.visible = false;
  const sourceNode = nodes.value.find(n => n.id === e.edge.source);
  if (!sourceNode) return;
  edgeContextMenu.jsonPath = sourceNode.data.jsonPath;
  edgeContextMenu.x = (e.event as MouseEvent).clientX;
  edgeContextMenu.y = (e.event as MouseEvent).clientY;
  edgeContextMenu.visible = true;
}

function onContextDeleteNode() {
  contextMenu.visible = false;
  emit('deleteNode', contextMenu.jsonPath);
}

function onContextDeleteEdge() {
  edgeContextMenu.visible = false;
  emit('deleteEdge', edgeContextMenu.jsonPath);
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

function onContextAddNode() {
  paneContextMenu.visible = false;
  emit('addNodeAtPosition', paneContextMenu.flowX, paneContextMenu.flowY);
}

function onConnect(connection: Connection) {
  const sourceNode = nodes.value.find(n => n.id === connection.source);
  const targetNode = nodes.value.find(n => n.id === connection.target);
  if (!sourceNode || !targetNode) return;

  // Allow connection to root node - use special marker
  if (targetNode.data.kind === 'root') {
    emit('connectEdge', sourceNode.data.jsonPath, ['__ROOT__'], 'graph');
    return;
  }

  if (!connection.targetHandle) return;
  emit('connectEdge', sourceNode.data.jsonPath, targetNode.data.jsonPath, connection.targetHandle);
}
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

.blend-node {
  min-width: 180px;
  background: rgba(30, 30, 35, 0.95);
  border: 1px solid rgba(100, 100, 120, 0.3);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
  color: #e8e8e8;
  overflow: hidden;
  transition: all 0.2s;
}

.blend-node.is-selected {
  border-color: rgba(64, 158, 255, 0.8);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.25);
}

.blend-node.root-node {
  border-color: rgba(255, 193, 7, 0.6);
  background: rgba(40, 35, 25, 0.95);
}

.blend-node.type-blend,
.blend-node.type-additive {
  border-color: rgba(76, 175, 80, 0.5);
}

.blend-node.type-layered_blend,
.blend-node.type-merge {
  border-color: rgba(103, 58, 183, 0.5);
}

.blend-node.type-state_machine {
  border-color: rgba(33, 150, 243, 0.5);
}

.blend-node.type-bone_binding {
  border-color: rgba(255, 152, 0, 0.5);
}

.node-header {
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.node-title {
  font-weight: 600;
  font-size: 12px;
  color: #e8e8e8;
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

.pin-label {
  font-size: 11px;
  color: #b0b0b0;
  margin: 0 8px;
}

.input-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.node-ref {
  font-size: 10px;
  color: #888;
  margin-top: 4px;
  padding: 0 12px;
  font-family: 'Consolas', monospace;
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

:deep(.vue-flow__node.highlighted .blend-node) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
}

:deep(.vue-flow__node) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
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
</style>
