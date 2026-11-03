"""Import the artist's free-format Blockbench model as two independently posed sticks."""
import base64
import json
import math
from pathlib import Path
import sys
from import_models import rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/bentobox'


def convert(path):
    model = json.loads(path.read_text(encoding='utf-8'))
    cubes = {e['uuid']: e for e in model['elements']}
    groups = {}

    def collect(node):
        if isinstance(node, dict):
            groups[node['name']] = node
            for child in node.get('children', []):
                collect(child)

    for node in model['outliner']:
        collect(node)
    for index, texture in enumerate(model['textures']):
        target = ASSETS / f'textures/item/chopsticks_{index}.png'
        target.write_bytes(base64.b64decode(texture['source'].split(',', 1)[1]))

    parts = []
    for prefix, direction in [('L', 1), ('R', -1)]:
        stick = groups[f'{prefix}_Stick']
        pivot = rotate(groups[f'{prefix}_Grip']['origin'], stick['origin'], stick['rotation'])
        tip = rotate(groups[f'{prefix}_Tip']['origin'], stick['origin'], stick['rotation'])
        faces = []

        def visit(node, ancestors):
            if isinstance(node, dict):
                if not node.get('export', True):
                    return
                for child in node.get('children', []):
                    visit(child, ancestors + [node])
                return
            e = cubes[node]
            if not e.get('export', True):
                return
            x0, y0, z0 = e['from']
            x1, y1, z1 = e['to']
            corners = {
                'north': [(x1,y1,z0),(x1,y0,z0),(x0,y0,z0),(x0,y1,z0)],
                'south': [(x0,y1,z1),(x0,y0,z1),(x1,y0,z1),(x1,y1,z1)],
                'west': [(x0,y1,z0),(x0,y0,z0),(x0,y0,z1),(x0,y1,z1)],
                'east': [(x1,y1,z1),(x1,y0,z1),(x1,y0,z0),(x1,y1,z0)],
                'up': [(x0,y1,z0),(x0,y1,z1),(x1,y1,z1),(x1,y1,z0)],
                'down': [(x0,y0,z1),(x0,y0,z0),(x1,y0,z0),(x1,y0,z1)]}
            for side, face in e['faces'].items():
                if face.get('texture') is None:
                    continue
                texture = model['textures'][int(face['texture'])]
                u0, v0, u1, v1 = face['uv']
                uv = [(u0,v0),(u0,v1),(u1,v1),(u1,v0)]
                turns = int(face.get('rotation', 0) / 90) % 4
                if turns:
                    uv = uv[-turns:] + uv[:-turns]
                points = []
                for point in corners[side]:
                    point = rotate(point, e.get('origin', [0,0,0]), e.get('rotation', [0,0,0]))
                    for group in reversed(ancestors):
                        point = rotate(point, group['origin'], group.get('rotation', [0,0,0]))
                    points.append(point)
                a = [points[1][j]-points[0][j] for j in range(3)]
                b = [points[2][j]-points[0][j] for j in range(3)]
                normal = [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]
                length = math.sqrt(sum(n*n for n in normal))
                if length < 1e-10:
                    continue
                faces.append(dict(texture=int(face['texture']), normal=[n/length for n in normal],
                    vertices=[p+[u/texture['uv_width'], v/texture['uv_height']] for p,(u,v) in zip(points,uv)]))

        visit(stick, [])
        assert faces, f'No visible faces in {prefix}_Stick'
        parts.append(dict(pivot=pivot, tip=tip, direction=direction, faces=faces))

    display = groups['Display']
    mesh = dict(displayPivot=display['origin'], displayRotation=display['rotation'], openAngle=12, parts=parts)
    target = ASSETS / 'bento/chopsticks.json'
    target.write_text(json.dumps(mesh, separators=(',', ':')))
    closed = abs(parts[0]['tip'][0] - parts[1]['tip'][0])
    tips = [rotate(p['tip'], p['pivot'], [0,0,p['direction']*mesh['openAngle']]) for p in parts]
    gap = abs(tips[0][0] - tips[1][0])
    assert gap > closed + 2, 'Opening did not spread the tips'
    assert gap - .4 > 2.6, 'Food will not fit between the open tips'
    print(f'Imported {len(cubes)} cubes, {sum(len(p["faces"]) for p in parts)} faces; tip gap {closed:.2f} -> {gap:.2f}')


if __name__ == '__main__':
    convert(Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / 'models/chopsticks-v1-blockbench.bbmodel')
