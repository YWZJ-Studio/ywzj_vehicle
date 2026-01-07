function prepareBones() {
    const deltaTime = (context.currentTimeMillis() - context.lastRenderTime()) / 1000;
    const forwardSpeed = context.getForwardSpeed();
    const turnSpeed = context.getTurnSpeed();
    const trackWidth = 3.0 / 20;
    const leftTrackSpeed = (forwardSpeed + turnSpeed * trackWidth / 2) * 20;
    const rightTrackSpeed = (forwardSpeed - turnSpeed * trackWidth / 2) * 20;

    context.advanceProgress("tread_l_move", "tread_r_move", leftTrackSpeed, rightTrackSpeed, deltaTime, 0.25)

    for (let i = 0; i < 19; i++) {
        const angle = i < 10 ? context.leftWheelDegrees(0.3125) : context.rightWheelDegrees(0.3125);
        context.rotateBone("wheel" + i, angle, 0, 0)
    }

    const yRot0 = context.getPartYRot("turret");
    const xRot0 = context.getPartXRot("turret");
    context.rotateBone("turret", 0, -yRot0, 0);
    context.rotateBone("cannon", xRot0, 0, 0);

    const xRot1 = context.getPartXRot("anti_aircraft_machine_gun");
    const yRot1 = context.getPartYRot("anti_aircraft_machine_gun");
    context.rotateBone("mg", 0, -yRot1, 0);
    context.rotateBone("mg_up", xRot1, 0, 0);
}
