function prepareBones() {
    const forwardSpeed = context.getForwardSpeed();
    const deltaTime = (context.currentTimeMillis() - context.lastRenderTime()) / 1000 * 20;
    const distance = deltaTime * forwardSpeed;
    const wheelDiameter = 20 / 16.0;
    const rot = distance / (wheelDiameter * 3.1415) * 360.0;

    const cache = context.loadCache() ?? {};
    const previousWheelRotation = cache.wheelRotation ?? 0;
    const wheelRotation = (previousWheelRotation + rot) % 360;
    const previousGunBarrelRotation = cache.gunBarrelRotation ?? 0;
    let gunBarrelRotation = previousGunBarrelRotation;
    if (context.hasPower()) {
        gunBarrelRotation = (previousGunBarrelRotation + deltaTime * 200) % 360;
    }
    cache.wheelRotation = wheelRotation;
    cache.gunBarrelRotation = gunBarrelRotation;
    context.saveCache(cache);

    const turnAngle = context.getTurnAngle();
    const turnRotation = turnAngle * 16;

    context.rotateBone("wheel1", wheelRotation, -turnRotation, 0);
    context.rotateBone("wheel2", wheelRotation, -turnRotation, 0);

    context.rotateBone("wheel4", wheelRotation, -turnRotation * 0.5, 0);
    context.rotateBone("wheel3", wheelRotation, -turnRotation * 0.5, 0);

    context.rotateBone("wheel6", wheelRotation, turnRotation * 0.5, 0);
    context.rotateBone("wheel5", wheelRotation, turnRotation * 0.5, 0);

    context.rotateBone("wheel8", wheelRotation, turnRotation, 0);
    context.rotateBone("wheel7", wheelRotation, turnRotation, 0);

    context.rotateBone("mg_rotator", 0, 0, gunBarrelRotation);

    context.rotateBone("turret_main", 0, -context.getPartYRot("turret"), 0);
    context.rotateBone("mg", context.getPartXRot("turret"), 0, 0);
    context.rotateBone("missile", context.getPartXRot("turret_missile"), 0, 0);
}
