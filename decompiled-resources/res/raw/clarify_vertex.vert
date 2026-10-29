attribute vec4 aPosCoord;
attribute vec2 aTexCoord;

varying mediump vec2 vTexCoord;

uniform mat4 uSTMatrix;

void main() {
    gl_Position = aPosCoord;
    vec4 tc = vec4(aTexCoord, 0.0, 1.0);
    vTexCoord = (uSTMatrix * tc).xy;
}