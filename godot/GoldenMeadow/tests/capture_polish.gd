extends SceneTree
func _initialize() -> void:call_deferred("capture")
func frames(count: int) -> void:
    for i in range(count):await process_frame
func picture(name: String) -> void:
    await RenderingServer.frame_post_draw
    root.get_texture().get_image().save_png("user://"+name+".png")
func capture() -> void:
    var world=load("res://scenes/main.tscn").instantiate()
    root.add_child(world)
    world.set_quality(0)
    await frames(65)
    await picture("GoldenMeadow_Village")
    world.hud.menus.open("map")
    await frames(4)
    await picture("GoldenMeadow_Map")
    world.hud.menus.open("settings")
    await frames(4)
    await picture("GoldenMeadow_Settings")
    world.hud.menus.close()
    world.go_beach()
    world.player.visual.rotation.y=-PI/2
    await frames(60)
    await picture("GoldenMeadow_Coast")
    world.queue_free()
    await frames(3)
    quit()
