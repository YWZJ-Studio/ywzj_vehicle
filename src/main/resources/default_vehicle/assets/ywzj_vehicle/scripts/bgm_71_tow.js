function prepareBones() {
    context.rotateBone("rot",  0, -context.getPartYRot("tow"), 0)
    context.rotateBone("tow", context.getPartXRot("tow"), 0, 0)
}
