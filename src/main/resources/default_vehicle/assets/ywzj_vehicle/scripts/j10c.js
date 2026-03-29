const missiles_pl_12 = ["pl-12-1", "pl-12-3", "pl-12-2", "pl-12-4", "pl-12-5", "pl-12-6"]
const missiles_pl_8 = ["missile1", "missile2"]

function updateBones(context) {
    const pitchInput = context.getPitchInput()
    const yawInput = context.getYawInput()
    const rollInput = context.getRollInput()

    const builder = createPoseBuilder();
    builder.setRotation("wingLR", -pitchInput * 8, 0, 0);
    builder.setRotation("wingRR", -pitchInput * 8, 0, 0);
    builder.setRotation("wingLB", rollInput * 12, 0, 0);
    builder.setRotation("wingRB", -rollInput * 12, 0, 0);
    builder.setRotation("weiyiR2", 0, -yawInput * 20, 0);
    builder.setRotation("ctrl", -8 * pitchInput, 0, -8 * rollInput);
    let remainMissiles = context.getWeaponRemainAmmo("sighting_system", 1)
    for (let i = 0; i < missiles_pl_12.length; i++) {
        if (i < missiles_pl_12.length - remainMissiles) {
            builder.hideBone(missiles_pl_12[i])
        }
    }
    remainMissiles = context.getWeaponRemainAmmo("sighting_system", 2)
    for (let i = 0; i < missiles_pl_8.length; i++) {
        if (i < missiles_pl_8.length - remainMissiles) {
            builder.hideBone(missiles_pl_8[i])
        }
    }
    return builder;
}
