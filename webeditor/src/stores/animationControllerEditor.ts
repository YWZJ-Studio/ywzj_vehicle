import {defineStore} from 'pinia';

export type AnimationEditorMode = 'blend' | 'state';

export type AnimationSelection =
  | {
      kind: 'blend-node';
      nodeId: string;
      jsonPath: Array<string | number>;
      type?: string;
      label?: string;
    }
  | {
      kind: 'blend-edge';
      edgeId: string;
      jsonPath: Array<string | number>;
    }
  | {
      kind: 'state-node';
      nodeId: string;
      machineName: string;
      stateName: string;
      jsonPath: Array<string | number>;
    }
  | {
      kind: 'transition-edge';
      edgeId: string;
      machineName: string;
      fromState: string;
      toState: string;
      transitionIndex: number;
      jsonPath: Array<string | number>;
    };

export interface AnimationEditorContext {
  mode: AnimationEditorMode;
  selection: AnimationSelection | null;
}

export const useAnimationControllerEditorStore = defineStore('animationControllerEditor', {
  state: () => ({
    contexts: {} as Record<string, AnimationEditorContext>,
  }),

  actions: {
    ensure(path: string) {
      if (!this.contexts[path]) {
        this.contexts[path] = {
          mode: 'blend',
          selection: null,
        };
      }
      return this.contexts[path];
    },

    setMode(path: string, mode: AnimationEditorMode) {
      const ctx = this.ensure(path);
      ctx.mode = mode;
      ctx.selection = null;
    },

    setSelection(path: string, selection: AnimationSelection | null) {
      const ctx = this.ensure(path);
      ctx.selection = selection;
    },

    getContext(path: string): AnimationEditorContext {
      return this.ensure(path);
    },

    clear(path: string) {
      delete this.contexts[path];
    },
  },
});
