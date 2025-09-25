attribute vec3 a_position;

uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;

varying vec3 co_worldpos;

void main() {
    gl_Position = u_projViewTrans * u_worldTrans * vec4(a_position, 1.0);
    co_worldpos = a_position.xyz;
}
