<template>
  <div class="flow-wrap">
    <VueFlow
      v-if="nodes.length"
      :nodes="nodes"
      :edges="edges"
      :default-edge-options="{ type: 'smoothstep' }"
      fit-view-on-init
      class="flow-canvas"
      @node-click="onNodeClick"
      @pane-click="onPaneClick"
      @node-drag-stop="onNodeDragStop"
    >
      <Background />
      <Controls />

      <template #node-default="{ data, id }">
        <div
          class="blend-node"
          :class="[`type-${data.rawType}`, { 'is-selected': selectedId === id }]"
        >
          <div class="node-type">{{ data.rawType }}</div>
          <div v-if="data.ref" class="node-ref">{{ data.ref }}</div>
        </div>
      </template>
    </VueFlow>

    <div v-else class="empty-graph">
      <el-empty description="该控制器没有混合图定义" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { VueFlow } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import type { NodeMouseEvent, NodeDragEvent } from '@vue-flow/core';
import { buildBlendGraph, getValueAtPath } from '@/utils/animationControllerGraph';
import type { AnimationControllerRoot } from '@/types/animationController';
import type { AnimationSelection } from '@/stores/animationControllerEditor';

const props = defineProps<{
  root: AnimationControllerRoot;
  selectedId: string | null;
}>();

const emit = defineEmits<{
  select: [sel: AnimationSelection | null];
  moveNode: [jsonPath: Array<string | number>, x: number, y: number];
}>();

const graphRef = ref(buildBlendGraph(props.root));
watch(() => props.root, () => { graphRef.value = buildBlendGraph(props.root); }, { deep: true });

const nodes = computed(() => graphRef.value.nodes);
const edges = computed(() => graphRef.value.edges);

function onNodeClick(e: NodeMouseEvent) {
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
  emit('select', null);
}

function onNodeDragStop(e: NodeDragEvent) {
  emit('moveNode', e.node.data.jsonPath, e.node.position.x, e.node.position.y);
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
  padding: 8px 14px;
  border-radius: 8px;
  border: 2px solid var(--el-border-color);
  background: var(--el-bg-color);
  min-width: 90px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.blend-node.is-selected {
  border-color: var(--el-color-warning);
  box-shadow: 0 0 0 3px var(--el-color-warning-light-5);
}

.blend-node.type-blend,
.blend-node.type-additive,
.blend-node.type-layered_blend,
.blend-node.type-merge {
  border-color: var(--el-color-success-light-3);
  background: var(--el-color-success-light-9);
}

.blend-node.type-state_machine {
  border-color: var(--el-color-primary-light-3);
  background: var(--el-color-primary-light-9);
}

.blend-node.type-bone_binding {
  border-color: var(--el-color-warning-light-3);
  background: var(--el-color-warning-light-9);
}

.node-type {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.node-ref {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
  font-family: 'Consolas', monospace;
}
</style>
