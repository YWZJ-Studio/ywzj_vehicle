let LARGE_SMOKE;
/**
 * 脚本初始化方法，在初始化脚本的作用域时调用。对于每个载具display，作用域各自独立<br/>
 * 一些必要的上下文会自动加载到脚本作用域中<br/>
 * 如boneHandlers {@link BoneHandlers} 和context {@link Wheeledcontext}<br/>
 * 可以在此方法中注册需要操控的bone<br/>
*/
function initBones() {
    for (let i = 1; i <= 19; i++) {
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
    const vf = context.getForwardSpeed();
    const t = (context.currentTimeMillis() - context.lastRenderTime()) / 1000 * 20;
    const s = t * vf;
    const l = 20 / 16.0;
    const r = s / (l * 3.1415) * 360.0;

    const cache = context.loadCache() ?? {};

    const previousWheelRotation = cache.wheelRotation ?? 0;
    const wheelRotation = (previousWheelRotation + r) % 360;
    cache.wheelRotation = wheelRotation;

    context.saveCache(cache);

    const vt = context.getTurnAngle();
    const turnRotation = vt * 16;

    boneHandlers.getBone("wheel1")?.rotate(wheelRotation, -turnRotation, 0);
    boneHandlers.getBone("wheel2")?.rotate(wheelRotation, -turnRotation, 0);
    boneHandlers.getBone("wheel3")?.rotate(wheelRotation, -turnRotation * 0.5, 0);
    boneHandlers.getBone("wheel5")?.rotate(wheelRotation, -turnRotation * 0.5, 0);
    boneHandlers.getBone("wheel6")?.rotate(wheelRotation, turnRotation * 0.5, 0);
    boneHandlers.getBone("wheel4")?.rotate(wheelRotation, turnRotation * 0.5, 0);
    boneHandlers.getBone("wheel8")?.rotate(wheelRotation, turnRotation, 0);
    boneHandlers.getBone("wheel7")?.rotate(wheelRotation, turnRotation, 0);

    const xRot0 = context.getPartXRot("ztl11_turret");
    const yRot0 = context.getPartYRot("ztl11_turret");
    boneHandlers.getBone("turret")?.rotate(0, -yRot0, 0);
    boneHandlers.getBone("canno")?.rotate(xRot0, 0, 0);

    const xRot1 = context.getPartXRot("ztl11_commander_machine_gun");
    const yRot1 = context.getPartYRot("ztl11_commander_machine_gun");
    boneHandlers.getBone("machine_gun")?.rotate(0, -yRot1, 0);
    boneHandlers.getBone("bone17")?.rotate(xRot1, 0, 0);
}

function tickParticle() {
    if (context.hasPassenger() && context.getTickCount() % 5 === 0) {
        const v1 = context.getLookAngle();
        const v2 = MathUtil.vec3(-v1.z, 0, v1.x).normalize();
        const engineSmokePos = context.position()
            .add(context.getLookAngle().normalize().scale(-2))
            .add(v2.scale(-1.6))
            .add(0, 2, 0);
        context.addParticle(LARGE_SMOKE, engineSmokePos.x, engineSmokePos.y, engineSmokePos.z, 0, 0, 0);
    }
}