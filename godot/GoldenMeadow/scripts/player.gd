extends CharacterBody3D

var hud: CanvasLayer
var yaw := 0.0
var pitch := -0.18
var pivot: Node3D
var camera: Camera3D
var visual: Node3D
var left_leg: Node3D
var right_leg: Node3D
var left_arm: Node3D
var right_arm: Node3D
var stride := 0.0
var sprinting := false
var animation_controller: Node
var stamina:=100.0
var exhausted:=false

func _ready() -> void:
    floor_snap_length = 0.5
    floor_max_angle = deg_to_rad(50.0)
    if not has_node("GroundCollider"):
        var collision:=CollisionShape3D.new()
        var shape:=CapsuleShape3D.new()
        shape.radius=0.32
        shape.height=1.8
        collision.name="GroundCollider"
        collision.shape=shape
        collision.position.y=0.9
        add_child(collision)
    visual=get_node_or_null("Visual")
    if not visual:
        visual=Node3D.new()
        visual.name="Visual"
        add_child(visual)
        _build_character()
    animation_controller=load("res://scripts/avatar_animation.gd").new()
    add_child(animation_controller)
    pivot=get_node_or_null("CameraPivot")
    if not pivot:
        pivot=Node3D.new()
        pivot.name="CameraPivot"
        pivot.position.y=1.20
        add_child(pivot)
        var spring:=SpringArm3D.new()
        spring.name="SpringArm"
        spring.spring_length=3.2
        spring.margin=0.2
        pivot.add_child(spring)
        var view:=Camera3D.new()
        view.name="Camera"
        view.current=true
        view.fov=68
        view.near=0.12
        view.far=210
        spring.add_child(view)
    var arm:=pivot.get_node("SpringArm") as SpringArm3D
    arm.add_excluded_object(get_rid())
    camera=arm.get_node("Camera")

func _mat(color: String) -> StandardMaterial3D:
    var m := StandardMaterial3D.new()
    m.albedo_color = Color(color)
    m.roughness = 0.9
    return m

func _capsule(parent: Node3D, pos: Vector3, radius: float, height: float, material: Material) -> MeshInstance3D:
    var node := MeshInstance3D.new()
    var mesh := CapsuleMesh.new()
    mesh.radius = radius
    mesh.height = height
    mesh.radial_segments = 12
    mesh.rings = 4
    node.mesh = mesh
    node.material_override = material
    parent.add_child(node)
    node.position = pos
    return node

func _build_character() -> void:
    var coat := _mat("171d21")
    var trousers := _mat("252b30")
    var leather := _mat("101518")
    var scarf := _mat("263b34")
    var skin := _mat("b99675")
    _capsule(visual,Vector3(0,1.12,0),0.27,0.73,coat)
    _capsule(visual,Vector3(0,1.49,0),0.065,0.15,skin)
    var head_scene = load("res://assets/character/B5_Head.glb")
    if head_scene is PackedScene:
        var head = head_scene.instantiate()
        head.rotation.y=PI
        visual.add_child(head)
    _capsule(visual,Vector3(0,1.44,0),0.22,0.15,scarf)
    _capsule(visual,Vector3(0,1.12,0.27),0.22,0.57,leather)
    _add_b5_details(coat)
    for side in [-1,1]:
        var leg := Node3D.new()
        leg.name="LeftLeg" if side==-1 else "RightLeg"
        visual.add_child(leg)
        leg.position = Vector3(side*0.135,0.79,0)
        _capsule(leg,Vector3(0,-0.30,0),0.105,0.65,trousers)
        _capsule(leg,Vector3(0,-0.66,-0.06),0.11,0.24,leather)
        var arm := Node3D.new()
        arm.name="LeftArm" if side==-1 else "RightArm"
        visual.add_child(arm)
        arm.position = Vector3(side*0.3,1.36,0)
        _capsule(arm,Vector3(side*0.015,-0.22,0),0.085,0.53,coat)
        _capsule(arm,Vector3(side*0.015,-0.5,0),0.065,0.15,skin)
        if side == -1:
            left_leg=leg
            left_arm=arm
        else:
            right_leg=leg
            right_arm=arm

func _add_b5_details(material: Material) -> void:
    # The existing body, limb pivots, movement and animations remain in use.
    for side in [-1,1]:
        var pocket := MeshInstance3D.new()
        var box := BoxMesh.new()
        box.size=Vector3(0.105,0.13,0.035)
        pocket.mesh=box
        pocket.material_override=material
        pocket.position=Vector3(side*0.13,1.04,-0.235)
        visual.add_child(pocket)
        var trim := MeshInstance3D.new()
        var strip := BoxMesh.new()
        strip.size=Vector3(0.006,0.13,0.008)
        trim.mesh=strip
        trim.material_override=_mat("41b883")
        trim.position=Vector3(side*0.17,1.05,-0.26)
        visual.add_child(trim)
    for back in [false,true]:
        var logo := Label3D.new()
        logo.text="B5"
        logo.font=load("res://assets/Arabic.ttf")
        logo.font_size=128
        logo.pixel_size=0.001
        logo.modulate=Color("e6f4eb")
        logo.outline_size=0
        logo.shaded=true
        logo.position=Vector3(0,1.25,0.48 if back else -0.26)
        if not back: logo.rotation.y=PI
        visual.add_child(logo)

func _unhandled_input(event: InputEvent) -> void:
    if event is InputEventMouseButton and event.button_index==MOUSE_BUTTON_RIGHT:
        Input.mouse_mode = Input.MOUSE_MODE_CAPTURED if event.pressed else Input.MOUSE_MODE_VISIBLE
    if event is InputEventMouseMotion and Input.mouse_mode==Input.MOUSE_MODE_CAPTURED:
        look(event.relative*0.003*float(get_parent().settings.get("sensitivity",1.0)))
    

func look(amount: Vector2) -> void:
    yaw -= amount.x
    pitch = clampf(pitch-amount.y,-0.75,0.65)

func _physics_process(delta: float) -> void:
    var movement := Input.get_vector("left","right","forward","back")
    if hud and hud.move_vector.length()>0.0: movement=hud.move_vector
    sprinting = Input.is_action_pressed("sprint") or (hud and hud.sprinting)
    if stamina<=1.0:exhausted=true
    elif stamina>=25.0:exhausted=false
    if exhausted:sprinting=false
    stamina=clampf(stamina+(-9.0 if sprinting and movement.length()>0.1 else 14.0)*delta,0,100)
    var speed := 7.2 if sprinting else 4.1
    var direction := Basis(Vector3.UP,yaw)*Vector3(movement.x,0,movement.y)
    velocity.x=move_toward(velocity.x,direction.x*speed,delta*24.0)
    velocity.z=move_toward(velocity.z,direction.z*speed,delta*24.0)
    var swimming:=position.x>160.0 and position.y< -2.0
    if swimming:
        velocity.y=clampf((-3.0-position.y)*4.0,-2.5,2.5)
    elif not is_on_floor(): velocity.y-=18.0*delta
    if (Input.is_action_just_pressed("jump") or (hud and hud.jump_requested)) and is_on_floor(): velocity.y=6.4
    if hud: hud.jump_requested=false
    move_and_slide()
    if position.y < -50.0: get_parent().return_home()
    pivot.rotation=Vector3(pitch,yaw,0)
    var walking := Vector2(velocity.x,velocity.z).length()
    if walking>0.1:
        visual.rotation.y=lerp_angle(visual.rotation.y,atan2(-direction.x,-direction.z),minf(delta*12.0,1.0))
        stride+=delta*walking*2.4
    animation_controller.set_motion(walking,is_on_floor(),swimming)
    visual.position.y=lerpf(visual.position.y,absf(sin(stride))*minf(walking/4.0,1.4)*0.025,minf(1,delta*12))
