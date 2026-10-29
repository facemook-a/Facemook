"""Original synthesized game audio, CC0. Python + NumPy for optional regeneration."""
from pathlib import Path
import wave, numpy as np
R=22050; root=Path(__file__).parent;rng=np.random.default_rng(3501)
def save(name,a):
 a=np.clip(a,-.95,.95)
 with wave.open(str(root/(name+'.wav')),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(R);w.writeframes((a*32767).astype('<i2').tobytes())
def freq(n):return 440*2**((n-69)/12)
duration=32;music=np.zeros(R*duration)
chords=[[50,57,62,65],[46,53,58,62],[48,55,60,64],[45,52,57,60]]*2
for bar,chord in enumerate(chords):
 start=bar*4
 t=np.arange(4*R)/R
 pad=np.zeros(len(t))
 for note in chord:
  f=freq(note);pad+=(np.sin(2*np.pi*f*t)+.2*np.sin(2*np.pi*f*2*t))*.025
 pad*=np.sin(np.pi*np.minimum(t/4,1))**.6
 music[start*R:(start+4)*R]+=pad
 for k in range(8):
  note=chord[[0,2,1,3,2,1,3,2][k]]+12
  tt=np.arange(int(2*R))/R;f=freq(note)
  piano=(np.sin(2*np.pi*f*tt)+.24*np.sin(2*np.pi*f*2.003*tt)+.10*np.sin(2*np.pi*f*3.99*tt))*np.exp(-tt*2.6)*np.minimum(tt*150,1)*.07
  index=int((start+k*.5)*R);length=min(len(piano),len(music)-index);music[index:index+length]+=piano[:length]
# Cyclic echoes maintain a clean loop.
music+=np.roll(music,int(R*.32))*.16+np.roll(music,int(R*.61))*.08
save('music',music)
t=np.arange(4*R)/R;bird=np.zeros(len(t))
for start,dur,f in [(.2,.15,2400),(.43,.2,3100),(1.4,.28,2200),(2.8,.19,2900),(3.1,.3,2600)]:
 tt=np.arange(int(dur*R))/R;chirp=np.sin(2*np.pi*(f*tt+900*tt**2+30*np.sin(tt*35)))*np.sin(np.pi*tt/dur)**2*.20
 i=int(start*R);bird[i:i+len(chirp)]+=chirp
save('birds',bird)
for name,dur,volume in [('step',.15,.25),('land',.28,.35),('ocean',8,.2)]:
 n=int(dur*R);a=rng.normal(0,1,n);a=np.convolve(a,np.ones(24)/24,mode='same')
 t=np.arange(n)/R
 a*=volume*(np.exp(-t*28) if name!='ocean' else (.65+.25*np.sin(2*np.pi*t/8)))
 save(name,a)
t=np.arange(int(.35*R))/R;save('jump',np.sin(2*np.pi*(230*t-150*t*t))*np.sin(np.pi*t/.35)**2*.06)
t=np.arange(int(.75*R))/R;save('door',(np.sin(2*np.pi*(85*t+15*np.sin(t*3)))+np.sin(2*np.pi*173*t)*.15)*np.sin(np.pi*t/.75)**2*.08)
t=np.arange(int(.5*R))/R;save('interact',(np.sin(2*np.pi*660*t)+.4*np.sin(2*np.pi*990*t))*np.exp(-t*9)*np.minimum(t*100,1)*.12)
