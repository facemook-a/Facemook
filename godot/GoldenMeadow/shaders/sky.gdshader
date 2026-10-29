shader_type sky;
// Blue golden-hour sky, soft cloud banks and a single plausible solar halo.
uniform vec3 sun_direction = vec3(-0.48,0.45,-0.75);
float hash(vec3 p){p=fract(p*.3183099+vec3(.1,.2,.3));p*=17.;return fract(p.x*p.y*p.z*(p.x+p.y+p.z));}
float noise3(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm(vec3 p){float s=0.,a=.55;for(int i=0;i<4;i++){s+=noise3(p)*a;p=p*2.03+vec3(3.1,1.7,.8);a*=.5;}return s;}
void sky(){
 vec3 d=normalize(EYEDIR),sun=normalize(sun_direction);
 float up=max(d.y,0.),alignment=max(dot(d,sun),0.);
 vec3 color=mix(vec3(.74,.77,.72),vec3(.12,.36,.64),pow(up,.45));
 color+=vec3(.65,.32,.09)*pow(alignment,18.)*.55;
 vec3 p=d/max(d.y+.22,.13)*3.0;
 float n=fbm(p);
 float cloud=smoothstep(.47,.66,n)*smoothstep(.01,.18,d.y);
 float lighting=smoothstep(.68,.48,n);
 vec3 tint=mix(vec3(.30,.40,.49),vec3(.94,.89,.74),lighting);
 color=mix(color,tint,cloud*.94);
 float rim=exp(-abs(n-.49)*75.)*pow(alignment,8.)*smoothstep(.03,.2,d.y);
 color+=vec3(1.,.70,.30)*rim*.28;
 color+=vec3(1.6,1.2,.65)*pow(alignment,1200.)*(1.-cloud*.8);
 COLOR=max(color,vec3(0.));
}
