extends Node3D
var opened := false
var speaker: AudioStreamPlayer3D
var save_id:=""
var motion: Tween
func _ready() -> void:
    speaker=AudioStreamPlayer3D.new()
    speaker.stream=load("res://assets/audio/door.wav")
    speaker.max_distance=18.0
    speaker.volume_db=-5
    speaker.bus="Effects" if AudioServer.get_bus_index("Effects")>=0 else "Master"
    add_child(speaker)
func interact() -> void:
    set_opened(not opened)
    speaker.play()
    var world=get_parent().get_parent()
    world._save_progress()
func set_opened(value: bool,instant:=false) -> void:
    opened=value
    if motion and motion.is_running():motion.kill()
    if instant:
        rotation.y=-1.65 if opened else 0.0
        return
    motion=create_tween()
    motion.tween_property(self,"rotation:y",-1.65 if opened else 0.0,0.5).set_trans(Tween.TRANS_SINE)
