extends SceneTree
func _initialize() -> void:call_deferred("build")
func own(node: Node,root_node: Node) -> void:
    for child in node.get_children():
        child.owner=root_node
        if child.scene_file_path.is_empty():own(child,root_node)
func build() -> void:
    var actor=load("res://scenes/player.tscn").instantiate()
    root.add_child(actor)
    own(actor.visual,actor.visual)
    var scene:=PackedScene.new()
    var error:=scene.pack(actor.visual)
    if error==OK:error=ResourceSaver.save(scene,"res://scenes/avatar.tscn")
    print("Avatar scene save: ",error)
    actor.queue_free()
    await process_frame
    quit(error)
