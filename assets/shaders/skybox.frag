varying vec3 co_worldpos;

uniform float co_radius;
uniform vec4 co_topColor;
uniform vec4 co_bottomColor;

void main() {
    float t = (co_worldpos.y + co_radius / 10) / co_radius;
    vec4 left = vec4(1.0, 0.0, 0.0, 1.0);
    vec4 right = vec4(0.0, 1.0, 0.0, 1.0);

    gl_FragColor = mix(co_bottomColor, co_topColor, t);
}
