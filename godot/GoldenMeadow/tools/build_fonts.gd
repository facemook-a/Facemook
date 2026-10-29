extends SceneTree
func _initialize() -> void:
    for weight in ["Regular","Bold"]:
        var font:=FontFile.new()
        font.data=FileAccess.get_file_as_bytes("res://assets/ui/fonts/raw/Tajawal-"+weight+".ttf")
        var error:=ResourceSaver.save(font,"res://assets/ui/fonts/Tajawal-"+weight+".tres")
        print(weight," save=",error," glyph_a=",font.get_char_size(65,16))
    quit()
