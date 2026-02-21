function updateBones(context) {
    const previousGunBarrelRotation = context.getFloat("gunBarrelRotation", 0);
    const deltaTime = (context.currentTimeMillis() - context.lastRenderTime()) / 1000 * 20;
    const deltaRotation = context.getPower() > 0 ? (deltaTime * 128) : 0;
    const gunBarrelRotation = (previousGunBarrelRotation + deltaRotation) % 360;
    context.setFloat("gunBarrelRotation", gunBarrelRotation)

    const builder = createPoseBuilder();
    builder.setRotation("mg_rotator", 0, 0, gunBarrelRotation);
    return builder;
}
