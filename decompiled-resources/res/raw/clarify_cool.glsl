#extension GL_OES_EGL_image_external : require

precision highp float;
varying vec2 vTexCoord;

uniform samplerExternalOES uTex0;
uniform sampler2D uTex1;
uniform vec2 uSize;

const float uAmount = 0.6;
const float uThreshold = 0.0;

const float uBrightness = 0.015;
const float uContrast = 0.15;

const vec3 uParamS = vec3(0.0, 0.0, 0.30);
const vec3 uParamM = vec3(0.0, 0.0, 0.05);
const vec3 uParamH = vec3(-0.16, 0.10, 0.30);

const vec2 BAL_SIZE = vec2(201.0, 256.0);
                                            
vec2 BAL_Center(vec2 v, vec2 s) { return (floor(v * s) + 0.5) / s; }

float BAL_Process(float src, float s, float m, float h) {
    float cmax = 255.0 - clamp(h * 100.0, 0.0, 255.0);
    float cmin = clamp(-s * 100.0, 0.0, 255.0);
    float clen = cmax - cmin;
    float cnew = clamp((src * 255.0 - cmin) / clen, 0.0, 1.0);
    cnew = texture2D(uTex1, BAL_Center(vec2((1.0 - h) * 0.5, cnew), BAL_SIZE)).r;
    cnew = texture2D(uTex1, BAL_Center(vec2((1.0 - s) * 0.5, cnew), BAL_SIZE)).r;
    float clr = texture2D(uTex1, BAL_Center(vec2((1.0 + m) * 0.5, cnew), BAL_SIZE)).g;
    return clr;
}
                                    
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

    vec3 dif = src.rgb - blr.rgb;
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
        
vec3 WORK_BAL(vec3 src) {
    vec3 rgb = src;
    rgb.r = BAL_Process(rgb.r, uParamS.x, uParamM.x, uParamH.x);
    rgb.g = BAL_Process(src.g, uParamS.y, uParamM.y, uParamH.y);
    rgb.b = BAL_Process(src.b, uParamS.z, uParamM.z, uParamH.z);
    return rgb;
}

void main() {
    vec4 src = texture2D(uTex0, vTexCoord);
    vec3 rgb = src.rgb;
    rgb = WORK_USM(src, vTexCoord, uSize, uAmount, uThreshold);
    rgb = WORK_BC(rgb, uBrightness, uContrast);
    rgb = WORK_BAL(rgb);
    gl_FragColor = vec4(rgb, src.a);
}