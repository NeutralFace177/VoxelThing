#version 330 core

layout (location = 0) in vec3 aPos;

out vec3 pos;

uniform mat4 view;
uniform mat4 proj;

void main() {
    mat4 centerView = mat4(mat3(view));
    gl_Position = proj * centerView * vec4(aPos, 1.0);
    pos = aPos;
}