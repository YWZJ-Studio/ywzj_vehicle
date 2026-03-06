import { parseJsonWithComments } from './jsonParser';
import type {Edge, Node} from '@vue-flow/core';
import type {
  AnimationControllerRoot,
  AnimationStateMachine,
  BlendNode,
  GraphEdgeData,
  GraphNodeData,
} from '@/types/animationController';

export function isAnimationControllerFile(path: string): boolean {
  const normalized = path.replace(/\\/g, '/').toLowerCase();
  return normalized.includes('/animation_controllers/') && normalized.endsWith('.json');
}

export function parseAnimationControllerContent(content: string): AnimationControllerRoot {
  return parseJsonWithComments(content) as AnimationControllerRoot;
}

export function buildBlendGraph(root: AnimationControllerRoot): { nodes: Node<GraphNodeData>[]; edges: Edge<GraphEdgeData>[] } {
  const nodes: Node<GraphNodeData>[] = [];
  const edges: Edge<GraphEdgeData>[] = [];
  if (!root.graph || typeof root.graph !== 'object') {
    return { nodes, edges };
  }

  let autoY = 0;
  let idCounter = 0;

  const walk = (
    node: BlendNode,
    path: Array<string | number>,
    depth: number,
    incoming?: { sourceId: string; label?: string }
  ): string => {
    const id = `blend_${idCounter++}`;
    const type = typeof node.type === 'string' ? node.type : 'unknown';

    const fallbackX = depth * 260;
    const fallbackY = autoY * 120;
    autoY += 1;

    nodes.push({
      id,
      position: {
        x: Number(node.editor?.x ?? fallbackX),
        y: Number(node.editor?.y ?? fallbackY),
      },
      data: {
        label: type,
        kind: 'blend',
        jsonPath: path,
        rawType: type,
      },
      type: 'default',
    });

    if (incoming) {
      edges.push({
        id: `blend_edge_${incoming.sourceId}_${id}_${incoming.label ?? 'link'}`,
        source: incoming.sourceId,
        target: id,
        label: incoming.label,
        data: {
          kind: 'blend-link',
          jsonPath: path,
        },
      });
    }

    const linkChild = (value: any, key: string) => {
      if (value && typeof value === 'object') {
        walk(value as BlendNode, [...path, key], depth + 1, { sourceId: id, label: key });
      }
    };

    const linkChildrenArray = (value: any[], key: string) => {
      value.forEach((item, idx) => {
        if (item && typeof item === 'object') {
          walk(item as BlendNode, [...path, key, idx], depth + 1, { sourceId: id, label: `${key}[${idx}]` });
        }
      });
    };

    linkChild((node as any).a, 'a');
    linkChild((node as any).b, 'b');
    linkChild((node as any).base, 'base');
    linkChild((node as any).add, 'add');

    if (Array.isArray((node as any).inputs)) {
      linkChildrenArray((node as any).inputs, 'inputs');
    }
    if (Array.isArray((node as any).layers)) {
      linkChildrenArray((node as any).layers, 'layers');
    }

    return id;
  };

  walk(root.graph, ['graph'], 0);
  return { nodes, edges };
}

export function buildStateGraph(root: AnimationControllerRoot): { nodes: Node<GraphNodeData>[]; edges: Edge<GraphEdgeData>[] } {
  const nodes: Node<GraphNodeData>[] = [];
  const edges: Edge<GraphEdgeData>[] = [];

  const machines = root.state_machines ?? {};
  let machineIndex = 0;

  for (const [machineName, machine] of Object.entries(machines) as Array<[string, AnimationStateMachine]>) {
    const states = machine.states ?? {};
    let stateIndex = 0;

    for (const [stateName, state] of Object.entries(states)) {
      const id = `state_${machineName}_${stateName}`;
      const fallbackX = machineIndex * 520 + (stateIndex % 3) * 180;
      const fallbackY = Math.floor(stateIndex / 3) * 140;

      nodes.push({
        id,
        position: {
          x: Number(state.editor?.x ?? fallbackX),
          y: Number(state.editor?.y ?? fallbackY),
        },
        data: {
          label: stateName,
          kind: 'state',
          jsonPath: ['state_machines', machineName, 'states', stateName],
          machineName,
          stateName,
        },
      });

      const transitions = state.transitions ?? [];
      transitions.forEach((transition, transitionIndex) => {
        if (!transition?.target) return;
        const targetId = `state_${machineName}_${transition.target}`;

        edges.push({
          id: `transition_${machineName}_${stateName}_${transition.target}_${transitionIndex}`,
          source: id,
          target: targetId,
          sourceHandle: `trans-${transitionIndex}`,
          label: transition.condition?.type ?? (transition.condition ? 'condition' : ''),
          data: {
            kind: 'transition',
            jsonPath: ['state_machines', machineName, 'states', stateName, 'transitions', transitionIndex],
            machineName,
            fromState: stateName,
            toState: transition.target,
            transitionIndex,
          },
        });
      });

      stateIndex += 1;
    }

    machineIndex += 1;
  }

  return { nodes, edges };
}

export function setValueAtPath(target: any, path: Array<string | number>, value: any) {
  if (!path.length) return;
  let current = target;
  for (let i = 0; i < path.length - 1; i += 1) {
    const key = path[i];
    if (current[key] == null || typeof current[key] !== 'object') {
      const next = path[i + 1];
      current[key] = typeof next === 'number' ? [] : {};
    }
    current = current[key];
  }
  current[path[path.length - 1]] = value;
}

export function getValueAtPath(target: any, path: Array<string | number>): any {
  let current = target;
  for (const key of path) {
    if (current == null) return undefined;
    current = current[key];
  }
  return current;
}

function readString(text: string, start: number) {
  const quote = text[start];
  let i = start + 1;
  let escaped = false;
  while (i < text.length) {
    const c = text[i];
    if (escaped) {
      escaped = false;
      i += 1;
      continue;
    }
    if (c === '\\') {
      escaped = true;
      i += 1;
      continue;
    }
    if (c === quote) {
      return { end: i + 1, value: text.slice(start + 1, i) };
    }
    i += 1;
  }
  return { end: text.length, value: '' };
}

function skipWhitespaceAndComments(text: string, start: number): number {
  let i = start;
  while (i < text.length) {
    const c = text[i];
    const n = text[i + 1];
    if (/\s/.test(c)) {
      i += 1;
      continue;
    }
    if (c === '/' && n === '/') {
      i += 2;
      while (i < text.length && text[i] !== '\n') i += 1;
      continue;
    }
    if (c === '/' && n === '*') {
      i += 2;
      while (i < text.length - 1 && !(text[i] === '*' && text[i + 1] === '/')) i += 1;
      i += 2;
      continue;
    }
    break;
  }
  return i;
}

function findTopLevelValueRange(text: string, key: string): { start: number; end: number; indent: string } | null {
  const rootStart = text.indexOf('{');
  if (rootStart < 0) return null;

  let i = rootStart + 1;
  let depth = 1;
  while (i < text.length) {
    i = skipWhitespaceAndComments(text, i);
    if (i >= text.length) break;

    const c = text[i];
    if (c === '}') {
      depth -= 1;
      if (depth === 0) break;
      i += 1;
      continue;
    }

    if ((c === '"' || c === "'") && depth === 1) {
      const keyInfo = readString(text, i);
      const propName = keyInfo.value;
      let cursor = skipWhitespaceAndComments(text, keyInfo.end);
      if (text[cursor] !== ':') {
        i = keyInfo.end + 1;
        continue;
      }

      cursor = skipWhitespaceAndComments(text, cursor + 1);
      const valueStart = cursor;

      let j = cursor;
      let valueDepth = 0;
      let inString = false;
      let stringQuote = '';
      let escaped = false;
      while (j < text.length) {
        const ch = text[j];
        const nx = text[j + 1];

        if (!inString && ch === '/' && nx === '/') {
          j += 2;
          while (j < text.length && text[j] !== '\n') j += 1;
          continue;
        }
        if (!inString && ch === '/' && nx === '*') {
          j += 2;
          while (j < text.length - 1 && !(text[j] === '*' && text[j + 1] === '/')) j += 1;
          j += 2;
          continue;
        }

        if (inString) {
          if (escaped) {
            escaped = false;
          } else if (ch === '\\') {
            escaped = true;
          } else if (ch === stringQuote) {
            inString = false;
            stringQuote = '';
          }
          j += 1;
          continue;
        }

        if (ch === '"' || ch === "'") {
          inString = true;
          stringQuote = ch;
          j += 1;
          continue;
        }

        if (ch === '{' || ch === '[') {
          valueDepth += 1;
          j += 1;
          continue;
        }
        if (ch === '}' || ch === ']') {
          if (valueDepth === 0 && ch === '}') {
            break;
          }
          valueDepth -= 1;
          j += 1;
          continue;
        }

        if (valueDepth === 0 && (ch === ',' || ch === '}')) {
          break;
        }

        j += 1;
      }

      if (propName === key) {
        const lineStart = text.lastIndexOf('\n', i) + 1;
        const linePrefix = text.slice(lineStart, i);
        const indent = linePrefix.match(/^\s*/)?.[0] ?? '  ';
        return { start: valueStart, end: j, indent };
      }

      i = j;
      if (text[i] === ',') i += 1;
      continue;
    }

    if (c === '{') depth += 1;
    if (c === '}') depth -= 1;
    i += 1;
  }

  return null;
}

function indentJson(value: any, indent: string): string {
  const raw = JSON.stringify(value, null, 2);
  return raw
    .split('\n')
    .map((line, idx) => (idx === 0 ? line : `${indent}${line}`))
    .join('\n');
}

function insertTopLevelProperty(text: string, key: string, value: any): string {
  const closeIndex = text.lastIndexOf('}');
  if (closeIndex < 0) {
    return JSON.stringify({ [key]: value }, null, 2);
  }

  const before = text.slice(0, closeIndex);
  const after = text.slice(closeIndex);
  const trimmedBefore = before.trimEnd();
  const needsComma = trimmedBefore.length > 1 && !trimmedBefore.endsWith('{');
  const suffix = before.slice(trimmedBefore.length);
  const serialized = indentJson(value, '  ');

  return `${trimmedBefore}${needsComma ? ',' : ''}\n  "${key}": ${serialized}${suffix}${after}`;
}

export function patchAnimationControllerSource(
  originalText: string,
  updates: Partial<Pick<AnimationControllerRoot, 'graph' | 'state_machines'>> & Record<string, any>
): string {
  let output = originalText;

  for (const [key, value] of Object.entries(updates)) {
    if (value === undefined) continue;
    const range = findTopLevelValueRange(output, key);
    if (!range) {
      output = insertTopLevelProperty(output, key, value);
      continue;
    }

    const replacement = indentJson(value, `${range.indent}  `);
    output = `${output.slice(0, range.start)}${replacement}${output.slice(range.end)}`;
  }

  return output;
}
