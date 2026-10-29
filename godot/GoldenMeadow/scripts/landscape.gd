extends RefCounted

const SIZE := 32.0
const STEPS := 16
const VILLAGE := [Vector2(0,0),Vector2(-35,-25),Vector2(35,-20),Vector2(-55,25),Vector2(25,60),Vector2(-65,-70),Vector2(65,-65),Vector2(75,25),Vector2(20,-105)]

static func height(x: float, z: float) -> float:
    var h := raw_height(x,z)
    for house in VILLAGE:
        var distance := Vector2(x,z).distance_to(house)
        if distance<16.0:
            var flat := raw_height(house.x,house.y)
            h=lerpf(flat,h,smoothstep(8.0,16.0,distance))
    return h

static func raw_height(x: float,z: float) -> float:
    var rolling := sin(x*0.018)*cos(z*0.016)*5.0+sin(x*0.047+z*0.015)*1.7+cos(z*0.036-x*0.025)*1.2
    rolling*=smoothstep(10.0,25.0,Vector2(x,z).length())
    var coast := -1.2-(x-130.0)*0.048+sin(z*0.025)*0.20
    return lerpf(rolling,coast,smoothstep(95.0,150.0,x))

static func normal_at(x: float, z: float) -> Vector3:
    return Vector3(height(x - 0.2, z) - height(x + 0.2, z), 0.4, height(x, z - 0.2) - height(x, z + 0.2)).normalized()

static func height_grid(coord: Vector2i) -> PackedFloat32Array:
    var grid:=PackedFloat32Array()
    for z in range(STEPS+1):
        for x in range(STEPS+1):grid.append(height(coord.x*SIZE+x*SIZE/STEPS,coord.y*SIZE+z*SIZE/STEPS))
    return grid

static func sample_grid(grid: PackedFloat32Array,coord: Vector2i,x: float,z: float) -> float:
    var fx:=clampf((x-coord.x*SIZE)/SIZE*STEPS,0,STEPS-0.0001)
    var fz:=clampf((z-coord.y*SIZE)/SIZE*STEPS,0,STEPS-0.0001)
    var ix:=floori(fx)
    var iz:=floori(fz)
    fx-=ix
    fz-=iz
    var i:=iz*(STEPS+1)+ix
    if fx+fz<=1:return grid[i]+fx*(grid[i+1]-grid[i])+fz*(grid[i+STEPS+1]-grid[i])
    return grid[i+STEPS+2]+(1-fx)*(grid[i+STEPS+1]-grid[i+STEPS+2])+(1-fz)*(grid[i+1]-grid[i+STEPS+2])

static func terrain_mesh(coord: Vector2i) -> ArrayMesh:
    var grid:=height_grid(coord)
    var vertices:=PackedVector3Array()
    var normals:=PackedVector3Array()
    var uvs:=PackedVector2Array()
    var indices:=PackedInt32Array()
    for z in range(STEPS+1):
        for x in range(STEPS+1):
            var wx:float=coord.x*SIZE+x*SIZE/STEPS
            var wz:float=coord.y*SIZE+z*SIZE/STEPS
            vertices.append(Vector3(wx,grid[z*(STEPS+1)+x],wz))
            normals.append(normal_at(wx,wz))
            uvs.append(Vector2(wx,wz))
    for z in range(STEPS):
        for x in range(STEPS):
            var i:=z*(STEPS+1)+x
            indices.append_array(PackedInt32Array([i,i+1,i+STEPS+1,i+1,i+STEPS+2,i+STEPS+1]))
    var arrays:=[]
    arrays.resize(Mesh.ARRAY_MAX)
    arrays[Mesh.ARRAY_VERTEX]=vertices
    arrays[Mesh.ARRAY_NORMAL]=normals
    arrays[Mesh.ARRAY_TEX_UV]=uvs
    arrays[Mesh.ARRAY_INDEX]=indices
    var mesh:=ArrayMesh.new()
    mesh.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES,arrays)
    return mesh

static func grass_mesh() -> ArrayMesh:
    var st := SurfaceTool.new()
    st.begin(Mesh.PRIMITIVE_TRIANGLES)
    for blade in range(7):
        var a := blade * 2.39996
        var right := Vector3(cos(a), 0.0, sin(a))
        var base := right * (0.10+blade*0.022)
        var tall := 0.33 + blade * 0.025
        for seg in range(2):
            var t0 := float(seg) / 2.0
            var t1 := float(seg+1) / 2.0
            var p0 := base + Vector3.UP * t0 * tall + right * t0*t0 * 0.20
            var p1 := base + Vector3.UP * t1 * tall + right * t1*t1 * 0.20
            var w0 := (1.0-t0) * 0.020
            var w1 := (1.0-t1) * 0.020
            var points := [p0-right*w0,p1-right*w1,p0+right*w0,p0+right*w0,p1-right*w1,p1+right*w1]
            var uvs := [Vector2(0,1-t0),Vector2(0,1-t1),Vector2(1,1-t0),Vector2(1,1-t0),Vector2(0,1-t1),Vector2(1,1-t1)]
            for i in range(6):
                st.set_normal(Vector3(0.0,0.65,0.76))
                st.set_uv(uvs[i])
                st.add_vertex(points[i])
    return st.commit()

static func grass_allowed(x: float, z: float) -> bool:
    if x>133.0: return false
    for p in VILLAGE:
        if absf(x-p.x)<5.8 and absf(z-p.y)<4.6:return false
    if z > 3.0 and z < 34.0 and absf(x-sin(z*0.12)*1.5) < 1.1: return false
    var p:=Vector2(x,z)
    for home in VILLAGE:
        var a:=Vector2(0,7)
        var b:Vector2=home+Vector2(0,7)
        var edge:=b-a
        if edge.length_squared()<1:continue
        var t:=clampf((p-a).dot(edge)/edge.length_squared(),0,1)
        if p.distance_to(a+edge*t)<0.75:return false
    return true
