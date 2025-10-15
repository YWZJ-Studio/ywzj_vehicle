#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform vec2 Resolution;

in vec2 texCoord;
out vec4 fragColor;

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    // 把 UV 从 [0,1] 映射到 [-1,1]（以屏幕中心为原点）
    vec2 uv = (texCoord - 0.5) * 2.0;
    float r = length(uv);

    // Barrel Distortion (边缘弯曲)
    float k = 0.05; // 越大越弯
    uv *= 1.0 + k * r * r;

    // 映射回 [0,1]
    uv = uv * 0.5 + 0.5;

    // 超出边界的黑边
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        vec3 col = texture(DiffuseSampler, uv).rgb;

        // 扫描线效果
        float scan = 0.05 * sin(texCoord.y * Resolution.y * 1.5 + Time * 20.0);

        // 雪花噪声
        float noise = rand(vec2(texCoord.x * Resolution.x, texCoord.y * Resolution.y + Time * 50.0)) * 0.2;

        // 叠加特效
        col += noise;
        col -= scan;

        fragColor = vec4(col, 1.0);
    }
}
