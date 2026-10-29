extends SceneTree

func _initialize() -> void:
    call_deferred("run")

func run() -> void:
    var world = load("res://scenes/main.tscn").instantiate()
    root.add_child(world)
    for i in range(70): await physics_frame
    var ok: bool=world.collected.size()==6 and world.player.is_on_floor()
    for w in world.wisps: ok=ok and not w.visible
    print(("PASS" if ok else "FAIL")+" Saved collection and location are restored in a new game process")
    world.queue_free()
    for i in range(3): await process_frame
    call_deferred("quit",0 if ok else 1)
