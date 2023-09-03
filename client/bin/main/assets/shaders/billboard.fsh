#version 330 core

in vec3 fPos;
in vec2 uv;

out vec4 fColor;

uniform sampler2D tex;
uniform bool hasTex;
uniform vec4 color;
// Sky
uniform sampler2D skyTex;
uniform float skyWidth;
uniform float skyHeight;
// Fog
uniform vec3 camPos;
uniform float camFar;

float doFog(float fog) {
    return fog * 2.0 - 1.0;
}

void main() {
    if (texture(tex, uv).a == 0.0) discard;
    float fog = clamp(distance(fPos, camPos) / camFar, 0.0, 1.0);
    fog = clamp(doFog(fog), 0.0, 1.0);

    vec4 worldColor = color;
    if (hasTex) worldColor *= texture(tex, uv);
    vec4 skyColor = texture(skyTex, gl_FragCoord.xy / vec2(skyWidth, skyHeight));

    fColor = mix(worldColor, skyColor, fog);
}
