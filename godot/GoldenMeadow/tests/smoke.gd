extends SceneTree

var errors: Array[String] = []
var world: Node3D

func _initialize() -> void:
    call_deferred("run")

func check(ok: bool, text: String) -> void:
    print(("PASS " if ok else "FAIL ")+text)
    if not ok: errors.append(text)

func frames(count: int) -> void:
    for i in range(count): await physics_frame

func run() -> void:
    root.size=Vector2i(1280,720)
    world=load("res://scenes/main.tscn").instantiate()
    root.add_child(world)
    await frames(100)
    var p: CharacterBody3D=world.player
    check(p.is_on_floor(),"Player settles on terrain")
    check(world.terrain.size()==49,"49 streamed terrain chunks are present")
    check(world.grass.size()==9,"Nine nearby grass chunks are present")
    var start: Vector3=p.position
    Input.action_press("right")
    await frames(60)
    Input.action_release("right")
    check(p.position.x>start.x+3.0,"Walking moves the character through the meadow")
    await frames(20)
    var y: float=p.position.y
    Input.action_press("jump")
    await frames(10)
    Input.action_release("jump")
    check(p.position.y>y+0.4,"Jump rises above the terrain")
    await frames(80)
    check(p.is_on_floor(),"Jump lands on terrain")
    # Use real input dispatch: joystick and independent camera touch.
    var touch := InputEventScreenTouch.new()
    touch.index=0
    touch.position=Vector2(120,600)
    touch.pressed=true
    Input.parse_input_event(touch)
    await frames(2)
    var drag := InputEventScreenDrag.new()
    drag.index=0
    drag.position=Vector2(175,570)
    drag.relative=Vector2(55,-30)
    Input.parse_input_event(drag)
    await frames(2)
    check(world.hud.move_vector.length()>0.7,"Touch joystick receives drag input")
    var look_touch := InputEventScreenTouch.new()
    look_touch.index=1
    look_touch.position=Vector2(900,400)
    look_touch.pressed=true
    Input.parse_input_event(look_touch)
    await frames(2)
    var yaw: float=p.yaw
    var look_drag := InputEventScreenDrag.new()
    look_drag.index=1
    look_drag.position=Vector2(950,400)
    look_drag.relative=Vector2(50,0)
    Input.parse_input_event(look_drag)
    await frames(2)
    check(absf(p.yaw-yaw)>0.1 and world.hud.move_vector.length()>0.7,"Multitouch camera works while walking")
    touch.pressed=false
    look_touch.pressed=false
    Input.parse_input_event(touch)
    Input.parse_input_event(look_touch)
    await frames(2)
    check(world.hud.move_vector==Vector2.ZERO,"Releasing joystick stops movement")
    # Check the UI with a touch event at the actual jump button position.
    await frames(45)
    var before_jump: float=p.position.y
    var jump_touch := InputEventScreenTouch.new()
    jump_touch.index=0
    jump_touch.position=Vector2(1210,635)
    jump_touch.pressed=true
    Input.parse_input_event(jump_touch)
    await frames(1)
    jump_touch.pressed=false
    Input.parse_input_event(jump_touch)
    await frames(10)
    check(p.position.y>before_jump+0.3,"On-screen jump button responds to touch")
    await frames(80)
    p.position=Vector3(117,world.Land.height(117,61)+2.0,61)
    p.velocity=Vector3.ZERO
    await frames(100)
    check(p.is_on_floor() and p.position.y>-10.0,"Exploration loads solid terrain beyond the starting area")
    check(world.terrain.size()==49 and world.grass.size()==9,"Streaming unloads old chunks to bound memory")
    world.set_quality(0)
    await frames(20)
    check(world.grass_count==1000 and world.grass.size()==9,"Low quality rebuilds grass successfully")
    world.set_quality(1)
    await frames(20)
    check(world.grass_count==3000,"Medium quality restores grass")
    world.return_home()
    await frames(100)
    check(p.is_on_floor() and absf(p.position.z-20)<0.5,"Return home loads terrain and resets location")
    # Walk into the open front door from immediately in front of it.
    p.position=Vector3(0,0.6,4.6)
    p.velocity=Vector3.ZERO
    p.yaw=0.0
    check(world.doors.size()==9,"Nine cottages have interactive doors")
    world.activities.interact()
    await frames(35)
    check(world.doors[0].opened,"Interaction opens the nearby cottage door")
    Input.action_press("forward")
    await frames(65)
    Input.action_release("forward")
    print("Doorway final position: ",p.position)
    check(p.position.z<2.0 and p.position.z> -3.0,"The cottage doorway is passable")
    await frames(20)
    world.go_beach()
    await frames(100)
    check(p.is_on_floor(),"Beach teleport loads solid coastal terrain")
    check(world.activities.people.size()==3,"Three adult beach companions are present")
    var person=world.activities.people[1]
    p.position=person.position+Vector3(-1.5,0.5,0)
    p.velocity=Vector3.ZERO
    await frames(30)
    world.activities.interact()
    check(person.following,"Interaction starts beach companionship")
    var old_position: Vector3=person.position
    p.yaw=0.0
    Input.action_press("forward")
    await frames(100)
    Input.action_release("forward")
    check(person.position.distance_to(old_position)>1.0,"Companion follows the walking player")
    await frames(60)
    world.activities.interact()
    check(not person.following,"Interaction ends companionship")
    var bird_position: Vector3=world.activities.birds[0].position
    await frames(10)
    check(bird_position.distance_to(world.activities.birds[0].position)>0.05,"Bird flock moves through the world")
    check(world.activities.ocean_audio.playing,"Ocean ambience is running")
    for w in world.wisps:
        p.position=w.position+Vector3(0,0.8,0)
        p.velocity=Vector3.ZERO
        await frames(4)
    check(world.collected.size()==6,"All six light collectibles can be gathered")
    world._save_progress()
    var saved = JSON.parse_string(FileAccess.get_file_as_string("user://journey.json"))
    check(saved is Dictionary and saved.collected.size()==6,"Progress saves to a valid local save file")
    world.collected.clear()
    # --fresh bypasses loading at startup only; temporarily use a reloaded scene in a separate run for restore testing.
    check(FileAccess.file_exists("user://journey.json"),"Save file remains available for the next session")
    world.queue_free()
    await frames(3)
    print("SMOKE RESULT: %d failures" % errors.size())
    call_deferred("quit",0 if errors.is_empty() else 1)
