"""Section 5: map decorations (WebP with transparency, sized for a 1080 px wide map)."""
import math
import random
from lib import *

OW = 5          # outline width in px
BROWN = "#6A3E14"
SAND = "#E3C08A"; SAND_D = "#C99E62"; SAND_L = "#F0D7A8"
LEAF = "#5FA83A"; LEAF_D = "#3D7F22"; LEAF_DD = "#2E6418"; LEAF_L = "#8CCB5E"


def shadow(cx, cy, rx, ry, a=0.18):
    return F(ellipse(cx, cy, rx, ry), INK, fa=a)


def g_open(clip_id):
    return f'<g clip-path="url(#{clip_id})">'


def svg_layers(layers):
    return "".join(l if isinstance(l, str) else layer_svg(l) for l in layers)


def clip_def(cid, shape):
    return f'<clipPath id="{cid}"><path d="{shape.d()}"/></clipPath>'


def tufts(x, y, s=1.0, n=3, color=OLIVE_L, outline=OLIVE_D):
    out = []
    angs = [-120, -95, -70, -105, -82][:n]
    for i, a in enumerate(angs):
        out.append(F(leaf(x + (i - n / 2) * 4 * s, y, a, (22 + (i % 2) * 8) * s, 4.2 * s),
                     color, outline, 2.2))
    return out


# ------------------------------------------------------------------ ribbon
def ribbon():
    W, H = 900, 230
    band = P("M120,52 Q450,18 780,52 L780,168 Q450,134 120,168 Z")
    tail_l = poly([(170, 92), (18, 100), (72, 150), (18, 200), (170, 206)])
    fold_l = poly([(120, 168), (170, 206), (170, 152)])
    out = [shadow(450, 212, 330, 10, 0.12)]
    for m in [(), (S(-1, 1, 450, 0),)]:
        out += [F(tail_l.tf(*m), PARCH_D, LINE, OW),
                ST(line((150, 118), (60, 120)).tf(*m), PARCH_DD, 3),
                ST(line((150, 182), (60, 182)).tf(*m), PARCH_DD, 3),
                F(fold_l.tf(*m), PARCH_DD, LINE, OW)]
    out += [F(band, PARCH, LINE, OW),
            ST(P("M140,66 Q450,33 760,66"), GOLD, 6),
            ST(P("M140,154 Q450,120 760,154"), GOLD, 6),
            ST(P("M150,80 Q450,49 750,80"), WHITE, 4, sa=0.6)]
    return W, H, out, ""


# ------------------------------------------------------------------ signpost
def signpost():
    W, H = 300, 360
    board = rrect(18, 42, 264, 150, 16).tf(R(-3, 150, 117))
    out = [shadow(150, 340, 70, 12),
           F(rrect(134, 150, 32, 196, 6), WOOD, LINE, OW),
           ST(line((144, 170), (144, 330)), WOOD_D, 3),
           F(board.tf(T(0, 8)), WOOD_D, LINE, OW),
           F(board, WOOD, LINE, OW)]
    for y in (82, 118, 154):
        out.append(ST(P(f"M40,{y} C110,{y-6} 190,{y+6} 262,{y-2}").tf(R(-3, 150, 117)), WOOD_D, 3))
    out.append(ST(P("M44,60 L250,48").tf(R(-1, 150, 117)), WOOD_L, 4))
    for x, y in [(40, 64), (258, 54), (44, 172), (262, 162)]:
        out += [F(circle(x, y, 6), GREY_D, LINE, 2.5), F(circle(x - 1.5, y - 1.5, 2), WHITE, fa=0.7)]
    out += tufts(122, 344, 1.0) + tufts(186, 346, 0.9)
    return W, H, out, ""


# ------------------------------------------------------------------ gate walls
def merlons(x0, x1, y, h, w, gap):
    out = []
    x = x0
    while x + w <= x1 + 0.1:
        out.append(rect(x, y - h, w, h + 6))
        x += w + gap
    return out


def bricks(x0, y0, x1, y1, bw, bh, color, sw=2.5, seed=1):
    rnd = random.Random(seed)
    out = []
    y = y0
    row = 0
    while y < y1:
        out.append(ST(line((x0, y), (x1, y)), color, sw, sa=0.55))
        x = x0 + (bw / 2 if row % 2 else 0) + rnd.uniform(-8, 8)
        while x < x1:
            if x > x0 + 4:
                out.append(ST(line((x, y), (x, min(y + bh, y1))), color, sw, sa=0.55))
            x += bw + rnd.uniform(-10, 10)
        y += bh
        row += 1
    return out


def gate_walls():
    W, H = 1080, 560
    out = []
    defs = ""
    wall_top = 250
    # main wall
    wall = rect(-10, wall_top, 1100, H - wall_top + 20)
    out += [F(wall, SAND, LINE, OW)]
    defs += clip_def("wallc", wall)
    out += [g_open("wallc")] + bricks(-10, wall_top + 40, 1100, H, 84, 44, SAND_D, seed=3) + ["</g>"]
    out += [F(m, SAND, LINE, OW) for m in merlons(-20, 1100, wall_top + 2, 40, 46, 30)]
    out += [F(rect(-10, wall_top - 2, 1100, 14), SAND_D, LINE, OW)]
    # towers
    for tx in (318, 642):
        tower = rect(tx, 110, 120, H - 90)
        out += [F(tower, SAND_L, LINE, OW)]
        defs += clip_def(f"t{tx}", tower)
        out += [g_open(f"t{tx}")] + bricks(tx, 150, tx + 120, H, 60, 40, SAND_D, seed=tx) + \
               [F(rect(tx + 86, 110, 40, H), SAND_D, fa=0.35), "</g>"]
        out += [F(m, SAND_L, LINE, OW) for m in merlons(tx - 8, tx + 128, 112, 34, 30, 16)]
        out += [F(rect(tx - 8, 106, 136, 16), SAND_D, LINE, OW),
                F(rrect(tx + 50, 170, 20, 54, 10), "#5A3A26", LINE, 3),
                F(rrect(tx + 50, 300, 20, 54, 10), "#5A3A26", LINE, 3)]
    # gate arch + doors
    arch = P("M450,560 L450,350 C450,280 500,250 540,250 C580,250 630,280 630,350 L630,560 Z")
    out += [F(arch.tf(S(1.13, 1.06, 540, 560)), SAND_D, LINE, OW)]
    for i in range(9):
        a = math.radians(180 + i * 22.5)
        cx, cy = 540, 350
        out.append(ST(line((cx + 92 * math.cos(a), cy + 98 * math.sin(a)),
                           (cx + 104 * math.cos(a), cy + 112 * math.sin(a))), LINE, 3))
    out += [F(arch, WOOD, LINE, OW)]
    defs += clip_def("door", arch)
    out += [g_open("door")]
    for x in range(462, 630, 24):
        out.append(ST(line((x, 240), (x, 560)), WOOD_D, 3))
    for y in (330, 420, 505):
        out += [F(rect(440, y, 200, 16), GREY_D, LINE, 3)]
        for x in range(458, 630, 34):
            out.append(F(circle(x, y + 8, 4), GREY_L, LINE, 1.5))
    out += [ST(line((540, 240), (540, 560)), LINE, 5), "</g>"]
    out += [F(circle(522, 455, 9), GOLD, BROWN, 3), F(circle(558, 455, 9), GOLD, BROWN, 3)]
    # greenery at base
    for x in (60, 220, 290, 790, 860, 1010):
        out += tufts(x, 556, 1.3)
    return W, H, out, defs


# ------------------------------------------------------------------ treasure chests
def chest(opened):
    W, H = 340, 300
    out = []
    defs = ('<radialGradient id="glow" cx="50%" cy="50%" r="50%">'
            '<stop offset="0" stop-color="#FFE7A3" stop-opacity="0.85"/>'
            '<stop offset="1" stop-color="#FFE7A3" stop-opacity="0"/></radialGradient>')
    out.append(f'<ellipse cx="170" cy="{150 if opened else 170}" rx="{170 if opened else 150}" '
               f'ry="{150 if opened else 120}" fill="url(#glow)"/>')
    if opened:
        for a in (-150, -120, -90, -60, -30):
            t1, t2 = math.radians(a - 6), math.radians(a + 6)
            out.append(F(poly([(170, 150), (170 + 160 * math.cos(t1), 150 + 160 * math.sin(t1)),
                               (170 + 160 * math.cos(t2), 150 + 160 * math.sin(t2))]), GOLD_L, fa=0.55))
    mound_back = ellipse(170, 238, 150, 40)
    out += [F(mound_back, "#A9794A", LINE, OW)]
    body = rrect(62, 132, 216, 108, 10)
    if opened:
        lid = P("M66,132 L274,132 L292,62 C260,40 80,40 48,62 Z")
        out += [F(lid, WOOD_D, LINE, OW), F(P("M80,124 L260,124 L272,74 C240,58 100,58 68,74 Z"), "#5A3420")]
        rnd = random.Random(4)
        coins = []
        for i in range(26):
            x = 92 + rnd.random() * 156
            y = 118 + rnd.random() * 26 - (1 - abs(x - 170) / 80) * 24
            coins.append((x, y))
        coins.sort(key=lambda p: p[1])
        for x, y in coins:
            out += [F(ellipse(x, y, 15, 9), GOLD, BROWN, 3), F(ellipse(x, y - 1, 9, 4.5), GOLD_L, fa=0.8)]
    out += [F(body, WOOD, LINE, OW)]
    for y in (168, 204):
        out.append(ST(line((66, y), (274, y)), WOOD_D, 3))
    if not opened:
        lid = P("M62,134 L278,134 L278,104 C278,62 238,46 170,46 C102,46 62,62 62,104 Z")
        out += [F(lid, WOOD, LINE, OW), ST(P("M70,104 C74,74 110,60 170,60 C230,60 266,74 270,104"), WOOD_L, 4),
                ST(line((64, 112), (276, 112)), WOOD_D, 3)]
        bands_top = 50
    else:
        bands_top = 132
    for x in (100, 240):
        out.append(F(rrect(x - 12, bands_top, 24, 240 - bands_top, 4), GOLD, BROWN, 3.5))
    out += [F(rrect(150, 120, 40, 46, 8), GOLD, BROWN, 3.5),
            F(P("M170,134 a6,6 0 1,0 0.1,0 Z") if False else circle(170, 138, 5.6), "#5A3420"),
            F(poly([(166, 141), (174, 141), (172, 154), (168, 154)]), "#5A3420")]
    mound_front = P("M10,250 C40,214 92,212 120,226 C150,240 190,240 220,226 C250,212 300,214 330,250 "
                    "C300,282 40,282 10,250 Z")
    out += [F(mound_front, "#B98A58", LINE, OW),
            ST(P("M60,250 C90,236 120,240 140,246"), "#D5A774", 4),
            F(circle(250, 252, 7), "#8E6438"), F(circle(96, 262, 5), "#8E6438")]
    out += [F(sparkle(46, 70, 16), GOLD_L, GOLD_D, 3), F(sparkle(300, 96, 11), GOLD_L, GOLD_D, 2.5)]
    if opened:
        out += [F(sparkle(84, 30, 12), GOLD_L, GOLD_D, 2.5), F(sparkle(262, 26, 15), GOLD_L, GOLD_D, 3)]
    return W, H, out, defs


# ------------------------------------------------------------------ tent
def tent():
    W, H = 400, 300
    shape = P("M18,250 L70,112 C110,96 150,74 196,66 C246,74 290,96 330,112 L382,250 Z")
    out = [shadow(200, 262, 196, 22, 0.18)]
    defs = clip_def("tent", shape)
    out += [F(shape, "#E8D6B0", LINE, OW), g_open("tent")]
    for x in range(-20, 420, 44):
        out.append(F(rect(x, 40, 22, 240), "#7A5338"))
    out += [F(rect(0, 230, 400, 30), INK, fa=0.15),
            F(P("M18,250 L70,112 C110,96 150,74 196,66 L196,90 C150,98 110,120 74,134 Z"), WHITE, fa=0.12),
            "</g>"]
    out += [F(shape, None, LINE, OW)]
    door = P("M160,250 L182,140 L218,140 L240,250 Z")
    out += [F(door, "#3A2618", LINE, OW),
            F(P("M218,140 L240,250 L262,250 C250,210 236,170 218,140 Z"), "#C7A97A", LINE, 4)]
    for x, top in [(70, 112), (196, 66), (330, 112)]:
        out.append(ST(line((x, top - 18), (x, top + 4)), WOOD_D, 7))
    for a, b in [((70, 112), (8, 268)), ((330, 112), (392, 268))]:
        out += [ST(line(a, b), "#8C7A5E", 3), F(rect(b[0] - 4, b[1] - 10, 8, 14), WOOD_D, LINE, 2)]
    return W, H, out, defs


# ------------------------------------------------------------------ palm
def palm():
    W, H = 400, 500
    out = [shadow(200, 482, 74, 12, 0.2)]
    pts = bez_pts((210, 482), (226, 380), (170, 270), (194, 170), 12)
    ws = [20 - 7 * i / 11 for i in range(12)]
    out += [F(tube(pts, ws), "#9C7148", LINE, OW)]
    for i in range(1, 11):
        (x, y), w = pts[i], ws[i]
        out.append(ST(P(f"M{x-w},{y-4} Q{x},{y+6} {x+w},{y-4}"), "#6E4C2C", 3))
    cx, cy = 194, 166
    # dates
    for i, (dx, dy) in enumerate([(-14, 18), (-4, 24), (8, 18), (-10, 30), (2, 34), (14, 28), (-18, 28)]):
        out.append(F(ellipse(cx + dx, cy + dy, 7, 9), "#C9622E", "#6A2E14", 2.5))
    fronds = [(168, 165, 1), (12, 165, -1), (-160, 175, 1), (-20, 175, -1),
              (-128, 150, 1), (-55, 150, -1), (-92, 118, -1)]
    for ang, ln, curl in fronds:
        a = math.radians(ang)
        droop = 70 if abs(math.cos(a)) > 0.5 else 20
        tip = (cx + ln * math.cos(a), cy + ln * math.sin(a) + droop)
        mid = (cx + ln * 0.55 * math.cos(a), cy + ln * 0.55 * math.sin(a) - 30)
        c = bez_pts((cx, cy), mid, mid, tip, 10)
        ws = [3 + 24 * math.sin(math.pi * i / 9) ** 0.8 for i in range(10)]
        ws[-1] = 1
        out.append(F(tube(c, ws), LEAF, LEAF_DD, 4))
        out.append(ST(smooth(c, closed=False), LEAF_DD, 3))
        for i in range(2, 9):
            x, y = c[i]
            nx, ny = c[i + 1][0] - c[i - 1][0], c[i + 1][1] - c[i - 1][1]
            ln2 = math.hypot(nx, ny) or 1
            px, py = -ny / ln2, nx / ln2
            w = ws[i] * 0.9
            out.append(ST(line((x, y), (x + px * w + nx / ln2 * 6, y + py * w + ny / ln2 * 6)), LEAF_D, 2))
    out.append(F(circle(cx, cy, 10), "#7A5534", LINE, 3))
    return W, H, out, ""


# ------------------------------------------------------------------ rocks
def rocks():
    W, H = 260, 180
    out = [shadow(130, 158, 116, 14, 0.2)]
    for cx, cy, r, seed in [(88, 116, 52, 1), (178, 124, 40, 2), (132, 142, 30, 3)]:
        rnd = random.Random(seed)
        ph = rnd.random() * 6
        sh = blob(cx, cy, lambda a, r=r, ph=ph: r * (1 + 0.08 * math.sin(3 * a + ph) + 0.05 * math.sin(5 * a)), 36)
        sh = sh.tf(S(1, 0.78, cx, cy))
        out += [F(sh, "#A99E8C", STONE_LINE, OW),
                F(sh.tf(S(0.84, 0.76, cx - r * 0.12, cy - r * 0.2)), "#CFC6B6"),
                ST(arc(cx - r * 0.1, cy - r * 0.12, r * 0.55, 200, 250, r * 0.4), WHITE, 4, sa=0.7)]
    out += tufts(30, 160, 1.0) + tufts(228, 160, 0.9)
    return W, H, out, ""


# ------------------------------------------------------------------ clouds
def cloud(variant):
    specs = {
        1: (380, 170, [(90, 110, 52), (160, 80, 62), (240, 92, 56), (300, 116, 42), (190, 120, 50)]),
        2: (300, 140, [(80, 92, 42), (140, 66, 50), (204, 82, 46), (246, 104, 32), (150, 100, 40)]),
        3: (440, 190, [(80, 128, 46), (150, 92, 60), (232, 74, 70), (316, 100, 58), (378, 130, 38),
                       (240, 128, 52)]),
    }
    W, H, circles = specs[variant]
    base_y = max(cy + r * 0.45 for _, cy, r in circles)
    shapes = [circle(*c) for c in circles] + \
             [rrect(circles[0][0] - circles[0][2] * 0.7, base_y - 34,
                    circles[-2 if variant != 3 else -2][0], 40, 20)]
    shapes[-1] = rrect(circles[0][0] - 20, base_y - 40,
                       max(c[0] for c in circles) - circles[0][0] + 40, 40, 20)
    body = shapes[0]
    for s_ in shapes[1:]:
        body = body + s_
    defs = clip_def(f"cl{variant}", body)
    out = [F(body, "#A9CBEF", "#A9CBEF", 10),
           F(body, "#DCEBFB"),
           g_open(f"cl{variant}"),
           F(body.tf(T(0, -16)), WHITE),
           "</g>"]
    for cx, cy, r in circles[1:3]:
        out.append(ST(arc(cx, cy, r * 0.7, 200, 260), WHITE, 6, sa=0.9))
    return W, H, out, defs


DECOS = [
    ("ribbon_world", ribbon, 900),
    ("sign_wood", signpost, 300),
    ("gate_walls", gate_walls, 1080),
    ("chest_treasure_closed", lambda: chest(False), 340),
    ("chest_treasure_open", lambda: chest(True), 340),
    ("deco_tent", tent, 400),
    ("deco_palm", palm, 400),
    ("deco_rock", rocks, 260),
    ("deco_cloud_1", lambda: cloud(1), 380),
    ("deco_cloud_2", lambda: cloud(2), 300),
    ("deco_cloud_3", lambda: cloud(3), 440),
]

if __name__ == "__main__":
    sizes = {}
    for name, fn, outw in DECOS:
        W, H, layers, defs = fn()
        img, size = export_raster(name, layers, W, H, outw, defs=defs, quality=90)
        sizes[name] = (img.size, size)
    for k, v in sizes.items():
        print(k, v[0], f"{v[1]/1024:.1f} KB")
    sheet([n for n, _, _ in DECOS], "/home/claude/bibliq/cache/sheet_deco.png", cell=260, cols=4,
          small=False, title="Map decorations", bg="#CFE6FF")
