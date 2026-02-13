// 方向盘旋转
function rotControl(context) {
    const turnAngle = context.getTurnAngle();
    const turnRotation = turnAngle * 16;
    // Create pose helper
    const builder = createPoseBuilder();
    // 方向盘跟随转向角度旋转
    builder.setRotation("control", 0, -turnRotation * 12, 0);
    return builder;
}