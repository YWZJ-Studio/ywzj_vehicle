function updateBones(context) {
    const builder = createPoseBuilder()
    if (context.hasOwner("turret")) {
        builder.setRotation("ctr_top_cap", 0, -150, 0)
    }
    return builder
}
