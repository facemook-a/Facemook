shader_type spatial;
render_mode cull_disabled;
varying vec3 world;
void vertex() {
 world=(MODEL_MATRIX*vec4(VERTEX,1.0)).xyz;
 VERTEX.y+=sin(world.x*0.10+TIME*0.9)*0.13+sin(world.z*0.15+world.x*0.06-TIME*1.1)*0.07;
}
void fragment() {
 vec2 ripple=vec2(sin(world.x*0.13+world.z*0.21+TIME)*0.08+sin(world.x*0.61+world.z*0.42-TIME*0.8)*0.025,
 cos(world.z*0.17+world.x*0.12-TIME*0.9)*0.07+sin(world.x*0.39-world.z*0.65+TIME)*0.020);
 NORMAL=normalize(NORMAL+vec3(ripple.x,0.0,ripple.y));
 float shine=pow(max(sin(world.x*0.17+world.z*0.24+TIME)*0.5+0.5,0.0),8.0);
 float edge=157.08+4.17*sin(world.z*0.025);
 float foam=(1.0-smoothstep(0.3,1.5,abs(world.x-edge+sin(world.z*0.7+TIME)*0.2)))*(0.7+0.3*sin(world.z*2.7+TIME));
 ALBEDO=mix(mix(vec3(0.012,0.17,0.24),vec3(0.05,0.35,0.39),shine*0.25),vec3(0.72,0.83,0.81),foam);
 ROUGHNESS=0.25;METALLIC=0.08;SPECULAR=0.65;
}
