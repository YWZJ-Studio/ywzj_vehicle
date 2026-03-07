export type AnimationControllerMode = 'blend' | 'state';

export interface AnimationControllerRoot {
  name?: string;
  script?: string;
  parameters?: Record<string, any>;
  event_animations?: Record<string, any>;
  loop_animations?: Record<string, any>;
  state_machines?: Record<string, AnimationStateMachine>;
  graph?: BlendNode;
  detached?: BlendNode[];
  [key: string]: any;
}

export interface AnimationStateMachine {
  start_state?: string;
  states?: Record<string, AnimationState>;
  [key: string]: any;
}

export interface AnimationState {
  transitions?: AnimationTransition[];
  on_enter?: AnimationAction[];
  on_update?: AnimationAction[];
  on_exit?: AnimationAction[];
  evaluate?: Record<string, any>;
  editor?: {
    x?: number;
    y?: number;
    comment?: string;
    [key: string]: any;
  };
  [key: string]: any;
}

export interface AnimationTransition {
  target?: string;
  condition?: AnimationCondition;
  duration?: number;
  blend_curve?: string;
  after_trigger?: AnimationAction[];
  [key: string]: any;
}

export interface AnimationCondition {
  type?: 'script' | 'and' | 'or' | 'not' | string;
  [key: string]: any;
}

export interface AnimationAction {
  type?: 'play_animation' | 'stop_animation' | 'set_variable' | 'play_sound' | 'script' | string;
  [key: string]: any;
}

export interface BlendNode {
  type?: string;
  editor?: {
    x?: number;
    y?: number;
    comment?: string;
    [key: string]: any;
  };
  [key: string]: any;
}

export interface BoneBindingSpecialBinding {
  bones?: string[];
  source?: string;
  axis?: 'x' | 'y' | 'z' | string;
  param?: number;
}

export interface BoneBindingPartBinding {
  bone?: string;
  part?: string;
  rotation_type?: string;
  axis?: 'x' | 'y' | 'z' | string;
  invert?: boolean;
}

export interface BoneBindingBlendNode extends BlendNode {
  type: 'bone_binding';
  special_bindings?: BoneBindingSpecialBinding[];
  part_bindings?: BoneBindingPartBinding[];
}

export interface GraphNodeData {
  label: string;
  kind: 'blend' | 'state';
  jsonPath: Array<string | number>;
  rawType?: string;
  machineName?: string;
  stateName?: string;
}

export interface GraphEdgeData {
  kind: 'transition' | 'blend-link';
  jsonPath: Array<string | number>;
  machineName?: string;
  fromState?: string;
  toState?: string;
  transitionIndex?: number;
}
