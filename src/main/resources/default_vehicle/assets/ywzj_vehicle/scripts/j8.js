const missiles = ["missile1", "missile4", "missile2", "missile5", "missile3", "missile6"]

function updateBones(context) {
    const pitchInput = context.getPitchInput()
    const yawInput = context.getYawInput()
    const rollInput = context.getRollInput()

    const builder = createPoseBuilder();
    builder.setRotation("wingLB", pitchInput * 16, 0, 0);
    builder.setRotation("wingRB", pitchInput * 16, 0, 0);
    builder.setRotation("wingLR", rollInput * 16, 0, 0);
    builder.setRotation("wingRR", -rollInput * 16, 0, 0);
    builder.setRotation("2", 0, -yawInput * 16, 0);
    let remainMissiles = context.getWeaponRemainAmmo("seat", 1)
    for (let i = 0; i < missiles.length; i++) {
        if (i < missiles.length - remainMissiles) {
            builder.hideBone(missiles[i])
        }
    }
    return builder;
}
