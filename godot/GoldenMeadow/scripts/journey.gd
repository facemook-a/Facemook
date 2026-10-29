extends Node
## Peaceful adventure progression; all original light locations are retained.
const ENTRIES := [
    ["home","باب البداية","ادخل البيت الحجري في وسط القرية."],
    ["coast","حيث يلتقي الضوء بالماء","وصل إلى الشاطئ شرق المروج."],
    ["company","رفقة الطريق","رافق إحدى شخصيات الشاطئ إلى القرية."],
    ["lights","ست علامات من نور","اجمع شعلات النور الست بين التلال."]]
var completed: Dictionary={}
var visited_houses: Array=[]
var clock:=0.0
func _process(delta: float) -> void:
    clock+=delta
    if clock<0.4:return
    clock=0
    var world=get_parent()
    var p: Vector3=world.player.position
    if p.x>139:complete("coast")
    if world.collected.size()==6:complete("lights")
    for i in range(world.Land.VILLAGE.size()):
        var house: Vector2=world.Land.VILLAGE[i]
        if absf(p.x-house.x)<4.3 and absf(p.z-house.y)<2.8 and absf(p.y-world.Land.height(house.x,house.y))<2:
            if not i in visited_houses:
                visited_houses.append(i)
                world._save_progress()
            if i==0:complete("home")
    for person in world.activities.people:
        if person.following and p.x<90 and p.distance_to(person.position)<6:complete("company")
func complete(id: String) -> void:
    if completed.get(id,false):return
    completed[id]=true
    var world=get_parent()
    for entry in ENTRIES:
        if entry[0]==id:world.hud.announce("اكتملت الرحلة · "+entry[1])
    if world.activities:world.activities.play_ui_sound()
    world._save_progress()
func summary() -> String:
    var text:="دفتر الرحلة  ·  %d / 4\n\n" % completed.size()
    for entry in ENTRIES:
        text+=("✓  " if completed.get(entry[0],false) else "○  ")+entry[1]+"\n"+entry[2]+"\n\n"
    return text+"بيوت زرتها: %d / 9\nتقدمك يُحفظ محليًا. يمكنك الاستكشاف بحرية بعد إكمال الرحلة." % visited_houses.size()
