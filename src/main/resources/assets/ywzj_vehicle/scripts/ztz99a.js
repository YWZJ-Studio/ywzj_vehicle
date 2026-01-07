function prepareBones() {
    const deltaTime = (context.currentTimeMillis() - context.lastRenderTime()) / 1000;
    const forwardSpeed = context.getForwardSpeed();
    const turnSpeed = context.getTurnSpeed();
    const trackWidth = 3.0 / 20;
    const leftTrackSpeed = (forwardSpeed + turnSpeed * trackWidth / 2) * 20;
    const rightTrackSpeed = (forwardSpeed - turnSpeed * trackWidth / 2) * 20;

    context.advanceProgress("tread_l_move", "tread_r_move", leftTrackSpeed, rightTrackSpeed, deltaTime, 0.25)

    for (let i = 0; i < 13; i++) {
        const angleBig = i < 10 ? context.leftWheelDegrees(0.375) : context.rightWheelDegrees(0.375);
        context.rotateBone("hull_big_" + i, angleBig, 0, 0)
    }
    for (let i = 0; i < 5; i++) {
        const angleSmall = i < 10 ? context.leftWheelDegrees(0.28) : context.rightWheelDegrees(0.28);
        context.rotateBone("hull_small_" + i, angleSmall, 0, 0)
    }

    const yRot0 = context.getPartYRot("turret");
    const xRot0 = context.getPartXRot("turret");
    context.rotateBone("turret", 0, -yRot0, 0);
    context.rotateBone("canno", xRot0, 0, 0);

    const xRot1 = context.getPartXRot("commander_machine_gun");
    const yRot1 = context.getPartYRot("commander_machine_gun");
    context.rotateBone("machine_gun", 0, -yRot1, 0);
    context.rotateBone("machine_gun_high", xRot1, 0, 0);
}
