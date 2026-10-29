extends SceneTree
func _initialize() -> void:
    call_deferred("capture")
func capture() -> void:
    var world=load("res://scenes/main.tscn").instantiate()
    root.add_child(world)
    world.set_quality(0)
    world.go_beach()
    world.player.visual.rotation.y=-PI/2
    for i in range(65):await process_frame
    await RenderingServer.frame_post_draw
    root.get_texture().get_image().save_png("user://Coast_preview.png")
    world.player.yaw=PI/2
    for i in range(4):await process_frame
    await RenderingServer.frame_post_draw
    root.get_texture().get_image().save_png("user://Face_preview.png")
    world.queue_free()
    await process_frame
    quit()
