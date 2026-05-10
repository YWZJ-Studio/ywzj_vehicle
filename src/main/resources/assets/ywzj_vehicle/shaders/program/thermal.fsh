#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D ThermalSampler;

in vec2 texCoord;

out vec4 fragColor;

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

void main() {
    // CRT黑边检测：应用与CRT shader相同的桶形畸变，判断当前像素是否在黑边区域
    vec2 crtUV = (texCoord - 0.5) * 2.0;
    float crtR = length(crtUV);
    crtUV *= 1.0 + 0.05 * crtR * crtR;
    crtUV = crtUV * 0.5 + 0.5;

    if (crtUV.x < 0.0 || crtUV.x > 1.0 || crtUV.y < 0.0 || crtUV.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    vec4 thermalColor = textureLod(ThermalSampler, crtUV, 0.0);

    // 1. 背景处理 (冷 → 灰)
    float sceneLuma = luma(sceneColor.rgb);
    float bgGray = 0.1 + sceneLuma * 0.2;

    // 传感器噪点
    float noise = random(texCoord * 100.0);
    bgGray += (noise - 0.5) * 0.06;

    // 晕影
    vec2 uv = texCoord * (1.0 - texCoord.yx);
    float vig = uv.x * uv.y * 15.0;
    vig = pow(vig, 0.25);
    bgGray *= vig;

    vec3 finalColor = vec3(bgGray);

    // 2. 环境热源 (岩浆、火等 → 白)
    float warmth = sceneColor.r - max(sceneColor.g, sceneColor.b);
    float brightHeat = smoothstep(0.92, 1.0, sceneLuma);
    float warmHeat = smoothstep(0.5, 0.9, sceneLuma) * smoothstep(0.05, 0.4, warmth);
    float envHeat = max(brightHeat, warmHeat);

    if (envHeat > 0.01) {
        float envGray = 0.5 + 0.5 * envHeat;
        finalColor = mix(finalColor, vec3(envGray), clamp(envHeat + 0.4, 0.0, 1.0));
    }

    // 3. 实体热源 (最高优先级 → 白)
    bool isEntityHot = thermalColor.a > 0.01 || dot(thermalColor.rgb, vec3(1.0)) > 0.01;

    if (isEntityHot) {
        float texLuma = luma(thermalColor.rgb);
        float heat = 0.4 + 0.6 * texLuma;
        heat = pow(heat, 0.8);

        // 黑(冷) → 白(热)
        vec3 objectColor = vec3(heat);

        finalColor = mix(finalColor, objectColor, thermalColor.a);
    }

    fragColor = vec4(finalColor, 1.0);
}
