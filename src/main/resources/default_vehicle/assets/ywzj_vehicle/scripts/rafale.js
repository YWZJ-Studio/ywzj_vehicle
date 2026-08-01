const magic = ["mod_migic_l", "mod_migic_r"]
const mica = ["mod_mica_em_l", "mod_mica_em_r"]
const gbu12 = ["missile_gbu12", "missile_gbu4", "missile_gbu3", "missile_gbu6", "missile_gbu2", "missile_gbu5"]

function updateBones(context) {
    const tickCount = context.tickCount()
    if (tickCount % 20 < 2) {
        context.setBoneIlluminated("illum_flash_mid", true)
    } else {
        context.setBoneIlluminated("illum_flash_mid", false)
    }

    const pitchInput = context.getPitchInput()
    const yawInput = context.getYawInput()
    const rollInput = context.getRollInput()

    const builder = createPoseBuilder()
    builder.setRotation("s_wing_l", pitchInput * 16, 0, 0)
    builder.setRotation("s_wing_r", pitchInput * 16, 0, 0)
    builder.setRotation("wing_back1_l", -rollInput * 16, 0, 0)
    builder.setRotation("wing_back1_r", rollInput * 16, 0, 0)
    builder.setRotation("tail3", 0, -yawInput * 16, 0)
    for (let i = 0; i < magic.length; i++) {
        if (i < magic.length - context.getWeaponRemainAmmo("sighting_system", 1)) {
            builder.hideBone(magic[i])
        }
    }
    for (let i = 0; i < mica.length; i++) {
        if (i < mica.length - context.getWeaponRemainAmmo("sighting_system", 2)) {
            builder.hideBone(mica[i])
        }
    }
    for (let i = 0; i < gbu12.length; i++) {
        if (i < gbu12.length - context.getWeaponRemainAmmo("sighting_system", 3)) {
            builder.hideBone(gbu12[i])
        }
    }
    return builder
}
