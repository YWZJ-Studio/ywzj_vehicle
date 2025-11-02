# python
import argparse
import json
import re
from pathlib import Path


def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_json(obj, path):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def find_bone(bones, name):
    for b in bones:
        if b.get("name") == name:
            return b
    return None


def read_bones(model):
    return model["minecraft:geometry"][0]["bones"]


def extract_transform(bone):
    return {
        "pivot": bone.get("pivot"),
        "rotation": bone.get("rotation", [0, 0, 0])
    }


def build_animation(bones, duration=1.0):
    anim = {
        "format_version": "1.8.0",
        "animations": {
        }
    }
    bones_map = {b.get("name"): b for b in bones if "name" in b}
    pattern = re.compile(r"^tread_([a-z])_(\d+)$")

    for name, bone in bones_map.items():
        m = pattern.match(name)
        if not m:
            continue
        part = m.group(1)
        idx = int(m.group(2))
        next_name = f"tread_{part}_{idx + 1}"
        next_bone = bones_map.get(next_name)
        if not next_bone:
            # 如果不存在下一个骨骼则跳过
            continue
        t0 = extract_transform(bone)
        t1 = extract_transform(next_bone)
        # 生成两个关键帧：time 0 -> time duration
        # 使用常见的骨骼动画结构：每个属性为关键帧数组（可根据具体引擎调整）
        bone_anim = {}

        if t0["rotation"] is None or t1["rotation"] is None:
            continue
        if t0["pivot"] is None or t1["pivot"] is None:
            continue

        # 平移关键帧
        d_rot = [t1["rotation"][i] - t0["rotation"][i] for i in range(3)]
        d_pos = [t1["pivot"][i] - t0["pivot"][i] for i in range(3)]
        bone_anim["rotation"] = {
            "0.0": [0, 0, 0],
            f"{duration}": d_rot
        }
        bone_anim["position"] = {
            "0.0": [0, 0, 0],
            f"{duration}": d_pos
        }

        anim_name = f"tread_{part}_move"

        if anim_name not in anim["animations"]:
            anim["animations"][anim_name] = {
                "bones": {},
                "animation_length": duration
            }

        anim["animations"].get(anim_name).get("bones")[name] = bone_anim
    return anim


def main():
    parser = argparse.ArgumentParser(description="履带动画生成工具")
    parser.add_argument("model", nargs="?", help="输入模型 JSON 文件，例如 `ztz99a.json`")
    parser.add_argument("--out", "-o", help="输出动画文件名，默认：<modelname>.animation.json")
    parser.add_argument("--duration", "-d", type=float, default=1.0, help="关键帧间时长（秒），默认 1.0")
    args = parser.parse_args()

    if not args.model:
        parser.print_help()
        return

    model_path = Path(args.model)
    if not model_path.exists():
        print(f"模型文件未找到: {model_path}")
        return

    model = load_json(model_path)
    try:
        bones = read_bones(model)
    except ValueError as e:
        print(str(e))
        return

    animation = build_animation(bones, duration=args.duration)

    out_path = Path(args.out) if args.out else model_path.with_name(model_path.stem + ".animation.json")
    save_json(animation, out_path)
    print(f"已生成动画: {out_path}")


if __name__ == "__main__":
    main()
