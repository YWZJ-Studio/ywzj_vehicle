function updateBones(context) {
    const previousPropellerRotation = context.getFloat("propellerRotation", 0)
    const propellerRotation = (previousPropellerRotation + context.getPower() / 15) % 360
    context.setFloat("propellerRotation", propellerRotation)

    let d1 = 0
    let d2 = 0
    const control = context.getControlUnit()
    if (control.left || control.right) {
        d1 = control.left ? -10 : 10
    }
    if (control.forward || control.backward) {
        d2 = control.forward ? 10 : -10
    } else {
        d2 = Math.max(-10, Math.min(10, (control.xRot - context.getXRot()) / 30 * 10))
    }

    const builder = createPoseBuilder()
    builder.setRotation("propeller", 0, -propellerRotation, 0)
    builder.setRotation("tailPropeller", -propellerRotation * 5, 0, 0)
    builder.setRotation("control", -d2, 0, -d1)
    return builder
}
