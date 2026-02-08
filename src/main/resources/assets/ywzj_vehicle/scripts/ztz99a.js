function prepareBones(context) {
    context.advanceTrackProgress("tread_l_move", "tread_r_move", 0.25, 3)

    // Create pose helper
    const builder = createPoseBuilder();

    for (let i = 0; i < 13; i++) {
        const angleBig = i < 10 ? context.getLeftWheelDegrees(0.375) : context.getRightWheelDegrees(0.375);
        builder.setRotation("hull_big_" + i, angleBig, 0, 0);
    }

    for (let i = 0; i < 5; i++) {
        const angleSmall = i < 10 ? context.getLeftWheelDegrees(0.28) : context.getRightWheelDegrees(0.28);
        builder.setRotation("hull_small_" + i, angleSmall, 0, 0);
    }

    const yRot0 = context.getPartYRot("turret");
    const xRot0 = context.getPartXRot("turret");
    builder.setRotation("turret", 0, -yRot0, 0);
    builder.setRotation("canno", xRot0, 0, 0);

    const xRot1 = context.getPartXRot("commander_machine_gun");
    const yRot1 = context.getPartYRot("commander_machine_gun");
    builder.setRotation("machine_gun", 0, -yRot1, 0);
    builder.setRotation("machine_gun_high", xRot1, 0, 0);
    
    // Build and return the pose
    return builder;
}

function createTrackPose(context) {
    return context.getTrackPose();
}
