#version 150

in vec2 texCoord;

uniform sampler2D DiffuseSampler;
uniform float Progress;
uniform int Type;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float distFromCenter = distance(texCoord, vec2(0.5, 0.5));
    float fProgress = clamp(Progress, 0.0, 1.0);
    if (Type == 1) {
        float darkness = 1.0 - 0.4 * fProgress - distFromCenter * 1.5 * fProgress;
        darkness = pow(clamp(darkness, 0.0, 1.0), fProgress);
        float redIntensity = fProgress * 0.8;
        vec3 pureRed = vec3(color.r * 0.8, 0.0, 0.0);
        vec3 mixedColor = mix(color.rgb, pureRed, redIntensity) + vec3(0.4 * fProgress, 0.0, 0.0);
        fragColor = vec4(mixedColor * darkness, color.a);
    } else if (Type == 2) {
        float darkness = 1.0 - 0.5 * fProgress - distFromCenter * 1.5 * fProgress;
        darkness = pow(clamp(darkness, 0.0, 1.0), fProgress);
        float gray = dot(color.rgb, vec3(0.05, 0.587, 0.114));
        float grayFactor = fProgress * 0.5;
        vec3 mixedColor = mix(color.rgb, vec3(gray), grayFactor) + vec3(0.2 * fProgress, 0.0, 0.0);
        fragColor = vec4(mixedColor * darkness, color.a);
    } else {
        fragColor = color;
    }
}