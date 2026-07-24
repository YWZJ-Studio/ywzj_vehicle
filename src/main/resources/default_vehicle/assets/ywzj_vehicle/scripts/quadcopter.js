function updateBones(context) {
    const previousPropellerRotation = context.getFloat("propellerRotation", 0)
    const propellerRotation = (previousPropellerRotation + context.getPower() / 5) % 360
    context.setFloat("propellerRotation", propellerRotation)

    const builder = createPoseBuilder()
    builder.setRotation("wing1_up", 0, -propellerRotation, 0)
    builder.setRotation("wing4_up", 0, -propellerRotation, 0)
    builder.setRotation("wing2_down", 0, -propellerRotation, 0)
    builder.setRotation("wing3_down", 0, -propellerRotation, 0)
    builder.setRotation("wing1_down", 0, propellerRotation, 0)
    builder.setRotation("wing4_down", 0, propellerRotation, 0)
    builder.setRotation("wing2_up", 0, propellerRotation, 0)
    builder.setRotation("wing3_up", 0, propellerRotation, 0)
    if (context.getCargoRopeLength() > 0) {
        builder.hideBone("rope")
        builder.hideBone("rope_connect")
    }
    return builder
}
