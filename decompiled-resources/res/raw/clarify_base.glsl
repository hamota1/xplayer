#extension GL_OES_EGL_image_external : require

precision highp float;
varying vec2 vTexCoord;

uniform samplerExternalOES uTex0;
uniform vec2 uSize;

const float uAmount = 0.6;
const float uThreshold = 0.0;

const float uBrightness = 0.025;
const float uContrast = 0.15;

vec3 HUE2RGB(float H) {
    H = fract(H);
    float R = abs(H * 6.0 - 3.0) - 1.0;
    float G = 2.0 - abs(H * 6.0 - 2.0);
    float B = 2.0 - abs(H * 6.0 - 4.0);
    return clamp(vec3(R, G, B), 0.0, 1.0);
}

vec3 RGB2HSV(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 HSV2RGB(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 WORK_USM(vec4 src, vec2 uv, vec2 size, float amount, float threshold) {
    vec2 step = vec2(1.0) / size;
    vec4 blr = src * 0.259272;
    blr += texture2D(uTex0, uv + vec2( 1.5 * step.x,  1.5 * step.y)) * 0.126788;
    blr += texture2D(uTex0, uv + vec2( 1.5 * step.x, -1.5 * step.y)) * 0.126788;
    blr += texture2D(uTex0, uv + vec2(-1.5 * step.x,  1.5 * step.y)) * 0.126788;
    blr += texture2D(uTex0, uv + vec2(-1.5 * step.x, -1.5 * step.y)) * 0.126788;
    blr += texture2D(uTex0, uv + vec2( 3.2 * step.x, 0.0)) * 0.029197;
    blr += texture2D(uTex0, uv + vec2(-3.2 * step.x, 0.0)) * 0.029197;
    blr += texture2D(uTex0, uv + vec2(0.0,  3.2 * step.y)) * 0.029197;
    blr += texture2D(uTex0, uv + vec2(0.0, -3.2 * step.y)) * 0.029197;
    blr += texture2D(uTex0, uv + vec2( 3.2 * step.x,  3.2 * step.y)) * 0.029197;
    blr += texture2D(uTex0, uv + vec2( 3.2 * step.x, -3.2 * step.y)) * 0.029197;
    blr += texture2D(uTex0, uv + vec2(-3.2 * step.x,  3.2 * step.y)) * 0.029197;
    blr += texture2D(uTex0, uv + vec2(-3.2 * step.x, -3.2 * step.y)) * 0.029197;

    vec4 dif = src - blr;
    vec3 dif_abs = abs(dif.rgb);
    vec3 dif_sign = sign(dif.rgb);
    dif.rgb = dif_sign * max(dif_abs - threshold, 0.0);

    vec3 usm = src.rgb + dif.rgb * amount;
    return usm;
}


vec3 WORK_BC(vec3 src, float b, float c) {
    vec3 rgb = (src - 0.5) * max(1.0 + c, 0.0) + 0.5;
    vec3 hsv = RGB2HSV(rgb);
    hsv.z *= (1.0 + b);
    rgb = HSV2RGB(hsv) + (b * 0.5);
    rgb = clamp(rgb, vec3(0.0), vec3(1.0));
    return rgb;
}
                                        
void main() {
    vec4 src = texture2D(uTex0, vTexCoord);
    vec3 rgb = WORK_USM(src, vTexCoord, uSize, uAmount, uThreshold);
    rgb = WORK_BC(rgb, uBrightness, uContrast);
    gl_FragColor = vec4(rgb, src.a);
}