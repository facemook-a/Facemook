shader_type spatial;
uniform sampler2D ground_texture : source_color, filter_linear_mipmap, repeat_enable;
uniform bool distant_terrain = false;
varying vec3 world_pos;
const vec2 homes[8]={vec2(-35,-18),vec2(35,-13),vec2(-55,32),vec2(25,67),vec2(-65,-63),vec2(65,-58),vec2(75,32),vec2(20,-98)};
float trail(vec2 p,vec2 a,vec2 b){vec2 edge=b-a;float t=clamp(dot(p-a,edge)/dot(edge,edge),0.,1.);return 1.-smoothstep(.75,1.45,length(p-a-edge*t));}
float terrain_height(vec2 p) {
    float rolling = sin(p.x*0.018)*cos(p.y*0.016)*5.0
                  + sin(p.x*0.047+p.y*0.015)*1.7 + cos(p.y*0.036-p.x*0.025)*1.2;
    rolling*=smoothstep(10.0,25.0,length(p));
    float coast=-1.2-(p.x-130.0)*0.048+sin(p.y*0.025)*0.20;
    return mix(rolling,coast,smoothstep(95.0,150.0,p.x));
}
void vertex() {
    world_pos = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
    if (distant_terrain) {
        VERTEX.y = terrain_height(world_pos.xz)-3.0;
        NORMAL = normalize(vec3(terrain_height(world_pos.xz-vec2(0.2,0))-terrain_height(world_pos.xz+vec2(0.2,0)),
                      0.4, terrain_height(world_pos.xz-vec2(0,0.2))-terrain_height(world_pos.xz+vec2(0,0.2))));
    }
}
void fragment() {
    vec3 t = texture(ground_texture, world_pos.xz * 0.18).rgb;
    float variation = sin(world_pos.x * 0.09 + sin(world_pos.z * 0.12)) * 0.06;
    vec3 grass = t * vec3(0.23, 0.39, 0.11) + variation * 0.5;
    float path_center = sin(world_pos.z * 0.12) * 1.5;
    float path_mask = (1.0-smoothstep(0.8, 2.1, abs(world_pos.x-path_center)))
                     * smoothstep(2.0, 6.0, world_pos.z) * (1.0-smoothstep(27.0, 36.0, world_pos.z));
    if(length(world_pos.xz)<155.){for(int i=0;i<8;i++){path_mask=max(path_mask,trail(world_pos.xz,vec2(0,7),homes[i]));}}
    ALBEDO = mix(grass, t * vec3(0.53, 0.43, 0.27), path_mask);
    ALBEDO=mix(ALBEDO,t*vec3(0.75,0.64,0.40),smoothstep(126.0,145.0,world_pos.x));
    ROUGHNESS = 1.0;
}
