function updateBones(context) {
    const pitchInput = context.getPitchInput()
    const rollInput = context.getRollInput()

    const tailL = Math.min(1, Math.max(-1, pitchInput + rollInput)) * 16;
    const tailR = Math.min(1, Math.max(-1, pitchInput - rollInput)) * 16;

    const builder = createPoseBuilder()
    builder.setRotation("ctr_tail_l", tailL, 0, 0)
    builder.setRotation("ctr_tail_r", tailR, 0, 0)
    return builder
}
