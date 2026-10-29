extends SceneTree

func _initialize() -> void:
    call_deferred("capture")

func capture() -> void:
    var world = load("res://scenes/main.tscn").instantiate()
    root.add_child(world)
    for i in range(65): await process_frame
    await RenderingServer.frame_post_draw
    var output := "user://preview.png"
    if OS.get_cmdline_user_args().size()>0:
        for arg in OS.get_cmdline_user_args():
            if arg.begins_with("--output="): output=arg.trim_prefix("--output=")
    root.get_texture().get_image().save_png(output)
    print("Saved actual game capture: "+output)
    world.queue_free()
    await process_frame
    quit()
