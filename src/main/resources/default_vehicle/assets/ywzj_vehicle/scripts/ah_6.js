function updateBones(context) {
    const previousPropellerRotation = context.getFloat("propellerRotation", 0)
    const propellerRotation = (previousPropellerRotation + context.getPower() / 15) % 360
    context.setFloat("propellerRotation", propellerRotation)
    const builder = createPoseBuilder()
    builder.setRotation("propeller", 0, -propellerRotation, 0)
    builder.setRotation("tailPropeller", -propellerRotation * 5, 0, 0)
    builder.setRotation("control", context.getPitchInput() * 10, 0, -context.getRollInput() * 10)
    return builder
}
