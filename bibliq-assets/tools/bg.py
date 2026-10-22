"""Section 6: World 1 background segments + World 1 -> 2 transition.

The whole strip is painted on ONE tall canvas (1080 x 7680) and then sliced into
1080 x 1920 segments, so neighbouring segments always line up perfectly.
Stacking order on the map (bottom -> top):
  bg_world1_01, bg_world1_02, bg_world1_03, bg_transition_1_2
"""
import math
import random
from lib import *

W, SEG = 1080, 1920
H = SEG * 4
GRASS = "#8FD16E"; GRASS_L = "#A6DD86"; GRASS_D = "#78BE58"; GRASS_DD = "#5E9F42"
SANDC = "#EDC688"; SANDC_L = "#F5D9A6"; SANDC_D = "#D9AE6A"
WATER = "#7DBDF5"; WATER_L = "#B9DCFA"; BANK = "#E6D29E"
rnd = random.Random(2026)

layers = []
defs = ""


def add(*ls):
    layers.extend(ls)


# ------------------------------------------------------------------ river (Eden stream)
def river_x(y):
    return 150 + 62 * math.sin(y / 620) + 26 * math.sin(y / 233 + 1.3)


river_pts = []
y = H + 80
while y > 1480:
    river_pts.append((river_x(y), y))
    y -= 60
# turn off the left edge inside the transition segment
x0, y0 = river_pts[-1]
river_pts += bez_pts((x0, y0), (x0 + 10, y0 - 220), (40, 1040), (-120, 900), 8)[1:]
river_w = [46 + 7 * math.sin(i * 0.7) for i in range(len(river_pts))]


def near_river(x, y, pad):
    if y < 880:
        return False
    if y > 1480:
        return abs(x - river_x(y)) < 60 + pad
    return min(math.hypot(x - px, y - py) for px, py in river_pts[-9:]) < 60 + pad


def grass_zone(y):
    """1 = full grass, 0 = full sand (transition at the top of the strip)."""
    if y > 1500:
        return 1.0
    if y < 700:
        return 0.0
    return (y - 700) / 800


# ------------------------------------------------------------------ base terrain
add(F(rect(0, 0, W, H), GRASS))
# light / dark meadow patches (grass area only)
for i in range(70):
    x = rnd.uniform(-80, W + 80)
    y = rnd.uniform(1350, H + 80)
    r = rnd.uniform(90, 220)
    c = GRASS_L if i % 2 else GRASS_D
    b = blob(x, y, lambda a, r=r, ph=rnd.random() * 6: r * (1 + 0.12 * math.sin(3 * a + ph)
                                                         + 0.06 * math.sin(5 * a)), 24)
    add(F(b.tf(S(1.5, 0.7, x, y)), c, fa=0.45))

# sand region with a wavy edge in the transition segment
edge = [(-20, 1260)]
for x in range(0, W + 41, 40):
    edge.append((x, 1180 + 70 * math.sin(x / 130 + 0.6) + 40 * math.sin(x / 57)))
edge += [(W + 20, 1260), (W + 20, -20), (-20, -20)]
sand = smooth(edge[:-2], closed=False)
sand_shape = Shape(sand.cmds + [("L", (W + 20, -20)), ("L", (-20, -20)), ("Z",)])
add(F(sand_shape, SANDC))
add(ST(sand, SANDC_D, 10, sa=0.9), ST(sand.tf(T(0, -8)), SANDC_L, 6, sa=0.8))

# grass islands fading into the sand
for i in range(16):
    x = rnd.uniform(60, W - 60)
    y = rnd.uniform(760, 1160)
    r = rnd.uniform(26, 70) * (y - 700) / 460
    if r < 12:
        continue
    b = blob(x, y, lambda a, r=r: r * (1 + 0.15 * math.sin(3 * a + x)), 18).tf(S(1.4, 0.8, x, y))
    add(F(b, GRASS_D, SANDC_D, 4), F(b.tf(S(0.8, 0.7, x, y - 3)), GRASS))

# dunes (sand area)
for i in range(26):
    x = rnd.uniform(0, W)
    y = rnd.uniform(40, 1050)
    ln = rnd.uniform(120, 260)
    add(ST(P(f"M{x-ln/2},{y} Q{x},{y-26} {x+ln/2},{y}"), SANDC_L, 9, sa=0.9),
        ST(P(f"M{x-ln/2+10},{y+9} Q{x},{y-14} {x+ln/2-10},{y+9}"), SANDC_D, 5, sa=0.5))

# ------------------------------------------------------------------ river drawing
add(F(tube(river_pts, [w + 22 for w in river_w]), BANK, GRASS_DD, 5))
add(F(tube(river_pts, river_w), WATER, "#4F8FD0", 4))
add(ST(smooth([(x - 12, y) for x, y in river_pts], closed=False), WATER_L, 7, sa=0.7))
for i in range(0, len(river_pts) - 3, 3):
    x, y = river_pts[i]
    if y < 950:
        continue
    add(ST(P(f"M{x+6},{y-14} Q{x+14},{y-20} {x+22},{y-14}"), WHITE, 3, sa=0.55))
for i in range(0, len(river_pts), 2):
    x, y = river_pts[i]
    side = 1 if i % 4 else -1
    sx = x + side * (river_w[i] + 14)
    sy = y + rnd.uniform(-20, 20)
    if sy < 900:
        continue
    add(F(ellipse(sx, sy, rnd.uniform(7, 12), rnd.uniform(5, 8)), STONE, STONE_LINE, 2.5))

# ------------------------------------------------------------------ small details
def tuft(x, y, s, color):
    return ST(P(f"M{x-6*s},{y-8*s} L{x},{y} L{x+2*s},{y-11*s} M{x},{y} L{x+8*s},{y-7*s}"), color, 2.6 * s, sa=0.75)


for i in range(1400):
    x = rnd.uniform(0, W)
    y = rnd.uniform(0, H)
    if near_river(x, y, 6):
        continue
    if rnd.random() > grass_zone(y):
        if rnd.random() < 0.25:
            add(F(ellipse(x, y, rnd.uniform(3, 6), rnd.uniform(2, 4)), SANDC_D, fa=0.7))
        continue
    add(tuft(x, y, rnd.uniform(0.8, 1.3), GRASS_DD if rnd.random() < 0.7 else GRASS_L))


def flower(x, y, s, col):
    out = []
    for k in range(5):
        a = math.radians(k * 72 - 90)
        out.append(F(circle(x + 5.2 * s * math.cos(a), y + 5.2 * s * math.sin(a), 4 * s), col,
                     "#8A6A4A" if col == WHITE else None, 1.2 if col == WHITE else 1.0))
    out.append(F(circle(x, y, 3 * s), GOLD, GOLD_D, 1.2))
    return out


FLOWER_COLS = [WHITE, WHITE, "#FFD34D", "#F7A9A0", "#C9B6F2"]
for i in range(260):
    x = rnd.uniform(0, W)
    y = rnd.uniform(1300, H)
    if near_river(x, y, 16) or rnd.random() > grass_zone(y):
        continue
    central = 290 < x < 790
    if central and rnd.random() < 0.7:
        continue
    col = rnd.choice(FLOWER_COLS)
    for k in range(rnd.randint(1, 4) if central else rnd.randint(3, 6)):
        add(*flower(x + rnd.uniform(-26, 26), y + rnd.uniform(-18, 18), rnd.uniform(0.8, 1.2), col))


# ------------------------------------------------------------------ bushes + trees (edges only)
def bush(x, y, r):
    pts = [(x + r * 0.55 * math.cos(math.radians(a)), y + r * 0.4 * math.sin(math.radians(a)))
           for a in range(0, 360, 72)]
    shapes = [circle(px, py, r * 0.55) for px, py in pts] + [circle(x, y, r * 0.6)]
    out = [F(ellipse(x + 10, y + r * 0.62, r * 1.05, r * 0.36), INK, fa=0.14)]
    out += outlined(shapes, LEAF_C, LINE_G, 4)
    out += [F(circle(x - r * 0.2, y - r * 0.25, r * 0.38), LEAF_LC, fa=0.8)]
    for k in range(3):
        a = rnd.uniform(0, 6.28)
        out.append(F(circle(x + r * 0.5 * math.cos(a), y + r * 0.35 * math.sin(a), 4.5), CORAL, "#9A2E24", 1.5))
    return out


LEAF_C = "#5FA83A"; LEAF_LC = "#8CCB5E"; LEAF_DC = "#3D7F22"; LINE_G = "#2E5A1A"


def tree(x, y, r, fruit):
    out = [F(ellipse(x + r * 0.35, y + r * 0.42, r * 1.05, r * 0.7), INK, fa=0.16)]
    lobes = []
    for k in range(7):
        a = math.radians(k * 360 / 7 + rnd.uniform(-10, 10))
        lobes.append(circle(x + r * 0.62 * math.cos(a), y + r * 0.55 * math.sin(a), r * 0.48))
    lobes.append(circle(x, y, r * 0.72))
    out += outlined(lobes, LEAF_DC, LINE_G, 5)
    out += [F(circle(x - r * 0.12, y - r * 0.14, r * 0.68), LEAF_C)]
    out += [F(circle(x - r * 0.3, y - r * 0.32, r * 0.34), LEAF_LC, fa=0.9)]
    if fruit:
        for k in range(7):
            a = rnd.uniform(0, 6.28)
            d = rnd.uniform(0.2, 0.7) * r
            out.append(F(ellipse(x + d * math.cos(a), y + d * math.sin(a), 5, 6.5), fruit, "#2E2240", 1.5))
    return out


placed = []
for side in ("L", "R"):
    yy = H - 120
    while yy > 1250:
        if side == "R":
            x = rnd.uniform(930, 1090)
        else:
            x = rnd.uniform(-40, 40)
        r = rnd.uniform(80, 120)
        if not near_river(x, yy, r * 0.6):
            kind = rnd.random()
            fruit = "#5E4A7A" if kind < 0.35 else ("#F4F4F4" if kind < 0.6 else None)
            add(*tree(x, yy, r, fruit))
            placed.append((x, yy))
        yy -= rnd.uniform(380, 560)

for i in range(60):
    y = rnd.uniform(1300, H)
    x = rnd.choice([rnd.uniform(270, 300), rnd.uniform(800, 900)]) if rnd.random() < 0.6 \
        else rnd.uniform(860, 1060)
    if near_river(x, y, 60) or any(math.hypot(x - px, y - py) < 150 for px, py in placed):
        continue
    add(*bush(x, y, rnd.uniform(34, 50)))
    placed.append((x, y))

# ------------------------------------------------------------------ desert details (transition top)
for i in range(18):
    x = rnd.uniform(40, W - 40)
    y = rnd.uniform(60, 1000)
    if 300 < x < 780 and rnd.random() < 0.6:
        continue
    r = rnd.uniform(14, 26)
    b = blob(x, y, lambda a, r=r: r * (1 + 0.1 * math.sin(3 * a + x)), 16).tf(S(1, 0.75, x, y))
    add(F(ellipse(x + 6, y + r * 0.6, r * 1.1, r * 0.35), INK, fa=0.15),
        F(b, "#B9A58A", STONE_LINE, 3), F(b.tf(S(0.7, 0.6, x - 3, y - 5)), "#D6C6AE"))
for i in range(12):
    x = rnd.uniform(40, W - 40)
    y = rnd.uniform(120, 1100)
    if 300 < x < 780:
        continue
    for a in range(-150, -20, 26):
        add(ST(P(f"M{x},{y} Q{x+16*math.cos(math.radians(a+20))},{y+16*math.sin(math.radians(a+20))} "
                 f"{x+30*math.cos(math.radians(a))},{y+30*math.sin(math.radians(a))}"), "#9C8A4E", 3))

# butterflies
for i in range(8):
    x = rnd.uniform(200, 880)
    y = rnd.uniform(1600, H - 100)
    col = rnd.choice([CORAL, GOLD, "#C9B6F2"])
    add(F(ellipse(x - 7, y - 4, 7, 9).tf(R(-25, x - 7, y - 4)), col, LINE, 1.6),
        F(ellipse(x + 7, y - 4, 7, 9).tf(R(25, x + 7, y - 4)), col, LINE, 1.6),
        ST(line((x, y - 10), (x, y + 8)), LINE, 3))

if __name__ == "__main__":
    svg = to_svg(layers, W, H)
    with open(f"{DIRS['svg']}/bg_world1_full_strip.svg", "w") as f:
        f.write(svg)
    full = render(svg, bg=GRASS).convert("RGB")
    full.save("/home/claude/bibliq/cache/bg_full.png")
    names = [("bg_transition_1_2", 0), ("bg_world1_03", 1), ("bg_world1_02", 2), ("bg_world1_01", 3)]
    for n, i in names:
        seg = full.crop((0, i * SEG, W, (i + 1) * SEG))
        q = 80
        path = f"{DIRS['nodpi']}/{n}.webp"
        while True:
            seg.save(path, "WEBP", quality=q, method=6)
            if os.path.getsize(path) < 190 * 1024 or q <= 50:
                break
            q -= 5
        seg.save(f"/home/claude/bibliq/cache/{n}.png")
        print(n, f"{os.path.getsize(path)/1024:.0f} KB  q={q}")
    prev = full.resize((W // 6, H // 6))
    prev.save("/home/claude/bibliq/cache/bg_strip_small.png")
