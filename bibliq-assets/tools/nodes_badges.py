"""Sections 3 + 4: level nodes, footprint, rank + achievement badges (Vector Drawables)."""
import math
from lib import *

BROWN = "#6A3E14"


# ------------------------------------------------------------------ helpers
def clip_seg(p0, p1, poly_pts):
    """Cyrus-Beck clip of a segment to a convex polygon (clockwise or ccw)."""
    t0, t1 = 0.0, 1.0
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    n = len(poly_pts)
    cx = sum(p[0] for p in poly_pts) / n
    cy = sum(p[1] for p in poly_pts) / n
    for i in range(n):
        a, b = poly_pts[i], poly_pts[(i + 1) % n]
        ex, ey = b[0] - a[0], b[1] - a[1]
        nx, ny = ey, -ex
        if nx * (cx - a[0]) + ny * (cy - a[1]) < 0:
            nx, ny = -nx, -ny                       # inward normal
        num = nx * (p0[0] - a[0]) + ny * (p0[1] - a[1])
        den = nx * dx + ny * dy
        if den == 0:
            if num < 0:
                return None
            continue
        t = -num / den
        if den > 0:
            t0 = max(t0, t)
        else:
            t1 = min(t1, t)
        if t0 > t1:
            return None
    return (p0[0] + t0 * dx, p0[1] + t0 * dy), (p0[0] + t1 * dx, p0[1] + t1 * dy)


def grass_tuft(x, y, s=1.0, flip=False):
    sh = []
    for ang, ln in [(-112, 6.5), (-90, 8.5), (-68, 6.0)]:
        a = 180 - ang if flip else ang
        sh.append(leaf(x, y, a, ln * s, 1.3 * s))
    return [F(s_, OLIVE_L, OLIVE_D, 0.8) for s_ in sh]


def stone_shape(cx, cy, r):
    return blob(cx, cy, lambda a: r * (1 + 0.05 * math.sin(3 * a + 0.5)
                                       + 0.03 * math.sin(5 * a)), 40).tf(S(1, 0.9, cx, cy))


# ------------------------------------------------------------------ nodes
def node_stone(current=False):
    out = [F(ellipse(32, 56.5, 21, 4.2), INK, fa=0.22)]
    r = 21.5 if current else 23.5
    cx, cy = 32, 32
    base = STONE_L if current else STONE
    dark = "#CFC8BB" if current else STONE_D
    st = stone_shape(cx, cy, r)
    out += [F(st, dark, STONE_LINE, 2.2),
            F(stone_shape(cx, cy, r).tf(S(0.9, 0.86, cx - 2, cy - 4)), base),
            F(ellipse(cx, cy - 1, r * 0.6, r * 0.5), WHITE, fa=0.55 if current else 0.35),
            ST(arc(cx, cy, r * 0.78, 200, 250, r * 0.7), WHITE, 2.2, sa=0.8),
            F(circle(cx + r * 0.55, cy + r * 0.45, 1.4), STONE_D, fa=0.7),
            F(circle(cx - r * 0.62, cy + r * 0.38, 1.0), STONE_D, fa=0.7)]
    out += grass_tuft(cx - r * 0.82, 52.5, 0.9) + grass_tuft(cx + r * 0.85, 53, 0.8, True)
    if current:
        out = [F(circle(32, 32, 30.4), ROYAL, fa=0.18)] + out
        out += [ST(circle(32, 32, 28.6), ROYAL, 3.4), ST(circle(32, 32, 26.4), GOLD, 1.2)]
    return out


def node_scroll_locked():
    out = [F(ellipse(32, 50, 23, 3.6), INK, fa=0.2)]
    body = rrect(10, 22.5, 44, 21, 10.5)
    out += [F(body, PARCH_D, LINE, 2.0),
            ST(line((14, 28), (50, 28)), PARCH, 1.6, sa=0.9),
            ST(line((14, 38.4), (50, 38.4)), PARCH_DD, 1.4)]
    for x in (13.6, 50.4):
        out += [F(ellipse(x, 33, 4.2, 10.5), PARCH_DD, LINE, 1.8),
                ST(P(f"M{x},{29.2} C{x+2.6},{29.6} {x+2.6},{36.4} {x},{36.8} "
                     f"C{x-1.6},{36.6} {x-1.6},{31.6} {x},{31.6}"), "#B89A6A", 1.1)]
    tails = [poly([(29, 36), (32, 38), (28.4, 49), (26.6, 46.6), (24.6, 47.8)]),
             poly([(35, 36), (32, 38), (35.6, 49), (37.4, 46.6), (39.4, 47.8)])]
    out += [F(t, "#A9473C", LINE, 1.3) for t in tails]
    seal = blob(32, 33, lambda a: 7.6 + 0.7 * math.sin(8 * a), 48)
    out += [F(seal.tf(T(0, 1)), SEAL_D, LINE, 1.6), F(seal, SEAL, LINE, 1.6),
            ST(circle(32, 33, 4.6), SEAL_L, 1.2),
            ST(arc(32, 33, 6, 200, 250), "#F08A7E", 1.2)]
    return out


def laurel(cx, cy, r, a0, a1, side, n=6, color=GOLD, outline=BROWN, size=1.0):
    out = []
    stem = arc(cx, cy, r, a0, a1)
    out.append(ST(stem, outline, 1.6))
    for i in range(n):
        a = math.radians(a0 + (a1 - a0) * (i + 0.5) / n)
        x, y = cx + r * math.cos(a), cy + r * math.sin(a)
        tang = math.degrees(a) + (90 if a1 > a0 else -90)
        out.append(F(leaf(x, y, tang + 40 * side, 6.4 * size, 2.2 * size), color, outline, 1.0))
        out.append(F(leaf(x, y, tang - 40 * side, 6.0 * size, 2.0 * size), color, outline, 1.0))
    return out


def node_stone_boss():
    cx, cy = 40, 40
    out = [F(ellipse(40, 72, 26, 5), INK, fa=0.24)]
    out += [L(ring(cx, cy, 33.5, 28.2), fill=GOLD, stroke=BROWN, sw=2.0, evenodd=True),
            ST(arc(cx, cy, 30.8, 200, 260), GOLD_L, 1.8)]
    for a in range(0, 360, 30):
        t = math.radians(a + 15)
        out.append(F(circle(cx + 30.8 * math.cos(t), cy + 30.8 * math.sin(t), 0.9), GOLD_D))
    out += laurel(cx, cy, 35.6, 100, 205, 1, 6, size=1.3) + laurel(cx, cy, 35.6, 80, -25, -1, 6, size=1.3)
    st = stone_shape(cx, cy, 25)
    out += [F(st, "#A79C8B", STONE_LINE, 2.4),
            F(stone_shape(cx, cy, 25).tf(S(0.9, 0.86, cx - 2, cy - 4)), "#C9C0B0"),
            F(ellipse(cx, cy - 1, 15.5, 12.6), WHITE, fa=0.35),
            ST(arc(cx, cy, 19.5, 200, 250, 17.5), WHITE, 2.4, sa=0.8)]
    out += [F(star(cx, 6.4, 6.4, 2.9, round_=0.35), GOLD, BROWN, 1.4)]
    return out


def footprint():
    pts = [(12, 2.2), (15.2, 3.4), (16.6, 7), (16.2, 11), (14.6, 14), (14.4, 17),
           (15, 19.4), (13.6, 22), (10.6, 22.2), (9, 19.6), (9.4, 16.6), (9, 13.6),
           (7.6, 10.4), (7.6, 6.2), (9, 3.2)]
    sole = smooth(pts)
    return [F(sole, LINE, fa=0.32),
            ST(line((9.4, 9.6), (15.6, 8.4)), LINE, 1.0, sa=0.25),
            ST(line((10.2, 16.4), (14.4, 16.4)), LINE, 1.0, sa=0.25)]


# ------------------------------------------------------------------ badge frames
def rank_frame():
    out = [F(circle(32, 32, 30), GOLD, BROWN, 2.0)]
    for a in range(0, 360, 30):
        t = math.radians(a)
        out.append(F(circle(32 + 27.4 * math.cos(t), 32 + 27.4 * math.sin(t), 0.9), GOLD_D))
    out += [ST(arc(32, 32, 27.4, 195, 255), GOLD_L, 1.8),
            F(circle(32, 32, 24.6), ROYAL, ROYAL_D, 1.4),
            ST(circle(32, 32, 22.2), ROYAL_L, 1.0, sa=0.7),
            F(ellipse(32, 24, 17, 10), WHITE, fa=0.07)]
    return out


ACH_C = (32, 28)


def ach_frame():
    cx, cy = ACH_C
    tails = [poly([(22, 44), (29, 46), (24.6, 62), (21.6, 58.4), (17.4, 60.4)]),
             poly([(42, 44), (35, 46), (39.4, 62), (42.4, 58.4), (46.6, 60.4)])]
    out = [F(t, CORAL, CORAL_D, 1.6) for t in tails]
    out += [F(circle(cx, cy, 26.5), GOLD, BROWN, 2.0)]
    for a in range(0, 360, 30):
        t = math.radians(a)
        out.append(F(circle(cx + 24.2 * math.cos(t), cy + 24.2 * math.sin(t), 0.8), GOLD_D))
    out += [ST(arc(cx, cy, 24.2, 195, 255), GOLD_L, 1.6),
            F(circle(cx, cy, 21.6), PARCH, PARCH_DD, 1.2)]
    return out


def place(layers_fn, m):
    """Run an object builder in 0..64 space centred on (32,32), then transform."""
    return [dict(l, d=P(l["d"]).tf(*m).d(), sw=l["sw"] * m_scale(m)) for l in layers_fn()]


def m_scale(m):
    mm = chain(*m)
    return math.sqrt(abs(mm[0] * mm[4] - mm[1] * mm[3]))


ACH_M = (S(0.84, 0.84, 32, 32), T(0, -4))


# ------------------------------------------------------------------ rank objects
def sole_shape(cx, cy, s, ang):
    pts = [(0, -12), (3.6, -10.6), (5, -6), (4.6, -1), (3.4, 2.6), (3.4, 6.6),
           (3.8, 9.6), (1.8, 12.6), (-2, 12.6), (-3.8, 9.6), (-3.6, 6), (-4, 2),
           (-5.2, -2.6), (-5, -7.4), (-3.4, -10.8)]
    return smooth(pts).tf(S(s), R(ang), T(cx, cy))


def obj_sandals():
    out = []
    for cx, cy, ang in [(25.4, 33.5, -14), (38.8, 30.5, 12)]:
        out += [F(sole_shape(cx, cy, 1.05, ang), WOOD_D, LINE, 1.6),
                F(sole_shape(cx, cy, 0.86, ang).tf(T(-0.4, -0.6)), WOOD_L)]
        m = (R(ang), T(cx, cy))
        out += [ST(P("M-5.2,-5.6 L4.8,-1.4").tf(*m), "#7A4320", 2.6),
                ST(P("M5,-6 L-4.8,-1.2").tf(*m), "#7A4320", 2.6),
                ST(P("M-4.4,5.2 C-2,3.2 2,3.2 4.4,5.2").tf(*m), "#7A4320", 2.4)]
    return out


def obj_net():
    cx, top = 32, 19.5
    region = [(cx + 17 * math.cos(math.radians(a)), top + 0 + 19 * math.sin(math.radians(a)))
              for a in range(0, 181, 10)]
    out = [F(poly(region), ROYAL_D, fa=0.6)]
    fish = (P("M0,-4 C6,-4.6 10,-1.6 11,0 C10,1.6 6,4.6 0,4 C-3,3.6 -5,2 -6,0 "
              "C-5,-2 -3,-3.6 0,-4 Z") +
            P("M-5.4,0 L-11,-4.6 C-10,-1.6 -10,1.6 -11,4.6 Z")).tf(R(-12), T(32, 30))
    out += [F(fish, "#DCE6EE", "#40546A", 1.3),
            F(circle(39.6, 27.8, 1.1).tf(), INK)]
    for k in range(-40, 50, 6):
        for d in (1, -1):
            seg = clip_seg((cx + k - 30 * d, top - 30), (cx + k + 30 * d, top + 30), region)
            if seg:
                out.append(ST(line(*seg), PARCH, 1.0, sa=0.85))
    out.append(ST(arc(cx, top, 17, 0, 180, 19), PARCH_D, 1.8))
    out.append(ST(line((12, top), (52, top)), WOOD_L, 2.6))
    for x in (17, 27, 37, 47):
        out.append(F(ellipse(x, top, 3, 2.2), GOLD, BROWN, 1.2))
    out += [F(fish.tf(), "#DCE6EE", fa=0.0)]
    return out


def obj_quill():
    m = (R(-16, 32, 32),)
    paper = rrect(19, 21, 26, 22, 2).tf(*m)
    out = [F(paper, PARCH, LINE, 1.6)]
    for i, wl in enumerate([17, 13, 16]):
        y = 27.5 + i * 5
        out.append(ST(line((22.5, y), (22.5 + wl, y)).tf(*m), PARCH_DD, 1.6))
    for y in (18, 42.4):
        out.append(F(rrect(16.4, y, 31.2, 4.6, 2.3).tf(*m), PARCH_D, LINE, 1.5))
    q = leaf(19, 49, -58, 31, 5.2)
    out += [F(q, WHITE, LINE, 1.5),
            ST(line((19, 49), (35.4, 22.6)), "#C9C2B8", 1.0),
            F(poly([(17.6, 51.6), (19.6, 47.4), (21.2, 48.6)]), INK)]
    for i in range(4):
        t = 0.35 + i * 0.13
        x, y = 19 + 16.4 * t * 1.9 * 0.5, 49 - 26.4 * t * 0.95
        out.append(ST(line((x, y), (x + 3.2, y + 0.8)), "#D9D3CA", 0.9))
    return out


def obj_staff_mantle():
    crook = P("M28,52 L28,19 C28,11 38.6,10.4 38.6,17.2 C38.6,20.4 35.6,21.4 34.4,19.4")
    mantle = P("M27,22 C30,19.6 33.6,19.8 36,22.6 C40,27 44,34 46.4,42.2 "
               "C44.6,43.6 42.4,42.2 40.8,44 C39,45.6 37,43.6 35.4,45 "
               "C33.8,40 31.4,32.6 27,28 Z")
    out = [ST(crook, LINE, 5.6), ST(crook, WOOD, 3.4), ST(crook.tf(T(-0.5, -0.4)), WOOD_L, 1.0),
           F(mantle, CORAL, CORAL_D, 1.6),
           ST(P("M33,25.6 C36.2,30.6 38.6,36.6 40,42.4"), CORAL_D, 1.2),
           ST(P("M30.4,28 C32.8,32 34.4,36.6 35.4,40.6"), CORAL_D, 1.0),
           ST(P("M35.4,44.6 C37,43.4 39,45.4 40.8,43.6 C42.4,41.8 44.6,43.4 46.4,42"), GOLD, 1.8)]
    return out


def obj_boat():
    out = [F(P("M14,44 C18,40.6 22,47.4 26,44 C30,40.6 34,47.4 38,44 C42,40.6 46,47.4 50,44 "
               "L50,50 L14,50 Z"), SKY, ROYAL_L, 1.0)]
    out += [F(poly([(31.6, 14.6), (31.6, 36), (19.6, 36)]), PARCH, LINE, 1.5),
            F(poly([(33.6, 18.4), (33.6, 36), (43.6, 36)]), PARCH_D, LINE, 1.5),
            ST(line((32.6, 12.4), (32.6, 38)), WOOD_D, 2.0),
            F(P("M15.6,37.4 L48.4,37.4 C47.2,42.8 43,45.4 38,45.4 L26,45.4 "
                "C21,45.4 16.8,42.8 15.6,37.4 Z"), WOOD, LINE, 1.6),
            ST(line((18.6, 41), (45.4, 41)), WOOD_D, 1.1),
            F(poly([(32.6, 12), (37, 13.6), (32.6, 15.2)]), CORAL, CORAL_D, 0.8)]
    return out


def obj_crown():
    out = laurel(32, 31, 15.6, 112, 262, 1, 7, size=1.45) + \
          laurel(32, 31, 15.6, 68, -82, -1, 7, size=1.45)
    out += [F(P("M28,46.6 L32,44.6 L36,46.6 L37.6,51.6 L34.4,49.8 L32,52 L29.6,49.8 "
                "L26.4,51.6 Z"), CORAL, CORAL_D, 1.2),
            F(circle(32, 46.2, 2.2), GOLD, BROWN, 1.1)]
    for a in (-70, -50, 230, 250):
        t = math.radians(a)
        out.append(F(circle(32 + 18 * math.cos(t), 32 + 18 * math.sin(t), 1.3), GOLD_L, BROWN, 0.8))
    return out


# ------------------------------------------------------------------ achievement objects
def obj_ark():
    out = []
    for i, c in enumerate([CORAL, GOLD, OLIVE_L, SKY]):
        out.append(ST(arc(32, 34, 17 - i * 2.4, 190, 350), c, 2.4))
    out += [F(P("M12,46 C16,43 20,49 24,46 C28,43 32,49 36,46 C40,43 44,49 48,46 C50,45 52,46 52,46 "
                "L52,52 L12,52 Z"), SKY, ROYAL_L, 1.0),
            F(P("M13,37 L51,37 L47.6,44.6 C46.8,46 45.4,46.6 44,46.6 L20,46.6 "
                "C18.6,46.6 17.2,46 16.4,44.6 Z"), WOOD, LINE, 1.6),
            ST(line((15.4, 41), (48.8, 41)), WOOD_D, 1.1),
            F(rect(21, 29.6, 22, 7.4), WOOD_L, LINE, 1.5),
            F(poly([(18.6, 30.2), (32, 23.4), (45.4, 30.2)]), CORAL_D, LINE, 1.5),
            F(rrect(29.4, 31.2, 5.2, 4.4, 1.2), WOOD_D),
            F(circle(24.6, 33.2, 1.1), WOOD_D), F(circle(39.4, 33.2, 1.1), WOOD_D)]
    return out


def obj_sling():
    out = [ST(P("M17,15 C20,26 25,36 28.8,42"), "#8A5329", 2.4),
           ST(P("M47,15 C44,26 39,36 35.2,42"), "#8A5329", 2.4),
           F(circle(17, 13.6, 2.2), None, "#8A5329", 1.6) if False else
           ST(circle(17, 13.2, 2.2), "#8A5329", 1.6),
           F(P("M27,40 C29,38.4 35,38.4 37,40 C38.6,44.6 36.4,49.4 32,49.4 "
               "C27.6,49.4 25.4,44.6 27,40 Z"), WOOD, LINE, 1.6),
           F(circle(32, 41.6, 4.8), "#C4C9CF", "#4F5862", 1.5),
           F(circle(30.4, 40.2, 1.3), WHITE, fa=0.8)]
    for y, x0 in [(22, 22), (27, 24)]:
        pass
    return out


def obj_trumpet():
    pts = bez_pts((15, 47), (22, 49), (38, 44), (44, 18), 14)
    ws = [1.6 + 5.2 * (i / 13) ** 1.6 for i in range(14)]
    horn = tube(pts, ws)
    out = [F(horn, "#EAD7AF", LINE, 1.6)]
    for i in (5, 8, 10):
        (x, y), w = pts[i], ws[i]
        a, b = pts[i - 1], pts[i + 1]
        ang = math.atan2(b[1] - a[1], b[0] - a[0])
        nx, ny = -math.sin(ang), math.cos(ang)
        out.append(ST(line((x + nx * w * 0.9, y + ny * w * 0.9), (x - nx * w * 0.9, y - ny * w * 0.9)),
                      "#B99A62", 1.2))
    ex, ey = pts[-1]
    out += [F(ellipse(ex, ey, 6.4, 2.6).tf(R(-12, ex, ey)), "#8C6A3A", LINE, 1.4),
            F(circle(15, 47, 1.8), "#B99A62", LINE, 1.1)]
    for r in (5.4, 9):
        out.append(ST(arc(ex + 1, ey - 1, r, -80, 10), ROYAL_L, 1.6))
    return out


def obj_manna():
    out = []
    import random
    rnd = random.Random(7)
    flakes = []
    for i in range(22):
        x = 19 + rnd.random() * 26
        y = 29 - (1 - abs(x - 32) / 13) * 8 + rnd.random() * 5
        flakes.append((x, y))
    flakes.sort(key=lambda p: p[1])
    for x, y in flakes:
        out.append(F(ellipse(x, y, 2.6, 2.0), WHITE, "#BDB2A0", 0.9))
    out += [F(P("M16,31 L48,31 L44,48 C43.6,49.6 42.4,50.4 40.8,50.4 L23.2,50.4 "
                "C21.6,50.4 20.4,49.6 20,48 Z"), WOOD_L, LINE, 1.6)]
    for y in (36, 41, 46):
        out.append(ST(line((17.4 + (y - 31) * 0.24, y), (46.6 - (y - 31) * 0.24, y)), WOOD_D, 1.1))
    for x in range(21, 45, 4):
        out.append(ST(line((x, 32), (x + (32 - x) * 0.12, 49.6)), WOOD, 1.0))
    out += [F(rrect(14.6, 29.4, 34.8, 4, 2), WOOD, LINE, 1.5),
            F(sparkle(48, 17, 3.2), GOLD_L, GOLD_D, 0.8),
            F(sparkle(17, 19, 2.2), GOLD_L, GOLD_D, 0.7)]
    return out


NODES = [("node_stone_done", node_stone(False), 64, 64, 64),
         ("node_stone_current", node_stone(True), 64, 64, 64),
         ("node_scroll_locked", node_scroll_locked(), 64, 64, 64),
         ("node_stone_boss", node_stone_boss(), 80, 80, 80),
         ("path_footprint", footprint(), 24, 24, 16)]

RANKS = [("badge_rank_1_sandals", obj_sandals), ("badge_rank_2_net", obj_net),
         ("badge_rank_3_quill", obj_quill), ("badge_rank_4_staff", obj_staff_mantle),
         ("badge_rank_5_boat", obj_boat), ("badge_rank_6_crown", obj_crown)]
ACH = [("badge_ark", obj_ark), ("badge_sling", obj_sling),
       ("badge_trumpet", obj_trumpet), ("badge_manna", obj_manna)]

if __name__ == "__main__":
    names = []
    for n, layers, w, h, dp in NODES:
        export_icon(n, layers, w, h, dp=dp); names.append(n)
    for n, fn in RANKS:
        export_icon(n, rank_frame() + fn(), 64, 64, dp=48); names.append(n)
    for n, fn in ACH:
        export_icon(n, ach_frame() + place(fn, ACH_M), 64, 64, dp=48); names.append(n)
    sheet(names, "/home/claude/bibliq/cache/sheet_nodes.png", title="Nodes & badges")
    print("ok", len(names))
