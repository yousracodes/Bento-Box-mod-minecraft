"""Import cube geometry and embedded textures without changing the source bbmodels."""
import base64
import json
import math
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/bentobox'


def rotate(point, origin, angles):
    x, y, z = [point[i] - origin[i] for i in range(3)]
    for axis, angle in enumerate(angles):
        c, s = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        if axis == 0:
            y, z = y*c-z*s, y*s+z*c
        elif axis == 1:
            x, z = x*c+z*s, -x*s+z*c
        else:
            x, y = x*c-y*s, x*s+y*c
    return [x+origin[0], y+origin[1], z+origin[2]]


def convert(path, name):
    model = json.loads(path.read_text(encoding='utf-8'))
    groups = {g['uuid']: g for g in model['groups']}
    cubes = {e['uuid']: e for e in model['elements']}
    lid = next(g for g in groups.values() if g['name'] == 'Lid')
    faces = []
    for i, tex in enumerate(model['textures']):
        target = ASSETS / f'textures/item/{name}_{i}.png'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(base64.b64decode(tex['source'].split(',', 1)[1]))

    def visit(node, ancestors):
        if isinstance(node, dict):
            group = groups[node['uuid']]
            for child in node.get('children', []):
                visit(child, ancestors + [group])
            return
        e = cubes[node]
        if e.get('export', True) is False:
            return
        x0, y0, z0 = e['from']
        x1, y1, z1 = e['to']
        # Each face starts at its texture's top left, counterclockwise from outside.
        corners = {
            'north': [(x1,y1,z0),(x1,y0,z0),(x0,y0,z0),(x0,y1,z0)],
            'south': [(x0,y1,z1),(x0,y0,z1),(x1,y0,z1),(x1,y1,z1)],
            'west': [(x0,y1,z0),(x0,y0,z0),(x0,y0,z1),(x0,y1,z1)],
            'east': [(x1,y1,z1),(x1,y0,z1),(x1,y0,z0),(x1,y1,z0)],
            'up': [(x0,y1,z0),(x0,y1,z1),(x1,y1,z1),(x1,y1,z0)],
            'down': [(x0,y0,z1),(x0,y0,z0),(x1,y0,z0),(x1,y0,z1)]}
        for side, f in e['faces'].items():
            if f.get('texture') is None:
                continue
            tex = model['textures'][int(f['texture'])]
            u0,v0,u1,v1 = f['uv']
            uv = [(u0,v0),(u0,v1),(u1,v1),(u1,v0)]
            turns = int(f.get('rotation',0)/90) % 4
            uv = uv[-turns:] + uv[:-turns] if turns else uv
            points = []
            for p in corners[side]:
                p = rotate(p, e.get('origin',[0,0,0]), e.get('rotation',[0,0,0]))
                for g in reversed(ancestors):
                    if g['name'] != 'Lid':
                        p = rotate(p, g['origin'], g.get('rotation',[0,0,0]))
                points.append(p)
            a = [points[1][j]-points[0][j] for j in range(3)]
            b = [points[2][j]-points[0][j] for j in range(3)]
            normal = [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
            length = math.sqrt(sum(n*n for n in normal))
            if not length:
                continue
            faces.append(dict(lid=any(g['name']=='Lid' for g in ancestors), texture=int(f['texture']),
                normal=[n/length for n in normal], vertices=[p+[u/tex['uv_width'],v/tex['uv_height']] for p,(u,v) in zip(points,uv)]))

    for node in model['outliner']:
        visit(node, [])
    target = ASSETS / f'bento/{name}.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(dict(pivot=lid['origin'],openAngle=lid['rotation'][0],faces=faces),separators=(',',':')))
    print(f'{name}: {len(cubes)} cubes, {len(faces)} faces')


if __name__ == '__main__':
    convert(pathlib.Path(sys.argv[1]), 'classic_bento')
    convert(pathlib.Path(sys.argv[2]), 'deluxe_bento')
