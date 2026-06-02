#version 150

in vec4 Position;

uniform mat4 ProjMat;
uniform vec2 OutSize;

out vec2 texCoord;

void main(){
    vec4 out_pos = ProjMat * Position;
    gl_Position = vec4(out_pos.xy, 0.2, 1.0);
    texCoord = Position.xy / OutSize;
}
