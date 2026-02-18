function prepareBones(context) {
    const cache = context.loadCache() ?? {};
    const previousPropellerRotation = cache.propellerRotation ?? 0;
    const rot = context.getPower() / 5
    cache.propellerRotation = (previousPropellerRotation + rot) % 360;
    context.saveCache(cache);

    const builder = createPoseBuilder();

    builder.setRotation("propeller", 0, -cache.propellerRotation, 0);
    builder.setRotation("propeller_tail", -cache.propellerRotation * 5, 0, 0);

    builder.setRotation("mg", 0, -context.getPartYRot("auto_cannon"), 0);
    builder.setRotation("mg_up", context.getPartXRot("auto_cannon"), 0, 0);

    return builder;
}
