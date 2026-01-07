function prepareBones() {
    const cache = context.loadCache() ?? {};
    const previousPropellerRotation = cache.propellerRotation ?? 0;
    const rot = context.getPower() / 5
    cache.propellerRotation = (previousPropellerRotation + rot) % 360;
    context.saveCache(cache);

    context.rotateBone("propeller", 0, -cache.propellerRotation, 0);
    context.rotateBone("propeller_tail", -cache.propellerRotation * 5, 0, 0);

    context.rotateBone("mg", 0, -context.getPartYRot("auto_cannon"), 0);
    context.rotateBone("mg_up", context.getPartXRot("auto_cannon"), 0, 0);
}
