extends Control
var hud: CanvasLayer
var page:=""
var panel: PanelContainer
var body: VBoxContainer
var heading: Label
var map_view: Control
var sliders: Array[HSlider]=[]
var slider_touch: HSlider
var slider_index:=-1
var opened: bool:
    get:return visible
func _ready() -> void:
    process_mode=Node.PROCESS_MODE_ALWAYS
    set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    var shade:=ColorRect.new()
    shade.color=Color(0.015,0.03,0.035,0.88)
    shade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    add_child(shade)
    panel=PanelContainer.new()
    panel.set_anchors_and_offsets_preset(Control.PRESET_CENTER)
    panel.position=Vector2(-335,-294)
    panel.size=Vector2(670,588)
    panel.add_theme_stylebox_override("panel",hud._panel(Color("122a29"),22))
    add_child(panel)
    var layout:=VBoxContainer.new()
    layout.add_theme_constant_override("separation",12)
    panel.add_child(layout)
    var top:=HBoxContainer.new()
    layout.add_child(top)
    heading=Label.new()
    heading.text="B5  ·  GOLDEN MEADOW"
    heading.add_theme_font_size_override("font_size",22)
    heading.add_theme_font_override("font",hud.strong_font)
    heading.add_theme_color_override("font_color",Color("edcf95"))
    heading.size_flags_horizontal=Control.SIZE_EXPAND_FILL
    top.add_child(heading)
    top.add_child(hud._button("×",Vector2(42,42),close))
    var tabs:=HBoxContainer.new()
    layout.add_child(tabs)
    for item in [["pause","اللعبة"],["map","الخريطة"],["journal","رحلتي"],["settings","الإعدادات"],["help","التحكم"]]:
        var id:String=item[0]
        tabs.add_child(hud._button(item[1],Vector2(116,40),func():open(id)))
    body=VBoxContainer.new()
    body.add_theme_constant_override("separation",10)
    body.size_flags_vertical=Control.SIZE_EXPAND_FILL
    layout.add_child(body)
    visible=false
func _clear() -> void:
    for child in body.get_children():
        body.remove_child(child)
        child.queue_free()
    sliders.clear()
    map_view=null
func _label(text: String,font_size:=17) -> Label:
    var label:=Label.new()
    label.text=text
    label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    label.horizontal_alignment=HORIZONTAL_ALIGNMENT_RIGHT
    label.add_theme_font_size_override("font_size",font_size)
    label.add_theme_constant_override("line_spacing",4)
    body.add_child(label)
    return label
func open(which:="pause") -> void:
    page=which
    hud.ui.visible=true
    hud.move_vector=Vector2.ZERO
    hud.touch_id=-1
    hud.look_id=-1
    Input.mouse_mode=Input.MOUSE_MODE_VISIBLE
    get_tree().paused=true
    visible=true
    _clear()
    var world=hud.get_parent()
    if which=="pause":
        _label("خُذ وقتك. تقدم الرحلة يبقى محفوظًا على هذا الجهاز.",20)
        body.add_child(hud._button("متابعة الاستكشاف",Vector2(0,50),close))
        body.add_child(hud._button("حفظ الرحلة",Vector2(0,46),func():world._save_progress();_label("تم حفظ الرحلة محليًا.")))
        body.add_child(hud._button("العودة للبيت",Vector2(0,46),func():close();world.return_home()))
        body.add_child(hud._button("الانتقال للشاطئ",Vector2(0,46),func():close();world.go_beach()))
        _label("GOLDEN MEADOW · مغامرة فردية تعمل دون إنترنت\nM للخريطة · J للرحلة · Esc للإيقاف المؤقت",15)
    elif which=="journal":
        _label(world.journey.summary(),18)
    elif which=="map":
        _label("المروج الذهبية  ·  اضغط موقعًا لتتبعه",20)
        map_view=load("res://scripts/world_map.gd").new()
        map_view.world=world
        map_view.custom_minimum_size=Vector2(610,300)
        body.add_child(map_view)
        _label("الذهبي: الشعلات · البيوت: القرية · الأزرق: الرفاق\n"+("الوجهة: "+world.navigation_name if not world.navigation_name.is_empty() else "تحرك شرقًا من القرية للوصول إلى البحر."),15)
        body.add_child(hud._button("مسح الوجهة",Vector2(0,40),func():world.navigation_name="";open("map")))
    elif which=="settings":
        _label("إعدادات محفوظة لهذا الجهاز",20)
        body.add_child(hud._button("جودة: "+["منخفضة","متوسطة","عالية"][int(world.settings.quality)],Vector2(0,44),func():
            var settings:Dictionary=world.settings.duplicate()
            settings.quality=(int(settings.quality)+1)%3
            world.apply_settings(settings)
            open("settings")))
        for item in [["master","الصوت العام",0.0,1.0],["music","الموسيقى",0.0,1.0],["ambience","الطبيعة والبحر",0.0,1.0],["effects","الخطوات والتفاعل",0.0,1.0],["sensitivity","حساسية النظر",0.4,2.0]]:
            var key:String=item[0]
            var row:=HBoxContainer.new()
            row.layout_direction=Control.LAYOUT_DIRECTION_RTL
            body.add_child(row)
            var label:=Label.new()
            label.text=item[1]
            label.horizontal_alignment=HORIZONTAL_ALIGNMENT_RIGHT
            label.custom_minimum_size=Vector2(215,38)
            row.add_child(label)
            var slider:=HSlider.new()
            slider.layout_direction=Control.LAYOUT_DIRECTION_LTR
            slider.min_value=item[2]
            slider.max_value=item[3]
            slider.step=0.02
            slider.value=float(world.settings[key])
            slider.custom_minimum_size=Vector2(360,38)
            slider.add_theme_icon_override("grabber",load("res://assets/ui/icons/knob.svg"))
            slider.add_theme_icon_override("grabber_highlight",load("res://assets/ui/icons/knob.svg"))
            row.add_child(slider)
            sliders.append(slider)
            slider.value_changed.connect(func(value: float):
                var settings:Dictionary=world.settings.duplicate()
                settings[key]=value
                world.apply_settings(settings))
        _label("الجودة المنخفضة تقلل العشب والظلال. اخترها إذا انخفضت سلاسة اللعب.",15)
    else:
        _label("اللمس\nاسحب العصا يسارًا للمشي، ويمين الشاشة للنظر.\nركض: تبديل السرعة · قفز: القفز أو السباحة قرب السطح.\nاقترب من الباب أو الشخصية واضغط زر التفاعل.\n\nالحاسوب\nWASD / الأسهم: المشي · الفأرة اليمنى: النظر.\nSpace: قفز · Shift: ركض · E: تفاعل.\nM: خريطة · J: دفتر الرحلة · P: تصوير · Esc: القائمة.\n\nالمرافقة\nاضغط «رافقني» قرب ليلى أو سلمى أو نور، و«إنهاء المرافقة» للتوقف.\nحاول جمع الشعلات الست وزيارة القرية والشاطئ.",18)
func close() -> void:
    visible=false
    get_tree().paused=false
    slider_touch=null
    if is_instance_valid(hud.get_parent().player):hud.get_parent()._save_progress()
func touch(event: InputEvent) -> bool:
    if not visible:return false
    if event is InputEventScreenTouch:
        if not event.pressed and event.index==slider_index:
            slider_touch=null
            return true
        if event.pressed:
            for slider in sliders:
                if slider.get_global_rect().has_point(event.position):
                    slider_touch=slider
                    slider_index=event.index
                    _set_slider(event.position)
                    return true
            if map_view and map_view.get_global_rect().has_point(event.position):
                map_view.select_at(event.position-map_view.global_position)
                return true
    if event is InputEventScreenDrag and slider_touch and event.index==slider_index:
        _set_slider(event.position)
        return true
    if event is InputEventMouseButton and event.pressed and event.button_index==MOUSE_BUTTON_LEFT and map_view and map_view.get_global_rect().has_point(event.position):
        map_view.select_at(event.position-map_view.global_position)
        return true
    return false
func _set_slider(position: Vector2) -> void:
    var ratio:=clampf((position.x-slider_touch.global_position.x)/slider_touch.size.x,0,1)
    slider_touch.value=lerpf(slider_touch.min_value,slider_touch.max_value,ratio)
