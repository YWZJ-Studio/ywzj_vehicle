function prepareBones() {
    const cache = context.loadCache() ?? {};
    const previousPropellerRotation = cache.propellerRotation ?? 0;
    const rot = context.getPower() / 5
    cache.propellerRotation = (previousPropellerRotation + rot) % 360;
    context.saveCache(cache);

    context.rotateBone("z10w_top", 0, -cache.propellerRotation, 0);
    context.rotateBone("z10w_tail", -cache.propellerRotation * 5, 0, 0);

    context.rotateBone("z10w_camera", 0, -context.getPartYRot("sighting_system"), 0);
    context.rotateBone("z10w_camera", context.getPartXRot("sighting_system"), 0, 0);

    context.rotateBone("z10w_canno", 0, -context.getPartYRot("auto_cannon"), 0);
    context.rotateBone("z10w_canno", context.getPartXRot("auto_cannon"), 0, 0);
}
