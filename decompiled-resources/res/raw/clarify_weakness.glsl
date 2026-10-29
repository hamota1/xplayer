#extension GL_OES_EGL_image_external : require
precision highp float;
uniform samplerExternalOES uTex0;
uniform int uMode;
varying vec2 vTexCoord;

const mat3 M_RGB2LMS = mat3(
    0.3904725,  0.07092586, 0.02314268,
    0.54990437, 0.96310739, 0.12801221,
    0.00890159, 0.00135809, 0.93605194
);

const mat3 M_LMS2RGB = mat3(
    2.85831110, -0.210434776, -0.0418895045,
    -1.62870796,  1.15841493,  -0.118154333,
    -0.0248186967, 0.000320463334, 1.06888657
);

void main() {
    vec4 src = texture2D(uTex0, vTexCoord);
    vec4 out_color = src;
    mat3 mat_mode;
    if(uMode == 1) {
        mat_mode = mat3(0.0, 0.0, 0.0, 0.90822864, 1.0, 0.0, 0.008192, 0.0, 1.0);
    } else if (uMode == 2) {
        mat_mode = mat3(1.0, 1.10104433, 0.0, 0.0, 0.0, 0.0, 0.0, -0.00901975, 1.0);
    } else if (uMode == 3) {
        mat_mode = mat3(1.0, 0.0, -0.15773032, 0.0, 1.0, 1.19465634, 0.0, 0.0, 0.0);
    }
    out_color.rgb = M_LMS2RGB * mat_mode * M_RGB2LMS * src.rgb;
    gl_FragColor = vec4(out_color.rgb, src.a);
}