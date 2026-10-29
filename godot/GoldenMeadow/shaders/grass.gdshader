shader_type spatial;
render_mode cull_disabled;
uniform vec3 root_color : source_color = vec3(0.065, 0.14, 0.027);
uniform vec3 tip_color : source_color = vec3(0.24, 0.45, 0.085);
uniform float draw_distance = 46.0;
varying float shade;
void vertex() {
    vec3 world = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
    float dist = distance(CAMERA_POSITION_WORLD.xz, world.xz);
    float fade = 1.0 - smoothstep(draw_distance * 0.72, draw_distance, dist);
    VERTEX *= fade;
    float bend = pow(1.0 - UV.y, 1.6);
    float wind = sin(world.x * 0.34 + world.z * 0.27 + TIME * 1.8)
               + 0.45 * sin(world.z * 0.61 - TIME * 2.4);
    VERTEX.x += wind * bend * 0.19;
    VERTEX.z += sin(world.x * 0.22 + TIME) * bend * 0.10;
    shade = 0.85 + 0.15 * sin(world.x * 4.3 + world.z * 3.7);
}
void fragment() {
    ALBEDO = mix(tip_color, root_color, pow(UV.y, 1.1)) * shade;
    ROUGHNESS = 0.94;
    // A little backlighting keeps the thin leaves readable on mobile.
    EMISSION = ALBEDO * (1.0 - UV.y) * 0.16;
    NORMAL = FRONT_FACING ? NORMAL : -NORMAL;
}
