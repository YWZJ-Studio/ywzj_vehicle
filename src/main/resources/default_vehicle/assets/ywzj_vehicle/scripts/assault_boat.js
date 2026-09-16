function updateBones(context) {
    const control = context.getControlUnit()
    const direction = control.forward ? 1 : (control.backward ? -1 : 0)
    const deltaTime = Math.max(0, context.currentTimeMillis() - context.lastRenderTime()) / 1000
    const previousRotation = context.getFloat("propellerRotation", 0)
    const propellerRotation = (previousRotation + direction * deltaTime * 2000) % 360
    context.setFloat("propellerRotation", propellerRotation)

    const builder = createPoseBuilder()
    builder.setRotation("root3", 0, context.getTurnAngle() * 8, 0)
    builder.setRotation("root4", 0, 0, propellerRotation)
    return builder
}
