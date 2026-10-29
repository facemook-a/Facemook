extends CanvasLayer

var move_vector := Vector2.ZERO
var sprinting := false
var jump_requested := false
var quality := 1
var menus: Control
var stamina_bar: ProgressBar
var photo := false
var touch_id := -1
var look_id := -1
var stick_origin := Vector2.ZERO
var stick_point := Vector2.ZERO
var ui: Control
var touch_art: Control
var status: Label
var bearing: Label
var message: Label
var notice_time := 9.0
var quality_button: Button
var interact_button: Button
var info: PanelContainer
var clock := 0.0
var buttons: Array[Button] = []
var font := preload("res://assets/ui/fonts/Tajawal-Regular.tres")
var strong_font := preload("res://assets/ui/fonts/Tajawal-Bold.tres")
var interaction_label: Label
var message_box: PanelContainer

func _ready() -> void:
    process_mode=Node.PROCESS_MODE_ALWAYS
    ui = Control.new()
    ui.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    ui.mouse_filter=Control.MOUSE_FILTER_IGNORE
    add_child(ui)
    var theme := Theme.new()
    theme.default_font=font
    theme.default_font_size=17
    ui.theme=theme
    var top := PanelContainer.new()
    top.position=Vector2(24,22)
    top.size=Vector2(280,86)
    top.add_theme_stylebox_override("panel",_panel(Color(0.035,0.08,0.07,0.60),16))
    ui.add_child(top)
    var brand:=HBoxContainer.new()
    brand.add_theme_constant_override("separation",12)
    top.add_child(brand)
    var mark:=TextureRect.new()
    mark.texture=load("res://assets/b5_icon_192.png")
    mark.custom_minimum_size=Vector2(46,46)
    mark.expand_mode=TextureRect.EXPAND_IGNORE_SIZE
    mark.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
    brand.add_child(mark)
    var stack := VBoxContainer.new()
    stack.add_theme_constant_override("separation",2)
    brand.add_child(stack)
    var title := Label.new()
    title.text="GOLDEN MEADOW"
    title.add_theme_font_size_override("font_size",18)
    title.add_theme_font_override("font",strong_font)
    title.add_theme_color_override("font_color",Color("edcf93"))
    stack.add_child(title)
    var subtitle := Label.new()
    subtitle.text="رحلة في المروج الذهبية"
    subtitle.add_theme_font_size_override("font_size",13)
    stack.add_child(subtitle)
    status=Label.new()
    status.add_theme_font_size_override("font_size",14)
    stack.add_child(status)
    bearing=Label.new()
    bearing.set_anchors_and_offsets_preset(Control.PRESET_CENTER_TOP)
    bearing.position=Vector2(-210,27)
    bearing.size=Vector2(420,30)
    bearing.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER
    bearing.add_theme_color_override("font_color",Color("ffe8b3"))
    bearing.add_theme_constant_override("outline_size",3)
    bearing.add_theme_font_override("font",strong_font)
    bearing.add_theme_color_override("font_outline_color",Color(0.035,0.06,0.05,0.9))
    ui.add_child(bearing)
    var tools := HBoxContainer.new()
    tools.set_anchors_and_offsets_preset(Control.PRESET_TOP_RIGHT)
    tools.position=Vector2(-421,24)
    tools.visible=false
    tools.add_theme_constant_override("separation",9)
    ui.add_child(tools)
    quality_button=_button("جودة: متوسطة",Vector2(132,44),cycle_quality)
    tools.add_child(quality_button)
    tools.add_child(_button("الصوت",Vector2(70,44),func(): get_parent().toggle_sound()))
    tools.add_child(_button("البيت",Vector2(65,44),func(): get_parent().return_home()))
    tools.add_child(_button("؟",Vector2(44,44),_toggle_help))
    var photo_button := _icon_button("camera",Vector2(46,46),"إخفاء الواجهة للتصوير",toggle_photo)
    photo_button.set_anchors_and_offsets_preset(Control.PRESET_TOP_RIGHT)
    photo_button.position=Vector2(-76,82)
    # Keep a small exit target available in photo mode.
    add_child(photo_button)
    photo_button.theme=theme
    message_box=PanelContainer.new()
    message_box.set_anchors_and_offsets_preset(Control.PRESET_CENTER_BOTTOM)
    message_box.position=Vector2(-295,-92)
    message_box.size=Vector2(590,48)
    message_box.add_theme_stylebox_override("panel",_panel(Color(0.04,0.09,0.09,0.82),14))
    ui.add_child(message_box)
    message=Label.new()
    message.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    message.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER
    message.add_theme_font_size_override("font_size",16)
    message.add_theme_color_override("font_color",Color("eee8d9"))
    message_box.add_child(message)
    announce("رحلتك تبدأ هنا · اقترب من البيت أو افتح الخريطة")
    var sprint := _icon_button("run",Vector2(72,72),"ركض · Shift",func():
        sprinting=not sprinting)
    sprint.toggle_mode=true
    sprint.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_RIGHT)
    sprint.position=Vector2(-204,-106)
    ui.add_child(sprint)
    var jump := _icon_button("jump",Vector2(88,88),"قفز · Space",func(): jump_requested=true)
    jump.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_RIGHT)
    jump.position=Vector2(-114,-126)
    ui.add_child(jump)
    interact_button=_icon_button("interact",Vector2(66,66),"تفاعل · E",func():
        if get_parent().activities:get_parent().activities.interact())
    interact_button.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_RIGHT)
    interact_button.position=Vector2(-104,-216)
    ui.add_child(interact_button)
    interaction_label=Label.new()
    interaction_label.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_RIGHT)
    interaction_label.position=Vector2(-332,-202)
    interaction_label.size=Vector2(214,38)
    interaction_label.horizontal_alignment=HORIZONTAL_ALIGNMENT_RIGHT
    interaction_label.vertical_alignment=VERTICAL_ALIGNMENT_CENTER
    interaction_label.add_theme_font_override("font",strong_font)
    interaction_label.add_theme_font_size_override("font_size",18)
    interaction_label.add_theme_constant_override("outline_size",3)
    interaction_label.add_theme_color_override("font_outline_color",Color(0.02,0.06,0.06,0.9))
    ui.add_child(interaction_label)
    var beach:=_button("الشاطئ",Vector2(88,46),func():get_parent().go_beach())
    beach.set_anchors_and_offsets_preset(Control.PRESET_TOP_RIGHT)
    beach.position=Vector2(-174,82)
    beach.visible=false
    ui.add_child(beach)
    # Empty control draws the analog stick; it does not intercept touch events.
    touch_art=Control.new()
    touch_art.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    touch_art.mouse_filter=Control.MOUSE_FILTER_IGNORE
    touch_art.draw.connect(_draw_stick)
    ui.add_child(touch_art)
    _make_help()
    stamina_bar=ProgressBar.new()
    stamina_bar.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_LEFT)
    stamina_bar.position=Vector2(56,-36)
    stamina_bar.size=Vector2(128,5)
    stamina_bar.show_percentage=false
    var bar_background:=StyleBoxFlat.new()
    bar_background.bg_color=Color(0.04,0.09,0.08,0.65)
    bar_background.set_corner_radius_all(3)
    var bar_fill:=StyleBoxFlat.new()
    bar_fill.bg_color=Color("82b39c")
    bar_fill.set_corner_radius_all(3)
    stamina_bar.add_theme_stylebox_override("background",bar_background)
    stamina_bar.add_theme_stylebox_override("fill",bar_fill)
    ui.add_child(stamina_bar)
    var navigation:=HBoxContainer.new()
    navigation.set_anchors_and_offsets_preset(Control.PRESET_TOP_RIGHT)
    navigation.position=Vector2(-222,24)
    navigation.add_theme_constant_override("separation",12)
    ui.add_child(navigation)
    navigation.add_child(_icon_button("journal",Vector2(48,48),"دفتر الرحلة · J",func():menus.open("journal")))
    navigation.add_child(_icon_button("map",Vector2(48,48),"الخريطة · M",func():menus.open("map")))
    navigation.add_child(_icon_button("menu",Vector2(48,48),"القائمة · Esc",func():menus.open("pause")))
    menus=load("res://scripts/menus.gd").new()
    menus.hud=self
    ui.add_child(menus)


func _panel(color: Color,radius: int) -> StyleBoxFlat:
    var style := StyleBoxFlat.new()
    style.bg_color=color
    style.set_corner_radius_all(radius)
    style.set_content_margin_all(13)
    style.border_color=Color(0.85,0.75,0.5,0.22)
    style.set_border_width_all(1)
    return style

func _button(text: String, size: Vector2, callback: Callable) -> Button:
    var button := Button.new()
    button.text=text
    button.custom_minimum_size=size
    button.focus_mode=Control.FOCUS_NONE
    button.add_theme_stylebox_override("normal",_panel(Color(0.06,0.12,0.10,0.78),14))
    button.add_theme_stylebox_override("hover",_panel(Color(0.16,0.24,0.17,0.94),14))
    button.add_theme_stylebox_override("pressed",_panel(Color(0.38,0.40,0.22,0.94),14))
    button.add_theme_color_override("font_color",Color("f8e8c4"))
    button.add_theme_font_size_override("font_size",16)
    var labels:={"×":"close","اللعبة":"play","الخريطة":"map","رحلتي":"journal","الإعدادات":"settings","التحكم":"help","متابعة الاستكشاف":"play","حفظ الرحلة":"save","العودة للبيت":"home","الانتقال للشاطئ":"wave","مسح الوجهة":"close"}
    if labels.has(text):
        button.icon=load("res://assets/ui/icons/"+labels[text]+".svg")
        button.add_theme_constant_override("icon_max_width",20)
        button.expand_icon=true
        if text=="×":button.text=""
    button.pressed.connect(callback)
    buttons.append(button)
    return button

func _icon_button(icon: String,size: Vector2,tooltip: String,callback: Callable) -> Button:
    var button:=_button("",size,callback)
    button.icon=load("res://assets/ui/icons/"+icon+".svg")
    button.expand_icon=true
    button.add_theme_constant_override("icon_max_width",30 if size.x>60 else 23)
    button.tooltip_text=tooltip
    var radius:=clampi(roundi(size.y*0.3),12,22)
    button.add_theme_stylebox_override("normal",_panel(Color(0.035,0.085,0.085,0.68),radius))
    button.add_theme_stylebox_override("hover",_panel(Color(0.10,0.22,0.20,0.94),radius))
    button.add_theme_stylebox_override("pressed",_panel(Color(0.22,0.38,0.30,0.96),radius))
    return button

func _make_help() -> void:
    info=PanelContainer.new()
    info.set_anchors_and_offsets_preset(Control.PRESET_CENTER)
    info.position=Vector2(-270,-190)
    info.size=Vector2(540,380)
    info.add_theme_stylebox_override("panel",_panel(Color(0.035,0.07,0.06,0.97),22))
    ui.add_child(info)
    var content := VBoxContainer.new()
    content.add_theme_constant_override("separation",13)
    info.add_child(content)
    var label := Label.new()
    label.text="مرحباً بك في المروج الذهبية\n\nتجول في القرية والشاطئ واجمع شعلات النور.\nاقترب من الباب واضغط تفاعل لفتحه أو إغلاقه.\nعلى الشاطئ: تفاعل مع الشخصيات للمرافقة أو إيقافها.\n\nالهاتف: عصا يساراً، اسحب يميناً، قفز وركض.\nالحاسوب: WASD، الفأرة اليمنى للنظر، Space للقفز.\nShift للركض · E للتفاعل · R للبيت · Q للجودة.\nزر الشاطئ ينقلك إلى البحر، ويمكنك الوصول سيراً شرقاً.\nاختر جودة منخفضة إذا كان هاتفك بطيئاً."
    label.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER
    label.add_theme_font_size_override("font_size",17)
    content.add_child(label)
    content.add_child(_button("انطلق",Vector2(0,44),_toggle_help))
    info.visible=false

func _toggle_help() -> void:
    menus.open("help")

func announce(text: String) -> void:
    message.text=text
    notice_time=7.0

func cycle_quality() -> void:
    var world=get_parent()
    var settings:Dictionary=world.settings.duplicate()
    settings.quality=(int(settings.quality)+1)%3
    world.apply_settings(settings)

func toggle_photo() -> void:
    photo=not photo
    ui.visible=not photo

func _input(event: InputEvent) -> void:
    if event.is_action_pressed("menu"):
        if menus.opened:menus.close()
        else:menus.open("pause")
        get_viewport().set_input_as_handled()
        return
    for item in [["map","map"],["journal","journal"]]:
        if event.is_action_pressed(item[0]):
            if menus.opened and menus.page==item[1]:menus.close()
            else:menus.open(item[1])
            get_viewport().set_input_as_handled()
            return
    if menus.touch(event):
        get_viewport().set_input_as_handled()
        return
    # Handle each touch explicitly, so buttons and two simultaneous fingers
    # work without converting the first finger into a mouse pointer.
    if event is InputEventScreenTouch and event.pressed:
        buttons=buttons.filter(func(button):return is_instance_valid(button))
        for button in buttons:
            if menus.opened and not menus.is_ancestor_of(button):continue
            if button.is_visible_in_tree() and button.get_global_rect().has_point(event.position):
                if button.toggle_mode: button.button_pressed=not button.button_pressed
                button.pressed.emit()
                get_viewport().set_input_as_handled()
                return
    if menus.opened:return
    var size := get_viewport().get_visible_rect().size
    if event is InputEventScreenTouch:
        if event.pressed:
            if event.position.x<size.x*0.42 and event.position.y>size.y*0.35 and touch_id==-1:
                touch_id=event.index
                stick_origin=event.position
                stick_point=event.position
            elif look_id==-1:
                look_id=event.index
        else:
            if event.index==touch_id:
                touch_id=-1
                move_vector=Vector2.ZERO
            if event.index==look_id: look_id=-1
    if event is InputEventScreenDrag:
        if event.index==touch_id:
            stick_point=event.position
            move_vector=((stick_point-stick_origin)/62.0).limit_length(1.0)
        if event.index==look_id: get_parent().player.look(event.relative*0.004*float(get_parent().settings.get("sensitivity",1.0)))
    touch_art.queue_redraw()

func _draw_stick() -> void:
    var size := get_viewport().get_visible_rect().size
    var origin := stick_origin if touch_id!=-1 else Vector2(119,size.y-121)
    touch_art.draw_circle(origin,63.0,Color(0.07,0.14,0.10,0.28))
    touch_art.draw_arc(origin,63.0,0,TAU,60,Color(0.94,0.87,0.63,0.35),1.5,true)
    touch_art.draw_circle(origin+move_vector*43.0,24.0,Color(0.94,0.87,0.63,0.33))

func _process(delta: float) -> void:
    clock+=delta
    notice_time-=delta
    message_box.modulate.a=clampf(notice_time,0.0,1.0)
    if clock<0.15: return
    clock=0.0
    var world := get_parent()
    stamina_bar.value=world.player.stamina
    stamina_bar.visible=world.player.stamina<99 or world.player.sprinting
    if world.activities:
        interaction_label.text=world.activities.interaction_text()
        interaction_label.visible=world.activities.interaction!=null
        interact_button.modulate.a=1.0 if world.activities.interaction else 0.4
    var p: Vector3=world.player.position
    status.text="نور  \u2066%d / 6\u2069    ·    %s" % [world.collected.size(),"شاطئ النور" if p.x>137 else "قرية المروج"]
    if not world.navigation_name.is_empty():
        bearing.text=world.navigation_name+" · %.0f م" % Vector2(p.x-world.navigation_target.x,p.z-world.navigation_target.z).length()
    elif world.collected.size()<6:
        var target: Vector3=world.nearest_wisp()
        var direction := target-p
        var angle := wrapf(atan2(-direction.x,-direction.z)-world.player.yaw,-PI,PI)
        var arrow := "↑" if absf(angle)<0.35 else ("←" if angle>0 else "→")
        bearing.text="%s   أقرب شعلة   %.0f م" % [arrow,Vector2(direction.x,direction.z).length()]
    else: bearing.text="✦  المروج مفتوحة لك  ✦"
    touch_art.queue_redraw()

func _notification(what: int) -> void:
    if what == NOTIFICATION_WM_GO_BACK_REQUEST and menus:
        if menus.opened:menus.close()
        else:menus.open("pause")
    if what == NOTIFICATION_APPLICATION_FOCUS_OUT:
        move_vector=Vector2.ZERO
        touch_id=-1
        look_id=-1
