export interface PackMeta {
  namespace: string;
  title?: string;
  description?: string;
  version?: string;
  date?: string;
  license?: string;
  authors?: string[];
  url?: string;
  dependencies?: Record<string, string>;
}

export interface VehicleAttributes {
  brake_force?: number;
  forward_force?: number;
  backward_force?: number;
  max_speed_forward?: number;
  max_speed_backward?: number;
  turn_step?: number;
  max_turn?: number;
}

export interface ViewInfo {
  third_person_center_offset?: [number, number, number];
  third_person_distance?: number;
  sound_distance?: number;
  lock_passenger_y_body_rot?: boolean;
}

export interface EnergyInfo {
  energy_capacity?: number;
  energy_consumption_per_tick?: number;
}

export interface PhysicsInfo {
  gravity?: number;
  drag?: number;
}

export interface DefenseStats {
  armor?: number;
  armor_toughness?: number;
}

export interface VehiclePart {
  id: string;
  name: string;
  type: string;
  is_seat?: boolean;
  seat_offset?: [number, number, number];
  owner_view_offset?: [number, number, number];
  sub_part_unit_ids?: string[];
  [key: string]: any;
}

export interface VehicleConfig {
  type: string;
  attributes?: VehicleAttributes;
  max_health?: number;
  view_info?: ViewInfo;
  energy_info?: EnergyInfo;
  physics_info?: PhysicsInfo;
  defense_stats?: DefenseStats;
  with_warning_receiver?: boolean;
  protect_passenger?: boolean;
  structure_model?: string;
  parts?: VehiclePart[];
  [key: string]: any;
}

export interface VehicleDisplay {
  type: string;
  model: string;
  texture: string;
  slot_texture?: string;
  animations?: string;
  script?: string;
  sounds?: Record<string, string>;
  description?: string;
}

export type ConfigType =
  | 'pack_meta'
  | 'vehicle_data'
  | 'vehicle_display'
  | 'part_config'
  | 'unknown';

