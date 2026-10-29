extends Node3D
const Land=preload("res://scripts/landscape.gd")
var world: Node3D
var people: Array[CharacterBody3D]=[]
var birds: Array[Node3D]=[]
var motes: Array[MeshInstance3D]=[]
var ocean: MeshInstance3D
var ocean_audio: AudioStreamPlayer
var foot_audio: AudioStreamPlayer3D
var effect_audio: AudioStreamPlayer
var foot_clock:=0.0
var was_grounded:=false
var time:=0.0
var interaction: Node3D

func _ready() -> void:
    world=get_parent()
    for name in ["Music","Ambience","Effects"]:
        if AudioServer.get_bus_index(name)<0:
            AudioServer.add_bus()
            AudioServer.set_bus_name(AudioServer.bus_count-1,name)
    world.wind_player.bus="Ambience"
    for door in world.doors:door.speaker.bus="Effects"
    for p in Land.VILLAGE:
        if p==Vector2.ZERO:continue
        world._make_cottage(Vector3(p.x,Land.height(p.x,p.y),p.y))
    _make_ocean()
    _make_people()
    _make_birds()
    _make_motes()
    _start_audio()

func _loop(name: String,db: float) -> AudioStreamPlayer:
    var player:=AudioStreamPlayer.new()
    var stream=load("res://assets/audio/"+name+".wav").duplicate()
    stream.loop_mode=AudioStreamWAV.LOOP_FORWARD
    stream.loop_end=stream.data.size()/2
    player.stream=stream
    player.volume_db=db
    player.bus="Music" if name=="music" else "Ambience"
    add_child(player)
    player.play()
    return player

func _start_audio() -> void:
    _loop("music",-4.0)
    _loop("birds",-13.0)
    ocean_audio=_loop("ocean",-25.0)
    foot_audio=AudioStreamPlayer3D.new()
    foot_audio.stream=load("res://assets/audio/step.wav")
    foot_audio.volume_db=-4.0
    foot_audio.bus="Effects"
    world.player.add_child(foot_audio)
    effect_audio=AudioStreamPlayer.new()
    effect_audio.bus="Effects"
    add_child(effect_audio)

func play_ui_sound() -> void:
    effect_audio.stream=load("res://assets/audio/interact.wav")
    effect_audio.play()

func _make_ocean() -> void:
    ocean=MeshInstance3D.new()
    var plane:=PlaneMesh.new()
    plane.size=Vector2(1800,2000)
    plane.subdivide_width=140
    plane.subdivide_depth=140
    ocean.mesh=plane
    var mat:=ShaderMaterial.new()
    mat.shader=load("res://shaders/ocean.gdshader")
    ocean.material_override=mat
    ocean.position=Vector3(1050,-2.5,0)
    ocean.extra_cull_margin=2
    add_child(ocean)

func _make_people() -> void:
    for i in range(3):
        var person=load("res://scripts/companion.gd").new()
        person.person_name=["ليلى","سلمى","نور"][i]
        person.position=Vector3(148+i*2.0,Land.height(148+i*2.0,-8+i*10.0)+1.0,-8+i*10.0)
        add_child(person)
        people.append(person)

func _make_birds() -> void:
    var mat:=StandardMaterial3D.new()
    mat.albedo_color=Color("31302a")
    mat.cull_mode=BaseMaterial3D.CULL_DISABLED
    for i in range(12):
        var bird:=Node3D.new()
        add_child(bird)
        for side in [-1,1]:
            var wing:=MeshInstance3D.new()
            var st:=SurfaceTool.new()
            st.begin(Mesh.PRIMITIVE_TRIANGLES)
            for p in [Vector3(0,0,0),Vector3(side*0.55,0,-0.06),Vector3(side*0.13,0,0.24)]:st.add_vertex(p)
            st.generate_normals()
            wing.mesh=st.commit()
            wing.material_override=mat
            bird.add_child(wing)
        birds.append(bird)

func _make_motes() -> void:
    var mesh:=SphereMesh.new()
    mesh.radius=0.025
    mesh.height=0.05
    mesh.radial_segments=6
    mesh.rings=3
    var mat:=StandardMaterial3D.new()
    mat.albedo_color=Color("ffdd7e")
    mat.emission_enabled=true
    mat.emission=Color("ffcd5a")
    mat.emission_energy_multiplier=2
    for i in range(24):
        var mote:=MeshInstance3D.new()
        mote.mesh=mesh
        mote.material_override=mat
        mote.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
        add_child(mote)
        motes.append(mote)

func interact() -> void:
    _update_target()
    if interaction:interaction.interact()
    else:world.hud.announce("اقترب من باب أو من شخصية على الشاطئ")

func _update_target() -> void:
    interaction=null
    var best:=3.5
    for target in world.doors+people:
        var d: float=world.player.position.distance_to(target.global_position)
        if d<best:
            best=d
            interaction=target

func interaction_text() -> String:
    if not interaction:return "تفاعل · E"
    if interaction is CharacterBody3D:return "رافقني" if not interaction.following else "إنهاء المرافقة"
    return "فتح الباب" if not interaction.opened else "سد الباب"

func _process(delta: float) -> void:
    time+=delta
    ocean.position.x=maxf(1050.0,world.player.position.x)
    ocean.position.z=world.player.position.z
    _update_target()
    var speed:=Vector2(world.player.velocity.x,world.player.velocity.z).length()
    foot_clock+=delta
    if speed>0.8 and world.player.is_on_floor() and foot_clock>(0.28 if world.player.sprinting else 0.46):
        foot_clock=0
        foot_audio.pitch_scale=randf_range(0.9,1.1)
        foot_audio.play()
    if world.player.is_on_floor() and not was_grounded:
        effect_audio.stream=load("res://assets/audio/land.wav")
        effect_audio.play()
    if not world.player.is_on_floor() and was_grounded and world.player.velocity.y>2:
        effect_audio.stream=load("res://assets/audio/jump.wav")
        effect_audio.play()
    was_grounded=world.player.is_on_floor()
    ocean_audio.volume_db=lerpf(-35.0,-4.0,smoothstep(75.0,150.0,world.player.position.x))
    for i in range(birds.size()):
        var angle:=time*0.12+i*0.7
        var bird:=birds[i]
        bird.position=Vector3(90+sin(angle)*70,18+sin(angle*1.7)*4,-15+cos(angle)*65)
        bird.rotation.y=-angle
        bird.get_child(0).rotation.z=sin(time*5+i)*0.5
        bird.get_child(1).rotation.z=-sin(time*5+i)*0.5
    for i in range(motes.size()):
        var angle:=time*0.2+i*2.4
        var offset:=Vector3(sin(angle)*8,0.8+sin(time+i)*0.4,cos(angle*0.7)*8)
        var p: Vector3=world.player.position+offset
        p.y=Land.height(p.x,p.z)+offset.y
        motes[i].position=p
