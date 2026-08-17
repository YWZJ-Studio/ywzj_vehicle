const missiles = ["missile_h1_1", "missile_h1_5", "missile_h1_2", "missile_h1_6", "missile_h1_3", "missile_h1_7", "missile_h1_4", "missile_h1_8"]

function updateBones(context) {
    const previousPropellerRotation = context.getFloat("propellerRotation", 0)
    const propellerRotation = (previousPropellerRotation + context.getPower() / 5) % 360
    context.setFloat("propellerRotation", propellerRotation)

    const builder = createPoseBuilder()
    builder.setRotation("propeller", 0, -propellerRotation, 0)
    builder.setRotation("propeller_tail", -propellerRotation * 5, 0, 0)

    builder.setRotation("lever1", context.getPitchInput() * -10, 0, -context.getRollInput() * -10)
    builder.setRotation("lever3", -context.getCollectivePitch() / 100 * 10, 0, 0)

    builder.setRotation("mg", 0, -context.getPartYRot("auto_cannon"), 0)
    builder.setRotation("mg_up", context.getPartXRot("auto_cannon"), 0, 0)

    let remainMissiles = context.getWeaponRemainAmmo("sighting_system", 1)
    for (let i = 0; i < missiles.length; i++) {
        if (i < missiles.length - remainMissiles) {
            builder.hideBone(missiles[i])
        }
    }
    return builder
}
