function prepareBones() {
    const cache = context.loadCache() ?? {};
    const previousPropellerRotation = cache.propellerRotation ?? 0;
    const rot = context.getPower() / 5
    cache.propellerRotation = (previousPropellerRotation + rot) % 360;
    context.saveCache(cache);

    // 双旋翼
    context.rotateBone("1", 0, cache.propellerRotation, 0);
    context.rotateBone("2", 0, -cache.propellerRotation, 0);

    // 机炮
    context.rotateBone("auto_cannon", 0, -context.getPartYRot("auto_cannon"), 0);
    context.rotateBone("auto_cannon", -context.getPartXRot("auto_cannon"), 0, 0);

    // 操作杆
    let d1 = 0;
    let d2 = 0;
    const control = context.getControlUnit();
    if (control.left || control.right) {
        d1 = control.left ? 10 : -10;
    }
    if (control.forward || control.backward) {
        d2 = control.forward ? -10 : 10;
    } else {
        d2 = Math.max(-10, Math.min(10, -(control.xRot - context.getXRot()) / 30 * 10));
    }
    context.rotateBone("czg", d2, 0, d1);
    context.rotateBone("zjg", context.getCollectivePitch() / 100 * 20, 0, 0);
}
