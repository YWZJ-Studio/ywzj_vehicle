
function prepareBones() {
    const vf = vehicleContext.getForwardSpeed();
    const t = (vehicleContext.currentTimeMillis() - vehicleContext.lastRenderTime()) / 1000 * 20;
    const s = t * vf;
    const l = 20 / 16.0;
    const r = s / (l * 3.1415) * 360.0;
    const previousRotation = vehicleContext.getWheelRotation();
    const wheelRot = vehicleContext.setWheelRotation(previousRotation + r);

    const vt = vehicleContext.getTurnAngle();
    const turnRotation = vt * 16;

    boneHandlers.getBone("wheel1")?.rotate(wheelRot, -turnRotation, 0);
    boneHandlers.getBone("wheel2")?.rotate(wheelRot, -turnRotation, 0);
    boneHandlers.getBone("wheel3")?.rotate(wheelRot, -turnRotation * 0.5, 0);
    boneHandlers.getBone("wheel5")?.rotate(wheelRot, -turnRotation * 0.5, 0);
    boneHandlers.getBone("wheel6")?.rotate(wheelRot, turnRotation * 0.5, 0);
    boneHandlers.getBone("wheel4")?.rotate(wheelRot, turnRotation * 0.5, 0);
    boneHandlers.getBone("wheel8")?.rotate(wheelRot, turnRotation, 0);
    boneHandlers.getBone("wheel7")?.rotate(wheelRot, turnRotation, 0);


    const xRot0 = vehicleContext.getPartXRot("ztl11_turret");
    const yRot0 = vehicleContext.getPartYRot("ztl11_turret");
    boneHandlers.getBone("turret")?.rotate(0, -yRot0, 0);
    boneHandlers.getBone("canno")?.rotate(xRot0, 0, 0);

    const xRot1 = vehicleContext.getPartXRot("ztl11_commander_machine_gun");
    const yRot1 = vehicleContext.getPartYRot("ztl11_commander_machine_gun");
    boneHandlers.getBone("machine_gun")?.rotate(0, -yRot1, 0);
    boneHandlers.getBone("bone17")?.rotate(xRot1, 0, 0);
}

function initBones() {
    for (let i = 1; i <= 8; i++) {
        let wheelName = `wheel${i}`;
        boneHandlers.addSpecialBone(wheelName);
    }
    boneHandlers.addSpecialBone("turret");
    boneHandlers.addSpecialBone("canno");
    boneHandlers.addSpecialBone("machine_gun");
    boneHandlers.addSpecialBone("bone17");
}