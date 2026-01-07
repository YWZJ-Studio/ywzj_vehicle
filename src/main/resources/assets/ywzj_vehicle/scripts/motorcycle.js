function prepareBones() {
    const forwardSpeed = context.getForwardSpeed();
    const deltaTime = (context.currentTimeMillis() - context.lastRenderTime()) / 1000 * 20;
    const distance = deltaTime * forwardSpeed;
    const wheelDiameter = 20 / 16.0;
    const rot = distance / (wheelDiameter * 3.1415) * 360.0;

    const cache = context.loadCache() ?? {};
    const previousWheelRotation = cache.wheelRotation ?? 0;
    const wheelRotation = (previousWheelRotation + rot) % 360;
    cache.wheelRotation = wheelRotation;
    context.saveCache(cache);

    const turnAngle = context.getTurnAngle();
    const turnRotation = turnAngle * 16;

    context.rotateBone("wheel_front", wheelRotation, 0, 0);
    context.rotateBone("wheel_back", wheelRotation, 0, 0);
    context.rotateBone("front",  0, -turnRotation, 0);
}
