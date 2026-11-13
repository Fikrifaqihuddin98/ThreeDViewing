// ==========================================================
// FILE: src/main/resources/shaders/phong.frag (Fragment Shader)
// Phong / Blinn-Phong per-pixel + Texture Mapping + Shadow Mapping (PCF 3x3)
// ==========================================================
#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;   // texture unit 0
uniform sampler2D shadowMap;        // texture unit 1 (Depth FBO)
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;
uniform bool useBias;               // false -> bias = 0 (untuk mengamati Shadow Acne)
uniform float biasScale;            // pengali bias (tombol , dan .)
uniform float specularStrength;
uniform bool unlit;                 // true untuk penanda posisi cahaya

// ---------------------------------------------------------------
// Indikator 5 : bandingkan d_current dengan d_shadow (+ bias)
// return 0.0 = terang, 1.0 = bayangan penuh (rata-rata 9 sampel -> tepi halus)
// ---------------------------------------------------------------
float shadowFactor(vec4 fragPosLS, vec3 norm, vec3 lightDir) {
    vec3 proj = fragPosLS.xyz / fragPosLS.w;      // perspective divide
    proj = proj * 0.5 + 0.5;                      // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0) return 0.0;                 // di luar far plane cahaya -> tidak berbayang

    // bias dinamis berdasarkan kemiringan permukaan (rumus modul, konstanta disesuaikan skala scene)
    float bias = useBias ? max(0.01 * (1.0 - dot(norm, lightDir)), 0.002) * biasScale : 0.0;

    float shadow = 0.0;
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float dShadow = texture(shadowMap, proj.xy + vec2(x, y) * texel).r;
            shadow += (proj.z - bias > dShadow) ? 1.0 : 0.0;   // d_current > d_shadow + bias
        }
    }
    return shadow / 9.0;
}

void main() {
    if (unlit) {                                  // sumber cahaya digambar polos
        FragColor = vec4(lightColor, 1.0);
        return;
    }

    // 1. Ambient Lighting
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse Lighting (Lambertian)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular Lighting (Phong vs Blinn-Phong)
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (diff > 0.0) {                             // tanpa highlight pada sisi membelakangi cahaya
        if (useBlinnPhong) {
            vec3 halfwayDir = normalize(lightDir + viewDir);
            spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);   // Blinn-Phong
        } else {
            vec3 reflectDir = reflect(-lightDir, norm);
            spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Standard Phong
        }
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow: terang = 1.0 , bayangan = 0.2 (hanya diffuse + specular yang diredam)
    float shadow = useShadow ? shadowFactor(FragPosLightSpace, norm, lightDir) : 0.0;
    float visibility = 1.0 - 0.8 * shadow;

    // 5. Combine Texture & Lighting
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + visibility * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
