function updateBones(context) {
    const builder = createPoseBuilder()
    if (context.getWeaponRemainAmmo("driver", 0) === 0) {
        builder.hideBone("missile")
    }
    return builder
}
