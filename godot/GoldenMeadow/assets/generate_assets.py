"""Rebuild original procedural textures; Pillow is needed only for this optional step."""
from PIL import Image, ImageDraw, ImageFilter
import random, math
from pathlib import Path
root=Path(__file__).parent
rng=random.Random(706)
def noise(size):
    result=Image.new('L',(size,size),0)
    import PIL.ImageChops as C
    for scale,weight in [(8,55),(32,55),(128,65),(size,80)]:
        small=Image.frombytes('L',(scale,scale),bytes(rng.randrange(256) for _ in range(scale*scale)))
        layer=small.resize((size,size),Image.Resampling.BILINEAR).point(lambda x:x*weight//255)
        result=C.add(result,layer)
    return result
n=noise(512)
n.convert('RGB').save(root/'ground.png')
img=Image.new('RGB',(1024,1024),(44,43,37)); d=ImageDraw.Draw(img)
for row in range(16):
    y=row*64
    offset=-rng.randint(0,120)
    x=offset
    while x<1024:
        w=rng.randint(80,155); c=rng.randint(80,135)
        d.rounded_rectangle((x+2,y+2,x+w-2,y+62),radius=6,fill=(c,int(c*.97),int(c*.87)),outline=(c+12,c+10,c+4),width=2)
        for _ in range(18):
            xx=rng.randint(x+5,x+w-4); yy=rng.randint(y+5,y+60)
            d.line((xx,yy,xx+rng.randint(1,10),yy+rng.randint(1,3)),fill=(c-20,c-21,c-22),width=1)
        x+=w
pix=img.load(); nn=noise(1024).load()
for y in range(1024):
    for x in range(1024):
        c=pix[x,y]; v=(nn[x,y]-110)*.36
        pix[x,y]=tuple(max(0,min(255,int(a+v))) for a in c)
img.save(root/'stone.png')
roof=Image.new('RGB',(512,512),(25,29,33)); d=ImageDraw.Draw(roof)
for row in range(16):
    for col in range(-1,9):
        x=col*64+(row%2)*32; y=row*32; c=rng.randint(42,68)
        d.rectangle((x+1,y+1,x+62,y+30),fill=(c,c+3,c+6))
        d.line((x+2,y+29,x+62,y+29),fill=(14,18,20),width=2)
        d.line((x+2,y+2,x+62,y+2),fill=(c+22,c+25,c+25))
roof.save(root/'slate.png')
