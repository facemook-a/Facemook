"""Original geometric B5 interface icons. Rebuild without third-party packages."""
from pathlib import Path
root=Path(__file__).parent/'icons'
paths={
'map':'<path d="m3 5 6-2 6 2 6-2v16l-6 2-6-2-6 2zM9 3v16M15 5v16"/>',
'journal':'<path d="M5 3h13a2 2 0 0 1 2 2v16H6a2 2 0 0 1-2-2V5a2 2 0 0 1 1-2zM4 17h16M8 7h8M8 11h6"/>',
'menu':'<path d="M5 6h14M5 12h14M5 18h14"/>',
'settings':'<path d="M4 7h16M4 17h16"/><circle cx="9" cy="7" r="2.5" fill="#142d2c"/><circle cx="15" cy="17" r="2.5" fill="#142d2c"/>',
'camera':'<path d="M8 5 9.5 3h5L16 5h4a1 1 0 0 1 1 1v13H3V6a1 1 0 0 1 1-1z"/><circle cx="12" cy="12" r="4"/>',
'run':'<circle cx="15" cy="4" r="2"/><path d="m12 8 3 2 4-1M12 8l-3 5 4 3-3 5M13 16l5 4M10 8l-4 2-3-1"/>',
'jump':'<path d="M12 17V3m-5 5 5-5 5 5M4 17v4h16v-4"/>',
'interact':'<path d="M8 12V5a2 2 0 0 1 4 0v6-3a2 2 0 0 1 4 0v3-1a2 2 0 0 1 4 0v6c0 3-3 5-6 5h-2c-2 0-3-1-4-2l-5-6a2 2 0 0 1 3-2l2 1z"/>',
'home':'<path d="m3 10 9-7 9 7M5 9v12h5v-7h4v7h5V9"/>',
'wave':'<path d="M2 17c3-4 5 4 8 0s5 4 8 0 4 0 4 0M2 11c3-4 5 4 8 0s5 4 8 0 4 0 4 0"/><path d="m12 3 3 3-3 3"/>',
'play':'<path d="m8 4 12 8-12 8z"/>',
'save':'<path d="M4 3h13l4 4v14H3V3zM7 3v6h10V3M7 21v-7h10v7"/>',
'close':'<path d="m6 6 12 12M18 6 6 18"/>',
'help':'<circle cx="12" cy="12" r="9"/><path d="M9 8a3 3 0 0 1 6 1c0 2-3 2-3 5M12 17h.01"/>',
'volume':'<path d="m3 9 5 0 5-5v16l-5-5H3zM17 8c3 2 3 6 0 8M20 5c5 4 5 10 0 14"/>',
'leaf':'<path d="M5 19C-1 9 7 2 21 3c0 14-8 22-16 16zm0 0L16 8"/>',
}
for name,body in paths.items():
 (root/(name+'.svg')).write_text('<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"><g fill="none" stroke="#eee9da" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">'+body+'</g></svg>')
(root/'knob.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18"><circle cx="9" cy="9" r="7" fill="#e7e2d3" stroke="#72ad98" stroke-width="2"/></svg>')
