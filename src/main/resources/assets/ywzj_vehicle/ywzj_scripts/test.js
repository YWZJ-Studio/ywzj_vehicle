let LARGE_SMOKE;
/**
 * 脚本初始化方法，在初始化脚本的作用域时调用。对于每个载具display，作用域各自独立<br/>
 * 一些必要的上下文会自动加载到脚本作用域中<br/>
 * 如boneHandlers {@link BoneHandlers} 和vehicleContext {@link WheeledVehicleContext}<br/>
 * 可以在此方法中注册需要操控的bone<br/>
*/
function initBones() {
    for (let i = 1; i <= 8; i++) {
        let wheelName = `wheel${i}`;
        boneHandlers.addSpecialBone(wheelName);
    }
    boneHandlers.addSpecialBone("turret");
    boneHandlers.addSpecialBone("canno");
    boneHandlers.addSpecialBone("machine_gun");
    boneHandlers.addSpecialBone("bone17");

    LARGE_SMOKE = ParticleUtil.buildParticleOptions("minecraft:large_smoke", "");
}

function prepareBones() {
    const vf = vehicleContext.getForwardSpeed();
    const t = (vehicleContext.currentTimeMillis() - vehicleContext.lastRenderTime()) / 1000 * 20;
    const s = t * vf;
    const l = 20 / 16.0;
    const r = s / (l * 3.1415) * 360.0;

    const cache = vehicleContext.loadCache() ?? {};

    const previousRotation = cache.wheelRotation ?? 0;
    const wheelRot = (previousRotation + r) % 360;
    cache.wheelRotation = wheelRot;

    vehicleContext.saveCache(cache);

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

function tickParticle() {
    if (vehicleContext.hasPassenger() && vehicleContext.getTickCount() % 5 === 0) {
        const v1 = vehicleContext.getLookAngle();
        const v2 = MathUtil.vec3(-v1.z, 0, v1.x).normalize();
        const engineSmokePos = vehicleContext.position()
            .add(vehicleContext.getLookAngle().normalize().scale(-2))
            .add(v2.scale(-1.6))
            .add(0, 2, 0);
        vehicleContext.addParticle(LARGE_SMOKE, engineSmokePos.x, engineSmokePos.y, engineSmokePos.z, 0, 0, 0);
    }
}