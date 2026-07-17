const missiles = ["missile_left_1", "missile_right_1", "missile_left_2", "missile_right_2", "missile_left_3", "missile_right_3", "missile_left_4", "missile_right_4"]

function updateBones(context) {
    const previousPropellerRotation = context.getFloat("propellerRotation", 0);
    const propellerRotation = (previousPropellerRotation + context.getPower() / 5) % 360;
    context.setFloat("propellerRotation", propellerRotation)

    const builder = createPoseBuilder();
    builder.setRotation("z10w_top", 0, -propellerRotation, 0);
    builder.setRotation("z10w_tail", -propellerRotation * 5, 0, 0);
    builder.setRotation("z10w_camera", context.getPartXRot("sighting_system"), context.getPartYRot("sighting_system"), 0);
    builder.setRotation("z10w_canno", context.getPartXRot("auto_cannon"), context.getPartYRot("auto_cannon"), 0);
    let remainMissiles = context.getWeaponRemainAmmo("sighting_system", 1)
    for (let i = 0; i < missiles.length; i++) {
        if (i < missiles.length - remainMissiles) {
            builder.hideBone(missiles[i])
        }
    }
    return builder;
}
