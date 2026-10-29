extends RefCounted
## Versioned, atomic local saves. Legacy journey.json keeps the same location.
const PATH := "user://journey.json"
static func read_file(path: String) -> Dictionary:
    if not FileAccess.file_exists(path):return {}
    var parser:=JSON.new()
    if parser.parse(FileAccess.get_file_as_string(path))!=OK:return {}
    var data=parser.data
    if not data is Dictionary:return {}
    var position=data.get("position",[])
    if not position is Array or position.size()!=3:return {}
    for value in position:
        if not (value is float or value is int) or not is_finite(float(value)) or absf(float(value))>1000000:return {}
    if not data.get("collected",{}) is Dictionary:return {}
    return data
static func read() -> Dictionary:
    var data:=read_file(PATH)
    if not data.is_empty():return data
    return read_file(PATH+".bak")
static func write(data: Dictionary) -> Error:
    var temporary:=PATH+".tmp"
    var file:=FileAccess.open(temporary,FileAccess.WRITE)
    if not file:return FileAccess.get_open_error()
    file.store_string(JSON.stringify(data))
    file.flush()
    file.close()
    if read_file(temporary).is_empty():return ERR_FILE_CORRUPT
    var original:=ProjectSettings.globalize_path(PATH)
    if not read_file(PATH).is_empty():
        var copied:=DirAccess.copy_absolute(original,original+".bak")
        if copied!=OK:return copied
    return DirAccess.rename_absolute(ProjectSettings.globalize_path(temporary),original)

static func settings() -> Dictionary:
    var result:={"quality":1,"sensitivity":1.0,"master":0.85,"music":0.65,"ambience":0.8,"effects":0.8}
    var config:=ConfigFile.new()
    if config.load("user://settings.cfg")==OK:
        for key in result:result[key]=config.get_value("game",key,result[key])
    result.quality=clampi(int(result.quality),0,2)
    result.sensitivity=clampf(float(result.sensitivity),0.4,2.0)
    for key in ["master","music","ambience","effects"]:result[key]=clampf(float(result[key]),0,1)
    return result
static func save_settings(values: Dictionary) -> Error:
    var config:=ConfigFile.new()
    for key in values:config.set_value("game",key,values[key])
    return config.save("user://settings.cfg")
