#extension GL_OES_EGL_image_external : require

precision highp float;
varying vec2 vTexCoord;

uniform samplerExternalOES uTex0;
uniform sampler2D uTex1;
uniform vec2 uSize;

const float uAmount = 0.6;
const float uThreshold = 0.0;

const float uBrightness = 0.015;
const float uContrast = 0.20;

const float HUE_Red = 0.0;
const float HUE_Yellow = 60.0;
const float HUE_Green = 120.0;
const float HUE_Cyan = 180.0;
const float HUE_Blue = 240.0;
const float HUE_Magenta = 300.0;
                                    
const vec2 uRange = vec2(15.0, 45.0);
                                    
const vec3 uMaster = vec3(0.0,  0.0,  0.0);
const vec3 uRed    = vec3(0.0,  0.21, 0.12);
const vec3 uYellow = vec3(0.0,  0.23, 0.18);
const vec3 uGreen  = vec3(0.0,  0.0,  0.0);
const vec3 uCyan   = vec3(0.0,  0.05, -0.22);
const vec3 uBlue   = vec3(0.0,  0.08, -0.21);
const vec3 uMagenta = vec3(0.0, 0.0,  0.0);

const vec3 uParamS = vec3(-0.19,  0.04,  0.23);
const vec3 uParamM = vec3( -0.015,  -0.005,  0.015);
const vec3 uParamH = vec3( 0.16,  0.19,  0.23);

const vec2 BAL_SIZE = vec2(201.0, 256.0);
                                            
vec2 BAL_Center(vec2 v, vec2 s) { return (floor(v * s) + 0.5) / s; }

vec3 BAL_CheckMIN(vec3 param) {
    vec3 v = param * 100.0;
    float x = max(v.y, v.z) - v.x;
    float y = max(v.z, v.x) - v.y;
    float z = max(v.x, v.y) - v.z;
    v = clamp(vec3(x,y,z), 0.0, 255.0);
    return v;
}
                                
vec3 BAL_CheckMAX(vec3 param) {
    vec3 v = param * 100.0;
    float x = v.x - min(v.y, v.z);
    float y = v.y - min(v.z, v.x);
    float z = v.z - min(v.x, v.y);
    v = 255.0 - clamp(vec3(x,y,z), 0.0, 255.0);
    return v;
}
                                    
float BAL_Keep_glow(float val, float f1, float f2) {
    float f_min = min(f1, f2);
    float f_max = max(f1, f2);
    
    if (val <= f_min) {
        return (val - f_max) * 0.5;
    }
    
    if (val >= f_max) {
        return (val - f_min) * 0.5;
    }
    
    return (2.0 * val - f_max - f_min) * 0.5;
}
                                    
vec3 WORK_BAL(vec3 src) {
    vec3 rgb = src; 
    vec3 cmin = BAL_CheckMIN(uParamS);
    vec3 cmax = BAL_CheckMAX(uParamH);
    vec3 clen = cmax - cmin;
    vec3 cdir = step(0.0, clen);
    vec3 above_max = step(cmax, rgb * 255.0);
    vec3 cnew = clamp((rgb * 255.0 - cmin) / clen, 0.0, 1.0);
    cnew = cdir * cnew + (vec3(1.0) - cdir) * above_max;
    rgb.r = texture2D(uTex1, BAL_Center(vec2((1.0 + uParamM.x) * 0.5, cnew.r), BAL_SIZE)).b;
    rgb.g = texture2D(uTex1, BAL_Center(vec2((1.0 + uParamM.y) * 0.5, cnew.g), BAL_SIZE)).b;
    rgb.b = texture2D(uTex1, BAL_Center(vec2((1.0 + uParamM.z) * 0.5, cnew.b), BAL_SIZE)).b;
    return rgb;
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

vec3 RGB2HSL(vec3 rgb) {
    float maxVal = max(max(rgb.r, rgb.g), rgb.b);
    float minVal = min(min(rgb.r, rgb.g), rgb.b);
    float delta = maxVal - minVal;

    float L = (maxVal + minVal) * 0.5;
    float S = 0.0;
    float H = 0.0;

    if (delta != 0.0) {
        S = delta / (1.0 - abs(2.0 * L - 1.0));

        if (maxVal == rgb.r) {
            H = (rgb.g - rgb.b) / delta;
            if (rgb.g < rgb.b) H += 6.0;
        } else if (maxVal == rgb.g) {
            H = 2.0 + (rgb.b - rgb.r) / delta;
        } else {
            H = 4.0 + (rgb.r - rgb.g) / delta;
        }

        H /= 6.0;
    }

    return vec3(H, S, L);
}

vec3 HSL2RGB(vec3 hsl) {
    vec3 rgb = HUE2RGB(hsl.x);
    float C = (1.0 - abs(2.0 * hsl.z - 1.0)) * hsl.y;
    return (rgb - 0.5) * C + hsl.z;
}

float HSL_Weight(float hue, float center, vec2 range) {
    float value = hue * 360.0;
    float delta = abs(value - center);
    delta = min(delta, 360.0 - delta);
    if (delta <= range.x) {
        return 1.0;
    } else if (delta <= range.y) {
        return (range.y - delta) / (range.y - range.x);
    } else {
        return 0.0;
    }
}

float HSL_Saturation(float s,
                    float w_red, float w_yellow, float w_green,
                    float w_cyan, float w_blue, float w_magenta) {
    float f_master   = mix(1.0 + uMaster.y,    1.0 / (1.0 - uMaster.y),    step(0.0, uMaster.y)) - 1.0;
    float f_red      = mix(1.0 + uRed.y,       1.0 / (1.0 - uRed.y),       step(0.0, uRed.y)) - 1.0;
    float f_yellow   = mix(1.0 + uYellow.y,    1.0 / (1.0 - uYellow.y),    step(0.0, uYellow.y)) - 1.0;
    float f_green    = mix(1.0 + uGreen.y,     1.0 / (1.0 - uGreen.y),     step(0.0, uGreen.y)) - 1.0;
    float f_cyan     = mix(1.0 + uCyan.y,      1.0 / (1.0 - uCyan.y),      step(0.0, uCyan.y)) - 1.0;
    float f_blue     = mix(1.0 + uBlue.y,      1.0 / (1.0 - uBlue.y),      step(0.0, uBlue.y)) - 1.0;
    float f_magenta  = mix(1.0 + uMagenta.y,   1.0 / (1.0 - uMagenta.y),   step(0.0, uMagenta.y)) - 1.0;
    float factor = mix(1.0 - uMaster.y, 1.0 + uMaster.y, step(0.0, uMaster.y));
    float offset = f_red * w_red + f_yellow * w_yellow + f_green * w_green + f_cyan * w_cyan + f_blue * w_blue + f_magenta * w_magenta;
    float sat = clamp(s * (1.0 + f_master + offset * factor), 0.0, 1.0);
    return sat;
}

vec3 HSL_Lightness(vec3 hsl, vec3 src,
                float w_red, float w_yellow, float w_green,
                float w_cyan, float w_blue, float w_magenta) {
    float f_master = clamp(uMaster.z, -1.0, 1.0);
    vec3 rgb = HSL2RGB(hsl);
    rgb = mix(mix(vec3(0.0), rgb, 1.0 + f_master), mix(rgb, vec3(1.0), f_master), step(0.0, f_master));
    float maxv = max(max(src.r, src.g), src.b);
    float minv = min(min(src.r, src.g), src.b);
    float offset = uRed.z * w_red + uYellow.z * w_yellow + uGreen.z * w_green +
                uCyan.z * w_cyan + uBlue.z * w_blue + uMagenta.z * w_magenta;
    rgb = mix(mix(vec3(minv), rgb, 1.0 + offset), mix(rgb, vec3(maxv), offset), step(0.0, offset));
    return rgb;
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

vec3 WORK_HSL(vec3 src) {
    vec3 rgb = src;
    vec3 hsl = RGB2HSL(src);
    float w_red = HSL_Weight(hsl.x, HUE_Red, uRange);
    float w_yellow = HSL_Weight(hsl.x, HUE_Yellow, uRange);
    float w_green = HSL_Weight(hsl.x, HUE_Green, uRange);
    float w_cyan = HSL_Weight(hsl.x, HUE_Cyan, uRange);
    float w_blue = HSL_Weight(hsl.x, HUE_Blue, uRange);
    float w_magenta = HSL_Weight(hsl.x, HUE_Magenta, uRange);
    rgb = HSL_Lightness(hsl, rgb, w_red, w_yellow, w_green, w_cyan, w_blue, w_magenta);
    hsl = RGB2HSL(rgb);
    hsl.y = HSL_Saturation(hsl.y, w_red, w_yellow, w_green, w_cyan, w_blue, w_magenta);
    rgb = HSL2RGB(hsl);
    return rgb;
}

void main() {
    vec4 src = texture2D(uTex0, vTexCoord);
    vec3 rgb = src.rgb;
    rgb = WORK_USM(src, vTexCoord, uSize, uAmount, uThreshold);
    rgb = WORK_BC(rgb, uBrightness, uContrast);
    rgb = WORK_HSL(rgb);
    rgb = WORK_BAL(rgb);
    gl_FragColor = vec4(rgb, src.a);
}