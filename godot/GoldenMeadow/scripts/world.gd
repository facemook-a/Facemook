extends Node3D

const Land = preload("res://scripts/landscape.gd")
const Walker = preload("res://scenes/player.tscn")
const Profile = preload("res://scripts/profile_store.gd")
const Overlay = preload("res://scripts/hud.gd")
var player: CharacterBody3D
var hud: CanvasLayer
var terrain := {}
var grass := {}
var pending: Array[Vector2i] = []
var center := Vector2i(99999,99999)
var grass_count := 3000
var grass_shape: ArrayMesh
var ground_material: ShaderMaterial
var grass_material: ShaderMaterial
var stone_material: StandardMaterial3D
var wood_material: StandardMaterial3D
var leaf_material: StandardMaterial3D
var wisps: Array[Node3D] = []
var collected := {}
var locations := [Vector2(17,-23),Vector2(-42,-34),Vector2(60,-68),Vector2(-83,18),Vector2(91,62),Vector2(-15,-112)]
var time := 0.0
var save_clock := 0.0
var wind_player: AudioStreamPlayer
var wind_enabled := true
var horizon: MeshInstance3D
var doors: Array[Node3D] = []
var activities: Node3D
var journey: Node
var saved_data: Dictionary={}
var settings: Dictionary={}
var navigation_name := ""
var navigation_target := Vector3.ZERO
var houses: Array[Node3D]=[]

func _ready() -> void:
    settings=Profile.settings()
    _setup_inputs()
    _setup_materials()
    _setup_light()
    _setup_horizon()
    _make_cottage()
    for p in [Vector2(-9,-3),Vector2(10,-4),Vector2(-13,1),Vector2(13,-9)]:
        _make_tree(self,Vector3(p.x,Land.height(p.x,p.y),p.y),int(p.x*100.0+p.y))
    player=get_node_or_null("Explorer")
    if not player:
        player=Walker.instantiate()
        player.name="Explorer"
        add_child(player)
    player.position = Vector3(1.0,Land.height(1.0,20.0)+2.0,20.0)
    _load_progress()
    hud = Overlay.new()
    add_child(hud)
    player.hud = hud
    _create_wisps()
    _stream_world(true)
    _make_wind()
    activities=load("res://scripts/activities.gd").new()
    add_child(activities)
    journey=load("res://scripts/journey.gd").new()
    add_child(journey)
    _restore_world_state()
    apply_settings(settings,false)

func _setup_inputs() -> void:
    var keys := {"forward":[KEY_W,KEY_UP], "back":[KEY_S,KEY_DOWN], "left":[KEY_A,KEY_LEFT], "right":[KEY_D,KEY_RIGHT], "jump":[KEY_SPACE], "sprint":[KEY_SHIFT], "home":[KEY_R], "photo":[KEY_P], "quality":[KEY_Q],"interact":[KEY_E],"menu":[KEY_ESCAPE],"map":[KEY_M],"journal":[KEY_J]}
    for action in keys:
        if not InputMap.has_action(action): InputMap.add_action(action)
        for key in keys[action]:
            var event := InputEventKey.new()
            event.physical_keycode = key
            InputMap.action_add_event(action,event)

func _setup_materials() -> void:
    ground_material = ShaderMaterial.new()
    ground_material.shader = load("res://shaders/ground.gdshader")
    ground_material.set_shader_parameter("ground_texture",load("res://assets/ground.png"))
    grass_material = ShaderMaterial.new()
    grass_material.shader = load("res://shaders/grass.gdshader")
    grass_shape = Land.grass_mesh()
    stone_material = _material(Color(0.76,0.75,0.68))
    stone_material.albedo_texture = load("res://assets/stone.png")
    stone_material.uv1_triplanar = true
    stone_material.uv1_scale = Vector3.ONE * 0.42
    wood_material = _material(Color("403429"))
    leaf_material = _material(Color.WHITE)
    leaf_material.albedo_texture = load("res://assets/leaves.png")
    leaf_material.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA_SCISSOR
    leaf_material.alpha_scissor_threshold = 0.45
    leaf_material.cull_mode = BaseMaterial3D.CULL_DISABLED
    leaf_material.vertex_color_use_as_albedo = true
    leaf_material.roughness = 0.95

func _setup_light() -> void:
    var env := Environment.new()
    env.background_mode = Environment.BG_SKY
    var sky := Sky.new()
    var sm := ShaderMaterial.new()
    sm.shader = load("res://shaders/sky.gdshader")
    sky.sky_material = sm
    sky.radiance_size = Sky.RADIANCE_SIZE_128
    env.sky = sky
    env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.ambient_light_color = Color("b4ccdf")
    env.ambient_light_energy = 0.48
    env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
    # Mobile-compatible depth fog; no volumetric fog dependency.
    env.fog_enabled = true
    env.fog_light_color = Color("c3c9b4")
    env.fog_light_energy = 0.75
    env.fog_density = 0.0028
    env.fog_sky_affect = 0.08
    var we := WorldEnvironment.new()
    we.environment = env
    add_child(we)
    var sun := DirectionalLight3D.new()
    sun.name = "EveningSun"
    sun.light_color = Color("ffda92")
    sun.light_energy = 1.55
    sun.shadow_enabled = true
    sun.directional_shadow_max_distance = 70.0
    sun.directional_shadow_mode = DirectionalLight3D.SHADOW_PARALLEL_2_SPLITS
    add_child(sun)
    sun.look_at(-Vector3(-0.48,0.45,-0.75),Vector3.UP)

func _material(color: Color) -> StandardMaterial3D:
    var mat := StandardMaterial3D.new()
    mat.albedo_color = color
    mat.roughness = 0.9
    return mat

func _setup_horizon() -> void:
    horizon = MeshInstance3D.new()
    var plane := PlaneMesh.new()
    plane.size = Vector2(1600,1600)
    plane.subdivide_width = 128
    plane.subdivide_depth = 128
    horizon.mesh = plane
    var mat := ground_material.duplicate() as ShaderMaterial
    mat.set_shader_parameter("distant_terrain",true)
    horizon.material_override = mat
    horizon.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    horizon.extra_cull_margin = 20.0
    add_child(horizon)

func _box(parent: Node3D, size: Vector3, pos: Vector3, mat: Material, solid := false) -> MeshInstance3D:
    var visual := MeshInstance3D.new()
    var mesh := BoxMesh.new()
    mesh.size = size
    visual.mesh = mesh
    visual.material_override = mat
    parent.add_child(visual)
    visual.position = pos
    if solid:
        var body := StaticBody3D.new()
        var shape := CollisionShape3D.new()
        var box := BoxShape3D.new()
        box.size = size
        shape.shape = box
        visual.add_child(body)
        body.add_child(shape)
    return visual

func _make_cottage(location := Vector3.ZERO) -> void:
    var house := Node3D.new()
    house.name = "StoneCottage"
    add_child(house)
    house.position=location
    houses.append(house)
    _box(house,Vector3(10.6,0.3,7.2),Vector3(0,0.1,0),stone_material,true)
    _box(house,Vector3(0.5,3.6,6.8),Vector3(-5,1.95,0),stone_material,true)
    _box(house,Vector3(0.5,3.6,6.8),Vector3(5,1.95,0),stone_material,true)
    _box(house,Vector3(10,3.6,0.5),Vector3(0,1.95,-3.2),stone_material,true)
    # Door and window openings in the front wall.
    for x in [-4.45,-1.65,1.65,4.45]:
        _box(house,Vector3(1.1,3.6,0.5),Vector3(x,1.95,3.2),stone_material,true)
    _box(house,Vector3(1.8,1.05,0.5),Vector3(0,3.23,3.2),stone_material,true)
    var window_mat := _material(Color("f1be69"))
    window_mat.emission_enabled = true
    window_mat.emission = Color("df9b46")
    window_mat.emission_energy_multiplier = 0.12
    for x in [-3.0,3.0]:
        _box(house,Vector3(1.75,1.05,0.5),Vector3(x,0.78,3.2),stone_material,true)
        _box(house,Vector3(1.75,0.7,0.5),Vector3(x,3.4,3.2),stone_material,true)
        _box(house,Vector3(1.45,1.6,0.07),Vector3(x,2.14,3.21),window_mat,true)
        for dx in [-0.79,0.0,0.79]:
            _box(house,Vector3(0.07,1.8,0.12),Vector3(x+dx,2.15,3.5),wood_material)
        for dy in [-0.88,0.0,0.88]:
            _box(house,Vector3(1.65,0.06,0.12),Vector3(x,2.15+dy,3.5),wood_material)
        _box(house,Vector3(1.9,0.14,0.8),Vector3(x,1.22,3.4),stone_material)
    # Each door has its own hinge, collider, sound and open/close state.
    var teal := _material(Color("315954"))
    var hinge := Node3D.new()
    hinge.set_script(load("res://scripts/door.gd"))
    house.add_child(hinge)
    hinge.position=Vector3(-0.72,1.54,3.2)
    hinge.save_id="%d_%d" % [roundi(location.x),roundi(location.z)]
    _box(hinge,Vector3(1.43,2.5,0.13),Vector3(0.715,0,0),teal,true)
    doors.append(hinge)
    _box(house,Vector3(1.8,0.08,1.0),Vector3(0,0.21,3.75),stone_material,true)
    var entry_ramp:=_box(house,Vector3(1.8,0.10,1.65),Vector3(0,0.075,5.0),stone_material,true)
    entry_ramp.rotation.x=0.155
    var slate := _material(Color("b2b1a8"))
    slate.albedo_texture = load("res://assets/slate.png")
    slate.uv1_triplanar = true
    slate.uv1_scale = Vector3.ONE * 0.35
    for side in [-1,1]:
        var roof := _box(house,Vector3(11.7,0.21,4.55),Vector3(0,4.65,side*1.95),slate,true)
        roof.rotation.x = side * 0.492
        var st := SurfaceTool.new()
        st.begin(Mesh.PRIMITIVE_TRIANGLES)
        for p in [Vector3(side*5.0,3.7,-3.2),Vector3(side*5.0,5.7,0),Vector3(side*5.0,3.7,3.2)]:
            st.set_uv(Vector2(p.z,p.y))
            st.add_vertex(p)
        st.generate_normals()
        var gable := MeshInstance3D.new()
        gable.mesh = st.commit()
        gable.material_override = stone_material
        gable.material_override.cull_mode = BaseMaterial3D.CULL_DISABLED
        house.add_child(gable)
        _box(house,Vector3(0.85,2.1,0.85),Vector3(side*3.8,5.3,-0.5),stone_material,true)
        _box(house,Vector3(1.05,0.18,1.0),Vector3(side*3.8,6.4,-0.5),slate)
    # Interior: table, benches, a rug, and a warm lamp.
    _box(house,Vector3(2.0,0.16,1.05),Vector3(2.4,1.12,0),wood_material,true)
    for x in [1.6,3.2]:
        for z in [-0.35,0.35]: _box(house,Vector3(0.12,0.9,0.12),Vector3(x,0.65,z),wood_material)
    _box(house,Vector3(2.4,0.12,0.45),Vector3(2.4,0.62,-1.0),wood_material,true)
    _box(house,Vector3(2.2,0.05,2.7),Vector3(-1.9,0.28,-0.8),_material(Color("795043")))
    var lamp := OmniLight3D.new()
    lamp.position = Vector3(1.8,2.6,0)
    lamp.light_color = Color("ffbb6a")
    lamp.light_energy = 1.4
    lamp.omni_range = 6.0
    house.add_child(lamp)
    _box(house,Vector3(0.23,0.4,0.23),Vector3(2.4,1.4,0),window_mat)
    _garden(house)
    _batch_house(house)

func _garden(house: Node3D) -> void:
    for side in [-1,1]:
        for z in [0.0,3.0,6.0]:
            _box(house,Vector3(0.12,0.9,0.12),Vector3(side*6.8,0.45,z),wood_material,true)
        for y in [0.32,0.70]:
            _box(house,Vector3(0.075,0.08,6.0),Vector3(side*6.8,y,3.0),wood_material,true)
            _box(house,Vector3(5.5,0.08,0.075),Vector3(side*4.05,y,6.0),wood_material,true)
        _box(house,Vector3(0.12,0.9,0.12),Vector3(side*1.3,0.45,6.0),wood_material,true)
    var flowers:=MultiMeshInstance3D.new()
    var mm:=MultiMesh.new()
    mm.transform_format=MultiMesh.TRANSFORM_3D
    var sphere:=SphereMesh.new()
    sphere.radius=0.07
    sphere.height=0.08
    sphere.radial_segments=7
    sphere.rings=4
    mm.mesh=sphere
    mm.instance_count=24
    for i in range(24):
        var side:float=-1 if i<12 else 1
        var x:=side*(2.0+fmod(i*0.43,3.4))
        var z:=4.5+sin(i*2.4)*0.5
        mm.set_instance_transform(i,Transform3D(Basis.IDENTITY,Vector3(x,0.38+sin(i)*0.1,z)))
    flowers.multimesh=mm
    flowers.material_override=_material(Color("d5b55b"))
    flowers.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    house.add_child(flowers)

func _make_tree(parent: Node3D, pos: Vector3, seed_value: int) -> void:
    var rng := RandomNumberGenerator.new()
    rng.seed = absi(seed_value)+617
    var tree := Node3D.new()
    parent.add_child(tree)
    tree.position = pos
    var size := rng.randf_range(0.8,1.45)
    tree.scale = Vector3.ONE * size
    var trunk := MeshInstance3D.new()
    var cylinder := CylinderMesh.new()
    cylinder.top_radius = 0.18
    cylinder.bottom_radius = 0.48
    cylinder.height = 4.2
    cylinder.radial_segments = 7
    trunk.mesh = cylinder
    trunk.material_override = wood_material
    trunk.position.y = 2.1
    tree.add_child(trunk)
    var leaves := MultiMeshInstance3D.new()
    var mm := MultiMesh.new()
    mm.transform_format = MultiMesh.TRANSFORM_3D
    mm.use_colors = true
    var card := QuadMesh.new()
    card.size = Vector2(2.2,2.2)
    mm.mesh = card
    mm.instance_count = 85
    for i in range(85):
        var radial := Vector3(rng.randf_range(-1.0,1.0),rng.randf_range(-0.8,0.8),rng.randf_range(-1.0,1.0)).normalized()
        var p := Vector3(radial.x*2.7,4.7+radial.y*2.1,radial.z*2.5)*rng.randf_range(0.35,1.0)
        p.y += 1.0
        var s := rng.randf_range(0.8,1.55)
        var facing := Basis.looking_at(radial,Vector3.UP).scaled(Vector3.ONE*s)
        mm.set_instance_transform(i,Transform3D(facing,p))
        mm.set_instance_color(i,Color(rng.randf_range(0.7,1.2),rng.randf_range(0.8,1.2),rng.randf_range(0.6,0.9)))
    leaves.multimesh = mm
    leaves.material_override = leaf_material
    tree.add_child(leaves)

func _stream_world(initial := false) -> void:
    var next := Vector2i(floori(player.position.x/Land.SIZE),floori(player.position.z/Land.SIZE))
    if center == next and not initial: return
    center = next
    horizon.position = Vector3(center.x*Land.SIZE,0,center.y*Land.SIZE)
    for c in terrain.keys():
        if absi(c.x-center.x)>3 or absi(c.y-center.y)>3:
            terrain[c].queue_free()
            terrain.erase(c)
    for c in grass.keys():
        if absi(c.x-center.x)>1 or absi(c.y-center.y)>1:
            grass[c].queue_free()
            grass.erase(c)
    pending.clear()
    for z in range(-3,4):
        for x in range(-3,4):
            var c := center+Vector2i(x,z)
            if not terrain.has(c) or (absi(x)<=1 and absi(z)<=1 and not grass.has(c)):
                pending.append(c)
    pending.sort_custom(func(a: Vector2i,b: Vector2i): return (a-center).length_squared()<(b-center).length_squared())
    # Physics coverage around the spawn is prepared immediately.
    if initial:
        for i in range(mini(9,pending.size())): _build_tile(pending.pop_front())

func _build_tile(c: Vector2i) -> void:
    if not terrain.has(c):
        var tile := Node3D.new()
        tile.name = "Tile_%s_%s" % [c.x,c.y]
        add_child(tile)
        var mesh := MeshInstance3D.new()
        mesh.mesh = Land.terrain_mesh(c)
        mesh.material_override = ground_material
        tile.add_child(mesh)
        # Triangle collider matches the rendered terrain, including slopes.
        mesh.create_trimesh_collision()
        terrain[c] = tile
        var rng := RandomNumberGenerator.new()
        rng.seed = absi(c.x*73856093 ^ c.y*19349663)+32
        if rng.randf()>0.25:
            var x := (c.x+rng.randf())*Land.SIZE
            var z := (c.y+rng.randf())*Land.SIZE
            if Vector2(x,z).length()>29.0 and x<126.0: _make_tree(tile,Vector3(x,Land.height(x,z),z),rng.randi())
    if absi(c.x-center.x)<=1 and absi(c.y-center.y)<=1 and not grass.has(c): _build_grass(c)

func _build_grass(c: Vector2i) -> void:
    var rng := RandomNumberGenerator.new()
    rng.seed = absi(c.x*83492791 ^ c.y*2971215073)+84
    var mm := MultiMesh.new()
    mm.transform_format = MultiMesh.TRANSFORM_3D
    mm.mesh = grass_shape
    mm.instance_count = grass_count
    var heights:=Land.height_grid(c)
    for i in range(grass_count):
        var x := (c.x+rng.randf())*Land.SIZE
        var z := (c.y+rng.randf())*Land.SIZE
        var s := rng.randf_range(0.65,1.45)
        if not Land.grass_allowed(x,z): s=0.0
        var basis := Basis(Vector3.UP,rng.randf()*TAU).scaled(Vector3(s,s,s))
        mm.set_instance_transform(i,Transform3D(basis,Vector3(x,Land.sample_grid(heights,c,x,z)-0.03,z)))
    var node := MultiMeshInstance3D.new()
    node.multimesh = mm
    node.material_override = grass_material
    node.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    add_child(node)
    grass[c] = node

func _create_wisps() -> void:
    var glow := _material(Color("f9d77e"))
    glow.emission_enabled = true
    glow.emission = Color("ffe9a3")
    glow.emission_energy_multiplier = 2.0
    for i in range(locations.size()):
        var p: Vector2 = locations[i]
        var wisp := Node3D.new()
        add_child(wisp)
        wisp.position = Vector3(p.x,Land.height(p.x,p.y),p.y)
        var orb := MeshInstance3D.new()
        var sphere := SphereMesh.new()
        sphere.radius = 0.19
        sphere.height = 0.38
        orb.mesh = sphere
        orb.material_override = glow
        orb.position.y = 1.7
        wisp.add_child(orb)
        var ring := MeshInstance3D.new()
        var torus := TorusMesh.new()
        torus.inner_radius = 0.65
        torus.outer_radius = 0.70
        ring.mesh = torus
        ring.material_override = glow
        ring.position.y = 0.15
        wisp.add_child(ring)
        for angle in range(5):
            var a := angle*TAU/5.0
            var rock := _box(wisp,Vector3(0.4,0.7,0.3),Vector3(cos(a)*1.2,0.3,sin(a)*1.2),stone_material,true)
            rock.rotation = Vector3(0.1,a,0.1)
        wisps.append(wisp)
        if collected.has(str(i)): wisp.visible = false

func _process(delta: float) -> void:
    time += delta
    save_clock += delta
    _stream_world()
    if not navigation_name.is_empty() and player.position.distance_to(navigation_target)<3.0:
        hud.announce("وصلت · "+navigation_name)
        navigation_name=""
    for house in houses:house.visible=player.position.distance_to(house.position)<195
    if not pending.is_empty(): _build_tile(pending.pop_front())
    for i in range(wisps.size()):
        if collected.has(str(i)): continue
        var wisp: Node3D = wisps[i]
        wisp.get_child(0).position.y = 1.65+sin(time*1.8+i)*0.24
        wisp.get_child(1).rotation.y += delta*0.4
        if player.position.distance_to(wisp.position)<2.0:
            collected[str(i)] = true
            wisp.visible = false
            hud.announce("شعلة جديدة  ·  %d / 6" % collected.size())
            if activities:activities.play_ui_sound()
            _save_progress()
            if collected.size()==6: hud.announce("اكتملت رحلة النور — العالم لك، واصل الاستكشاف")
    if save_clock>20.0:
        save_clock=0.0
        _save_progress()
    if Input.is_action_just_pressed("home"): return_home()
    if Input.is_action_just_pressed("quality"): hud.cycle_quality()
    if Input.is_action_just_pressed("photo"): hud.toggle_photo()
    if Input.is_action_just_pressed("interact") and activities: activities.interact()

func nearest_wisp() -> Vector3:
    var best := Vector3.ZERO
    var dist := INF
    for i in range(wisps.size()):
        if collected.has(str(i)): continue
        var d := player.position.distance_to(wisps[i].position)
        if d<dist:
            dist=d
            best=wisps[i].position
    return best

func set_quality(level: int) -> void:
    grass_count = [1000,3000,5200][level]
    get_node("EveningSun").shadow_enabled = level>0
    for c in grass.keys(): grass[c].queue_free()
    grass.clear()
    _stream_world(true)

func return_home() -> void:
    player.position = Vector3(1.0,Land.height(1.0,20.0)+1.0,20.0)
    player.velocity = Vector3.ZERO
    player.yaw = 0.0
    player.pitch = -0.18
    _stream_world(true)
    hud.announce("عدت إلى البيت الحجري")
    _save_progress()

func _save_progress() -> void:
    if not is_instance_valid(player):return
    var data:=saved_data.duplicate(true)
    data.merge({"version":2,"position":[player.position.x,player.position.y,player.position.z],"collected":collected,"yaw":player.yaw,"pitch":player.pitch},true)
    var door_states:={}
    for door in doors:door_states[door.save_id]=door.opened
    data["doors"]=door_states
    if journey:
        data["journey"]=journey.completed
        data["visited_houses"]=journey.visited_houses
    if activities:
        var companions:={}
        for person in activities.people:
            companions[person.person_name]={"following":person.following,"position":[person.position.x,person.position.y,person.position.z]}
        data["companions"]=companions
    var error:=Profile.write(data)
    if error==OK:saved_data=data
    elif hud:hud.announce("تعذر الحفظ المحلي؛ ملف الحفظ السابق محفوظ")

func _load_progress() -> void:
    if "--fresh" in OS.get_cmdline_user_args():return
    saved_data=Profile.read()
    if saved_data.is_empty():return
    collected=saved_data.get("collected",{})
    var p: Array=saved_data.position
    player.position=Vector3(float(p[0]),maxf(float(p[1]),Land.height(float(p[0]),float(p[2]))+0.03),float(p[2]))
    player.yaw=float(saved_data.get("yaw",0.0))
    player.pitch=clampf(float(saved_data.get("pitch",-0.18)),-0.75,0.65)

func _restore_world_state() -> void:
    var values=saved_data.get("doors",{})
    if values is Dictionary:
        for door in doors:door.set_opened(bool(values.get(door.save_id,false)),true)
    var progress=saved_data.get("journey",{})
    if progress is Dictionary:journey.completed=progress
    var visited=saved_data.get("visited_houses",[])
    if visited is Array:journey.visited_houses=visited
    var companions=saved_data.get("companions",{})
    if companions is Dictionary:
        for person in activities.people:
            var item=companions.get(person.person_name,{})
            if item is Dictionary:
                person.following=bool(item.get("following",false))
                var p=item.get("position",[])
                if p is Array and p.size()==3:
                    var location:=Vector3(float(p[0]),float(p[1]),float(p[2]))
                    if location.is_finite():person.position=location
    if collected.size()==6:journey.completed["lights"]=true

func apply_settings(values: Dictionary,persist:=true) -> void:
    var previous:=int(settings.get("quality",1))
    settings=values.duplicate()
    if OS.has_feature("android"):Engine.max_fps=30 if int(settings.quality)==0 else 60
    if previous!=int(settings.quality) or grass_count!=[1000,3000,5200][int(settings.quality)]:set_quality(int(settings.quality))
    if hud:
        hud.quality=int(settings.quality)
        hud.quality_button.text="جودة: "+["منخفضة","متوسطة","عالية"][hud.quality]
    for pair in [["Master","master"],["Music","music"],["Ambience","ambience"],["Effects","effects"]]:
        var index:=AudioServer.get_bus_index(pair[0])
        if index>=0:
            var volume:=float(settings.get(pair[1],0.8))
            AudioServer.set_bus_volume_db(index,linear_to_db(maxf(0.001,volume)))
            AudioServer.set_bus_mute(index,volume<=0 or (pair[0]=="Master" and not wind_enabled))
    if persist:Profile.save_settings(settings)

func set_navigation(label: String,target: Vector3) -> void:
    navigation_name=label
    navigation_target=target
    hud.announce("الوجهة · "+label)

func _batch_house(house: Node3D) -> void:
    # Batch static geometry by material while preserving independent door colliders.
    var batches:={}
    for child in house.get_children():
        if not child is MeshInstance3D:continue
        var material: Material=child.material_override
        if not material:continue
        if not batches.has(material):
            var surface:=SurfaceTool.new()
            surface.begin(Mesh.PRIMITIVE_TRIANGLES)
            batches[material]=surface
        batches[material].append_from(child.mesh,0,child.transform)
        for body in child.get_children():
            if body is StaticBody3D:body.reparent(house,true)
        child.queue_free()
    for material in batches:
        var mesh:=MeshInstance3D.new()
        mesh.mesh=batches[material].commit()
        mesh.material_override=material
        mesh.visibility_range_end=180
        house.add_child(mesh)

func _make_wind() -> void:
    # Original, synthesized ambience. No microphone or network permission.
    var rng := RandomNumberGenerator.new()
    rng.seed = 731
    var rate := 22050
    var data := PackedByteArray()
    data.resize(rate*6*2)
    var smooth := 0.0
    for i in range(rate*6):
        smooth = smooth*0.97+rng.randf_range(-1.0,1.0)*0.03
        var envelope := 0.6+0.4*sin(float(i)/rate*TAU/6.0)
        data.encode_s16(i*2,int(smooth*envelope*7500.0))
    var stream := AudioStreamWAV.new()
    stream.format = AudioStreamWAV.FORMAT_16_BITS
    stream.mix_rate = rate
    stream.data = data
    stream.loop_mode = AudioStreamWAV.LOOP_FORWARD
    stream.loop_end = rate*6
    wind_player = AudioStreamPlayer.new()
    wind_player.stream = stream
    wind_player.volume_db = -18.0
    wind_player.bus="Ambience" if AudioServer.get_bus_index("Ambience")>=0 else "Master"
    add_child(wind_player)
    wind_player.play()

func toggle_sound() -> void:
    wind_enabled = not wind_enabled
    AudioServer.set_bus_mute(AudioServer.get_bus_index("Master"),not wind_enabled)

func go_beach() -> void:
    player.position=Vector3(148,Land.height(148,0)+1.0,0)
    player.velocity=Vector3.ZERO
    player.yaw=-PI/2
    _stream_world(true)
    hud.announce("شاطئ النور — اقترب من الشخصيات واضغط تفاعل")
    _save_progress()

func _notification(what: int) -> void:
    if what == NOTIFICATION_WM_CLOSE_REQUEST or what == NOTIFICATION_APPLICATION_PAUSED:
        if is_instance_valid(player): _save_progress()
