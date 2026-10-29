//attribute vec4 av_Position;
//attribute vec2 af_Position;
//varying vec2 v_texPosition;
//void main() {
//    gl_Position = av_Position;
//    v_texPosition = af_Position;
//}
attribute vec4 position;
attribute vec2 inputTextureCoordinate;
varying vec2 textureCoordinate;
uniform mat4 vertexMatrix;
void main() {
    gl_Position = vertexMatrix * position;
    textureCoordinate = inputTextureCoordinate.xy;
}
