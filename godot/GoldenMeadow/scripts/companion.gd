extends CharacterBody3D
var following := false
var person_name := "ليلى"
var model: Node3D
var time := 0.0
func _ready() -> void:
    var shape:=CollisionShape3D.new()
    var capsule:=CapsuleShape3D.new()
    capsule.radius=0.28
    capsule.height=1.75
    shape.shape=capsule
    shape.position.y=0.875
    add_child(shape)
    model=load("res://assets/character/Companion.glb").instantiate()
    add_child(model)
    var label:=Label3D.new()
    label.text=person_name
    label.font=load("res://assets/Arabic.ttf")
    label.font_size=36
    label.pixel_size=0.005
    label.position.y=2.03
    label.billboard=BaseMaterial3D.BILLBOARD_ENABLED
    add_child(label)
func interact() -> void:
    following=not following
    var world:=get_parent().get_parent()
    world.hud.announce(person_name+ (": نمشيو نكتاشفو المكان مع بعضنا." if following else ": غادي نبقى هنا، نلقاك من بعد."))
    get_parent().play_ui_sound()
    world._save_progress()
func _physics_process(delta: float) -> void:
    time+=delta
    var world:=get_parent().get_parent()
    if not following and position.distance_to(world.player.position)>65.0:
        position.y=world.Land.height(position.x,position.z)
        velocity=Vector3.ZERO
        return
    var direction: Vector3=world.player.position-position
    direction.y=0
    var speed:=3.8 if following and direction.length()>2.2 else 0.0
    if direction.length()>40.0 and following and (world.player.camera.is_position_behind(global_position) or direction.length()>100):
        position=world.player.position+Vector3(-2,1,1)
    if speed>0.0:
        direction=_steer(direction.normalized())
    velocity.x=move_toward(velocity.x,direction.normalized().x*speed,delta*8)
    velocity.z=move_toward(velocity.z,direction.normalized().z*speed,delta*8)
    if not is_on_floor():velocity.y-=18*delta
    move_and_slide()
    if position.x>158.0:position.x=158.0
    if speed>0.1:
        model.rotation.y=lerp_angle(model.rotation.y,atan2(direction.x,direction.z),delta*5)
        model.position.y=sin(time*9)*0.025
    else:model.position.y=0.0

func _steer(direction: Vector3) -> Vector3:
    var world=get_parent().get_parent()
    var start:=global_position+Vector3.UP*0.8
    var query:=PhysicsRayQueryParameters3D.create(start,start+direction*1.2)
    query.exclude=[get_rid(),world.player.get_rid()]
    var space:=get_world_3d().direct_space_state
    if space.intersect_ray(query).is_empty():return direction
    for sign in [1.0,-1.0]:
        var side:=direction.rotated(Vector3.UP,sign*1.0)
        query.to=start+side*1.4
        if space.intersect_ray(query).is_empty():return side
    return Vector3.ZERO
