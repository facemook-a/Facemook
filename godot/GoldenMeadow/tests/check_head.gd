extends SceneTree
func _initialize() -> void:
    call_deferred("capture")
func capture() -> void:
    var world=load("res://scenes/main.tscn").instantiate()
    root.add_child(world)
    world.set_quality(0)
    for i in range(55):await process_frame
    world.player.yaw=PI
    world.player.pitch=-0.05
    for i in range(4):await process_frame
    await RenderingServer.frame_post_draw
    root.get_texture().get_image().save_png("user://B5_front_final.png")
    world.queue_free()
    await process_frame
    quit()
