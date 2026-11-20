#version 150

uniform sampler2D Sampler0;
uniform vec2 ScreenSize;
uniform float SightScale;

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec2 imageSize = vec2(textureSize(Sampler0, 0));
    float fit = min(ScreenSize.x / imageSize.x, ScreenSize.y / imageSize.y);
    vec2 uv = 0.5 + (texCoord0 - 0.5) * ScreenSize / (imageSize * fit * SightScale);
    // Outside the image the mask is opaque. Inside it, the PNG alpha reveals the world.
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        vec2 halfTexel = 0.5 / imageSize;
        fragColor = texture(Sampler0, clamp(uv, halfTexel, 1.0 - halfTexel));
    }
}
