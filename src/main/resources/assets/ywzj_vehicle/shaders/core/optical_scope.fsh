#version 150

uniform vec2 ScreenSize;

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec2 position = (texCoord0 - 0.5) * ScreenSize / min(ScreenSize.x, ScreenSize.y);
    float radius = 0.46;
    float feather = 0.025;
    float distanceFromCenter = length(position);
    float mask = smoothstep(radius - feather, radius, distanceFromCenter);

    vec2 pixelWidth = fwidth(position);
    vec2 halfWidth = max(vec2(0.00065), pixelWidth * 0.5);
    vec2 lines = 1.0 - smoothstep(halfWidth - pixelWidth * 0.5,
                                halfWidth + pixelWidth * 0.5, abs(position));
    float crosshair = max(lines.x, lines.y);

    float haze = 0.12 + 0.03 * smoothstep(0.0, radius, distanceFromCenter);
    float blackOpacity = max(mask, crosshair);
    float hazeOpacity = haze * (1.0 - blackOpacity);
    float alpha = blackOpacity + hazeOpacity;
    vec3 hazeColor = vec3(0.96, 0.97, 1.0);
    fragColor = vec4(hazeColor * hazeOpacity / max(alpha, 0.00001), alpha);
}
