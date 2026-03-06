<template>
  <BaseEdge :id="id" :style="style" :marker-end="markerEnd" :path="edgePath[0]" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { BaseEdge, getBezierPath, type EdgeProps } from '@vue-flow/core';

const props = defineProps<EdgeProps>();

const edgePath = computed(() => {
  return getBezierPath({
    sourceX: props.sourceX,
    sourceY: props.sourceY,
    sourcePosition: props.sourcePosition,
    targetX: props.targetX,
    targetY: props.targetY,
    targetPosition: props.targetPosition,
  });
});
</script>
