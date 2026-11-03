"""Derive a four-compartment mesh from the imported six-compartment Classic."""
import copy
import json
from pathlib import Path

ASSETS = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/bentobox'


def generate():
    model = json.loads((ASSETS / 'bento/classic_bento.json').read_text())
    assert len(model['faces']) == 34 * 6, 'Classic cube layout changed; review divider indices'
    faces = []
    for cube in range(34):
        # Remove one longitudinal divider and the middle transverse segment.
        if cube in (11, 13):
            continue
        group = copy.deepcopy(model['faces'][cube * 6:(cube + 1) * 6])
        xs = [v[0] for face in group for v in face['vertices']]
        lo, hi = min(xs), max(xs)
        for face in group:
            for vertex in face['vertices']:
                x = vertex[0]
                if cube == 10:
                    x -= 3
                elif lo > 2:
                    x -= 3
                elif hi < -2:
                    x += 3
                elif lo < -3 and hi > 3:
                    x = x * (hi - lo - 6) / (hi - lo)
                # Centered latch hardware retains its original size.
                vertex[0] = round(x, 6)
        faces.extend(group)
    model['faces'] = faces
    (ASSETS / 'bento/compact_bento.json').write_text(json.dumps(model, separators=(',', ':')))
    for texture in (ASSETS / 'textures/item').glob('classic_bento_*.png'):
        (texture.parent / texture.name.replace('classic_', 'compact_')).write_bytes(texture.read_bytes())
    print(f'compact_bento: {len(faces)} faces, four compartments')


if __name__ == '__main__':
    generate()
