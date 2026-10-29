extends Node
## AnimationTree controls the existing avatar's named limb pivots.
var tree: AnimationTree
var playback: AnimationNodeStateMachinePlayback
var state := ""
func _ready() -> void:
    var player:=AnimationPlayer.new()
    player.name="AvatarAnimationPlayer"
    player.root_node=NodePath("../..")
    add_child(player)
    var library:=AnimationLibrary.new()
    for mode in ["Idle","Walk","Run","Jump","Swim"]:library.add_animation(mode,_clip(mode))
    player.add_animation_library("",library)
    tree=AnimationTree.new()
    tree.name="AvatarAnimationTree"
    add_child(tree)
    tree.anim_player=tree.get_path_to(player)
    var machine:=AnimationNodeStateMachine.new()
    for mode in ["Idle","Walk","Run","Jump","Swim"]:
        var animation:=AnimationNodeAnimation.new()
        animation.animation=mode
        machine.add_node(mode,animation)
    for from in ["Idle","Walk","Run","Jump","Swim"]:
        for to in ["Idle","Walk","Run","Jump","Swim"]:
            if from==to:continue
            var transition:=AnimationNodeStateMachineTransition.new()
            transition.xfade_time=0.15
            machine.add_transition(from,to,transition)
    tree.tree_root=machine
    tree.active=true
    playback=tree.get("parameters/playback")
    playback.start("Idle")
    state="Idle"
func set_motion(speed: float,grounded: bool,swimming: bool) -> void:
    var next:="Swim" if swimming else ("Jump" if not grounded else ("Run" if speed>5 else ("Walk" if speed>0.35 else "Idle")))
    if next!=state:
        state=next
        playback.travel(next)
func _clip(mode: String) -> Animation:
    var clip:=Animation.new()
    clip.length={"Idle":3.0,"Walk":0.82,"Run":0.58,"Jump":0.8,"Swim":1.4}[mode]
    clip.loop_mode=Animation.LOOP_LINEAR
    var limbs:=["LeftLeg","RightLeg","LeftArm","RightArm"]
    for i in range(limbs.size()):
        var track:=clip.add_track(Animation.TYPE_VALUE)
        clip.track_set_path(track,NodePath("Visual/"+limbs[i]+":rotation"))
        for step in range(17):
            var phase:=step/16.0*TAU
            var swing:=sin(phase)*(1 if i%2==0 else -1)
            var value:=Vector3.ZERO
            if mode=="Walk":value.x=swing*(0.48 if i<2 else -0.32)
            if mode=="Run":value.x=swing*(0.8 if i<2 else -0.7)
            if mode=="Idle" and i>=2:value.z=(0.025+sin(phase)*0.01)*(1 if i==2 else -1)
            if mode=="Jump":value.x=-0.18 if i<2 else -0.55
            if mode=="Swim":value.x=swing*(0.22 if i<2 else 1.1)-0.6
            clip.track_insert_key(track,clip.length*step/16.0,value)
    return clip
