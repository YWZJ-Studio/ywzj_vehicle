#version 150

in vec2 texCoord0;
in vec4 vertexColor;
in vec3 normal;

out vec4 fragColor;

const float PI = 3.141592653589793;
const float TAU = 6.283185307179586;

void main() {
    vec2 uv = texCoord0 * 2.0 - 1.0;

    float dist = length(uv);

    float edge = 0.015;
    float alpha = 1.0 - smoothstep(1.0 - edge, 1.0 + edge, dist);

    float thickness = normal.x;
    if (thickness > 0.0) {
        float inner = 1.0 - thickness;
        float d = abs(dist - inner);
        alpha = 1.0 - smoothstep(thickness - edge, thickness + edge, d);
    }

    float start = normal.y;
    float end = normal.z;

    float angle = atan(uv.y, uv.x);
    float normalizedAngle = mod(angle + PI * 0.5 + TAU, TAU) / TAU;

    float len = end - start;
    if (len < 0.0) len += 1.0;
    float relPos = normalizedAngle - start;
    relPos = relPos - floor(relPos);

    bool isFullCircle = abs(start - end) < 0.00001;

    if (!isFullCircle) {
         if (relPos > len) {
             discard;
         }
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}