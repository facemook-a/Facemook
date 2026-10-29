extends SceneTree
var failures: Array[String]=[]
var world: Node3D
const Store=preload("res://scripts/profile_store.gd")
func _initialize() -> void:call_deferred("run")
func check(value: bool,label: String) -> void:
    print(("PASS " if value else "FAIL ")+label)
    if not value:failures.append(label)
func frames(count: int) -> void:
    for i in range(count):await physics_frame
func ui_frames(count: int) -> void:
    for i in range(count):await process_frame
func run() -> void:
    root.size=Vector2i(1280,720)
    var file:=FileAccess.open(Store.PATH,FileAccess.WRITE)
    file.store_string(JSON.stringify({"position":[1,0.5,20],"collected":{"0":true,"3":true},"legacy_note":"preserve-me"}))
    file.close()
    world=load("res://scenes/main.tscn").instantiate()
    root.add_child(world)
    await frames(100)
    check(world.collected.size()==2,"Legacy collectible progress migrates without resetting")
    check(world.player.animation_controller.state=="Idle","AnimationTree starts in idle state")
    var map_key:=InputEventKey.new()
    map_key.physical_keycode=KEY_M
    map_key.pressed=true
    Input.parse_input_event(map_key)
    await ui_frames(3)
    check(paused and world.hud.menus.page=="map","Desktop map shortcut works through real input dispatch")
    map_key.pressed=false
    Input.parse_input_event(map_key)
    var escape:=InputEventKey.new()
    escape.physical_keycode=KEY_ESCAPE
    escape.pressed=true
    Input.parse_input_event(escape)
    await ui_frames(3)
    check(not paused,"Escape closes the paused map and resumes gameplay")
    escape.pressed=false
    Input.parse_input_event(escape)
    Input.action_press("right")
    await frames(25)
    check(world.player.animation_controller.state=="Walk","AnimationTree transitions to walking")
    Input.action_press("sprint")
    await frames(20)
    check(world.player.animation_controller.state=="Run","AnimationTree transitions to running")
    Input.action_release("sprint")
    Input.action_release("right")
    await frames(40)
    Input.action_press("jump")
    await frames(8)
    Input.action_release("jump")
    check(world.player.animation_controller.state=="Jump","AnimationTree transitions to airborne pose")
    await frames(100)
    var menus=world.hud.menus
    menus.open("pause")
    var p:Vector3=world.player.position
    Input.action_press("right")
    await ui_frames(15)
    check(paused and world.player.position.is_equal_approx(p),"Pause freezes gameplay while UI remains active")
    Input.action_release("right")
    menus.open("map")
    await ui_frames(3)
    menus.map_view.select_at(menus.map_view.project(Vector2(148,0)))
    check(world.navigation_name=="شاطئ النور","Map selection creates an actual tracked destination")
    menus.open("settings")
    await ui_frames(4)
    var slider:HSlider=menus.sliders[0]
    var touch:=InputEventScreenTouch.new()
    touch.index=4
    touch.position=slider.global_position+Vector2(slider.size.x*.25,slider.size.y*.5)
    touch.pressed=true
    Input.parse_input_event(touch)
    await ui_frames(3)
    check(absf(world.settings.master-.25)<.04,"Touch controls the master-volume slider")
    var drag:=InputEventScreenDrag.new()
    drag.index=4
    drag.position=slider.global_position+Vector2(slider.size.x*.75,slider.size.y*.5)
    Input.parse_input_event(drag)
    await ui_frames(3)
    touch.pressed=false
    Input.parse_input_event(touch)
    check(absf(world.settings.master-.75)<.04,"Touch dragging updates a menu slider")
    menus.close()
    check(not paused,"Resume restores gameplay")
    check(absf(Store.settings().master-.75)<.04,"Audio settings persist across menu sessions")
    var muted:Dictionary=world.settings.duplicate()
    muted.master=0.0
    world.apply_settings(muted)
    check(AudioServer.is_bus_mute(AudioServer.get_bus_index("Master")),"Zero master volume actually mutes the audio bus")
    muted.master=0.75
    world.apply_settings(muted)
    world.return_home()
    await frames(80)
    world.doors[0].set_opened(true,true)
    world.go_beach()
    await frames(80)
    var person=world.activities.people[1]
    world.player.position=person.position+Vector3(-1.5,.5,0)
    await frames(25)
    world.activities.interact()
    check(person.following,"Existing companion interaction is preserved")
    await frames(30)
    check(world.journey.completed.get("coast",false),"Visiting the beach completes its journal objective")
    var beach_position:Vector3=world.player.position
    world.player.position=Vector3(180,-3,0)
    world.player.velocity=Vector3.ZERO
    world._stream_world(true)
    await frames(30)
    check(world.player.animation_controller.state=="Swim" and world.player.position.y> -3.8,"Water movement stays afloat and selects the swimming animation")
    world.player.position=beach_position
    world.player.velocity=Vector3.ZERO
    world._stream_world(true)
    await frames(20)
    world._save_progress()
    var saved:=Store.read()
    check(saved.get("version",0)==2 and saved.get("legacy_note","")=="preserve-me","Versioned save preserves unknown legacy fields")
    check(saved.doors.get("0_0",false),"Door state is written to the save")
    check(saved.companions.get(person.person_name,{}).get("following",false),"Companion state is written to the save")
    var count:=0
    for child in world.houses[0].get_children():
        if child is MeshInstance3D:count+=1
    check(count<=8,"Cottage visuals are batched into eight or fewer material meshes")
    world._save_progress()
    world.queue_free()
    await frames(4)
    world=load("res://scenes/main.tscn").instantiate()
    root.add_child(world)
    await frames(100)
    check(world.collected.size()==2 and world.doors[0].opened,"Collectibles and doors restore in a new scene")
    check(world.activities.people[1].following,"Companionship restores in a new scene")
    check(world.journey.completed.get("coast",false),"Journal progress restores in a new scene")
    world._save_progress()
    world._save_progress()
    file=FileAccess.open(Store.PATH,FileAccess.WRITE)
    file.store_string("{broken-save")
    file.close()
    check(Store.read().get("collected",{}).size()==2,"A corrupt save recovers from its verified backup")
    world.queue_free()
    await frames(4)
    print("POLISH RESULT: %d failures" % failures.size())
    call_deferred("quit",0 if failures.is_empty() else 1)
