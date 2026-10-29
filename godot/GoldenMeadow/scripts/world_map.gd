extends Control
var world: Node3D
const MINIMUM:=Vector2(-115,-140)
const EXTENT:=Vector2(300,240)
func project(p: Vector2) -> Vector2:return (p-MINIMUM)/EXTENT*size
func select_at(local: Vector2) -> void:
    var point:=MINIMUM+local/size*EXTENT
    var name:="نقطة على الخريطة"
    for house in world.Land.VILLAGE:
        if point.distance_to(house)<10:
            point=house+Vector2(0,5)
            name="البيت الحجري" if house==Vector2.ZERO else "بيت في القرية"
            break
    if point.x>137:
        point.x=148
        name="شاطئ النور"
    world.set_navigation(name,Vector3(point.x,world.Land.height(point.x,point.y),point.y))
    queue_redraw()
func _draw() -> void:
    draw_style_box(_background(),Rect2(Vector2.ZERO,size))
    for z in range(24):
        for x in range(30):
            var p:=MINIMUM+Vector2(x*10,z*10)
            var h:float=world.Land.height(p.x,p.y)
            var color:=Color("234c43").lightened(clampf((h+3)*0.012,0,0.14))
            if p.x>135:color=Color("a69b6f")
            if p.x>145 and h< -2.5:color=Color("1c5869")
            draw_rect(Rect2(project(p),size/Vector2(30,24)+Vector2.ONE),color)
    for house in world.Land.VILLAGE:
        var p:=project(house)
        draw_rect(Rect2(p-Vector2(4,3),Vector2(8,7)),Color("eed4a4"))
        draw_polyline(PackedVector2Array([p+Vector2(-5,-3),p+Vector2(0,-7),p+Vector2(5,-3)]),Color("eed4a4"),2)
    for i in range(world.locations.size()):
        var p:=project(world.locations[i])
        draw_circle(p,4,Color("91b49c") if world.collected.has(str(i)) else Color("f5c866"))
    for person in world.activities.people:draw_circle(project(Vector2(person.position.x,person.position.z)),3,Color("84ced6"))
    var p:=project(Vector2(world.player.position.x,world.player.position.z))
    var a:float=world.player.yaw
    var points:=PackedVector2Array()
    for corner in [Vector2(0,-9),Vector2(-6,6),Vector2(6,6)]:points.append(p+corner.rotated(-a))
    draw_colored_polygon(points,Color.WHITE)
    if not world.navigation_name.is_empty():
        var target:=project(Vector2(world.navigation_target.x,world.navigation_target.z))
        draw_arc(target,10,0,TAU,32,Color("fce2a8"),2,true)
        draw_line(p,target,Color(1,0.9,0.6,0.35),1,true)
func _background() -> StyleBoxFlat:
    var style:=StyleBoxFlat.new()
    style.bg_color=Color("1a3631")
    style.set_corner_radius_all(12)
    return style
