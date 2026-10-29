extends SceneTree
func _initialize() -> void:
    call_deferred("capture")
func capture() -> void:
    var world = load("res://scenes/main.tscn").instantiate()
    root.add_child(world)
    world.set_quality(0)
    for i in range(65): await process_frame
    world.player.yaw=PI
    world.player.pitch=-0.05
    for i in range(3): await process_frame
    await RenderingServer.frame_post_draw
    root.get_texture().get_image().save_png("user://B5_game_front.png")
    world.player.yaw=0.0
    for i in range(3): await process_frame
    await RenderingServer.frame_post_draw
    root.get_texture().get_image().save_png("user://B5_game_back.png")
    world.queue_free()
    await process_frame
    quit()
