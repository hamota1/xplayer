#extension GL_OES_EGL_image_external : require

precision highp float;

varying vec2 textureCoordinate;

uniform samplerExternalOES inImageTexture; // 用于HDR

// HDR增强相关参数
uniform int mEnhanceQuality;    // 1=开启HDR
uniform float mContrastAdjust;
uniform float mBrightnessAdjust;
uniform float mSaturationAdjust;

// 清晰度增强相关参数
uniform int mOpenClarify;   // 1=开启清晰度增强
uniform vec2 uStep; //uStep= (1.0 / W, 1.0 / H)；W 和 H 是纹理的宽高
uniform float uAmount;      // 默认2.0
uniform float uThreshold;   // 默认0.007843
uniform float uBrightness;  // 默认0.015
uniform float uContrast;    // 默认0.38

//const float uAmount = 2.0;
//const float uThreshold = 0.007843;
//const float uBrightness = 0.015;
//const float uContrast = 0.38;

// HDR公用
const float epsilon = 1e-10;
const vec3 halfVal = vec3(0.5, 0.5, 0.5);

// HDR颜色变换函数
vec3 hslToRgbColor(vec3 hsl) {
    float r = abs(hsl.x * 6.0 - 3.0) - 1.0;
    float g = 2.0 - abs(hsl.x * 6.0 - 2.0);
    float b = 2.0 - abs(hsl.x * 6.0 - 4.0);
    vec3 rgb = clamp(vec3(r, g, b), 0.0, 1.0);
    float c = (1.0 - abs(2.0 * hsl.z - 1.0)) * hsl.y;
    return (rgb - 0.5) * c + hsl.z;
}
vec3 rgbToHslColor(vec3 rgb) {
    vec4 p = (rgb.g < rgb.b) ? vec4(rgb.bg, -1.0, 2.0 / 3.0) : vec4(rgb.gb, 0.0, -1.0 / 3.0);
    vec4 q = (rgb.r < p.x) ? vec4(p.xyw, rgb.r) : vec4(rgb.r, p.yzx);
    float c = q.x - min(q.w, q.y);
    float h = abs((q.w - q.y) / (6.0 * c + epsilon) + q.z);
    float l = q.x - c * 0.5;
    float s = c / (1.0 - abs(l * 2.0 - 1.0) + epsilon);
    return vec3(h, s, l);
}

// 清晰度增强相关
#define EPSILON 1e-10
vec3 saturate(vec3 v) { return clamp(v, vec3(0.0), vec3(1.0)); }
vec3 HUEtoRGB(float H) {
    float R = abs(H * 6.0 - 3.0) - 1.0;
    float G = 2.0 - abs(H * 6.0 - 2.0);
    float B = 2.0 - abs(H * 6.0 - 4.0);
    return saturate(vec3(R,G,B));
}
vec3 RGBtoHCV(vec3 RGB) {
    vec4 P = (RGB.g < RGB.b) ? vec4(RGB.bg, -1.0, 2.0/3.0) : vec4(RGB.gb, 0.0, -1.0/3.0);
    vec4 Q = (RGB.r < P.x) ? vec4(P.xyw, RGB.r) : vec4(RGB.r, P.yzx);
    float C = Q.x - min(Q.w, Q.y);
    float H = abs((Q.w - Q.y) / (6.0 * C + EPSILON) + Q.z);
    return vec3(H, C, Q.x);
}
vec3 RGBtoHSV(vec3 RGB) {
    vec3 HCV = RGBtoHCV(RGB);
    float S = HCV.y / (HCV.z + EPSILON);
    return vec3(HCV.x, S, HCV.z);
}
vec3 HSVtoRGB(vec3 HSV) {
    vec3 RGB = HUEtoRGB(HSV.x);
    return ((RGB - 1.0) * HSV.y + 1.0) * HSV.z;
}
float procThreshold(float src, float threshold) {
    if(abs(src) < threshold)
    return 0.0;
    return src -= (sign(src) * threshold);
}

void main() {
    // 只会有一个分支生效，两者互斥
    if (mOpenClarify == 1) {
        // ----------- 清晰度增强 ------------
        vec4 src = texture2D(inImageTexture, textureCoordinate);
        vec4 blr = vec4(0.0);
        // 7x7高斯核
        blr += texture2D(inImageTexture, textureCoordinate + vec2(-3.0 * uStep.x, -3.0 * uStep.y)) * 0.00296902;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(-3.0 * uStep.x, -1.5 * uStep.y)) * 0.01330621;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(-3.0 * uStep.x, 0.0)) * 0.02193823;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(-3.0 * uStep.x, 1.5 * uStep.y)) * 0.01330621;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(-3.0 * uStep.x, 3.0 * uStep.y)) * 0.00296902;

        blr += texture2D(inImageTexture, textureCoordinate + vec2(-1.5 * uStep.x, -3.0 * uStep.y)) * 0.01330621;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(-1.5 * uStep.x, -1.5 * uStep.y)) * 0.05963430;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(-1.5 * uStep.x, 0.0)) * 0.09832033;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(-1.5 * uStep.x, 1.5 * uStep.y)) * 0.05963430;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(-1.5 * uStep.x, 3.0 * uStep.y)) * 0.01330621;

        blr += texture2D(inImageTexture, textureCoordinate + vec2(0.0, -3.0 * uStep.y)) * 0.02193823;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(0.0, -1.5 * uStep.y)) * 0.09832033;
        blr += src * 0.16210282;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(0.0, 1.5 * uStep.y)) * 0.09832033;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(0.0, 3.0 * uStep.y)) * 0.02193823;

        blr += texture2D(inImageTexture, textureCoordinate + vec2(1.5 * uStep.x, -3.0 * uStep.y)) * 0.01330621;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(1.5 * uStep.x, -1.5 * uStep.y)) * 0.05963430;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(1.5 * uStep.x, 0.0)) * 0.09832033;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(1.5 * uStep.x, 1.5 * uStep.y)) * 0.05963430;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(1.5 * uStep.x, 3.0 * uStep.y)) * 0.01330621;

        blr += texture2D(inImageTexture, textureCoordinate + vec2(3.0 * uStep.x, -3.0 * uStep.y)) * 0.00296902;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(3.0 * uStep.x, -1.5 * uStep.y)) * 0.01330621;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(3.0 * uStep.x, 0.0)) * 0.02193823;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(3.0 * uStep.x, 1.5 * uStep.y)) * 0.01330621;
        blr += texture2D(inImageTexture, textureCoordinate + vec2(3.0 * uStep.x, 3.0 * uStep.y)) * 0.00296902;

        vec4 dif = src - blr;
        dif.r = procThreshold(dif.r, uThreshold);
        dif.g = procThreshold(dif.g, uThreshold);
        dif.b = procThreshold(dif.b, uThreshold);

        vec4 msk = src + dif * uAmount;
        vec3 rgbColor = (msk.rgb - 0.5) * max(uContrast + 1.0, 0.0) + 0.5;
        vec3 hsvColor = RGBtoHSV(rgbColor);
        hsvColor.z *= (uBrightness + 1.0);
        rgbColor = HSVtoRGB(hsvColor);
        rgbColor += (uBrightness * 0.5);
        rgbColor = saturate(rgbColor);
        gl_FragColor = vec4(rgbColor, 1.0);
        return;
    }
    else if (mEnhanceQuality == 1) {
        // ----------- HDR增强 ------------
        vec4 glRgbColor = texture2D(inImageTexture, textureCoordinate);
        vec3 hslColor = rgbToHslColor(glRgbColor.rgb);

        float saturation = 1.0 + smoothstep(0.05, 0.8, hslColor.y) / 2.0;
        saturation = clamp(saturation, 1.0, mSaturationAdjust);
        float average = (glRgbColor.r + glRgbColor.g + glRgbColor.b) / 3.0;

        saturation = step(0.05, average) == 1.0 ? saturation : 1.0;
        hslColor.y = clamp(hslColor.y * saturation, 0.0, 1.0);

        float diffc = 1.0 - smoothstep(0.0, 0.7, hslColor.z);
        diffc = mBrightnessAdjust + diffc * 0.05;
        float brightnessAdjust = hslColor.z <= 0.7 ? diffc : 0.0;
        hslColor.z = clamp(hslColor.z + brightnessAdjust, 0.0, 1.0);
        glRgbColor = vec4(hslToRgbColor(hslColor), glRgbColor.a);

        vec3 contrastColor = mContrastAdjust * (glRgbColor.rgb - halfVal) + halfVal;
        glRgbColor.r = clamp(contrastColor.r, 0.0, 1.0);
        glRgbColor.g = clamp(contrastColor.g, 0.0, 1.0);
        glRgbColor.b = clamp(contrastColor.b, 0.0, 1.0);

        gl_FragColor = glRgbColor;
        return;
    }
    else {
        // 默认直接输出原图
        gl_FragColor = texture2D(inImageTexture, textureCoordinate);
    }
}