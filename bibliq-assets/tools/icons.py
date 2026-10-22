"""Section 1 + 2 of assets-list: icons and power-ups (Vector Drawables)."""
import math
import random
from lib import *

OUT_W = 1.8          # outline width at 48 viewport
BROWN = "#6A3E14"    # outline for gold things


# ------------------------------------------------------------------ stars
def star_icon(filled):
    base = star(24, 25.6, 20, 10, round_=0.42)
    if filled:
        return [
            F(base, GOLD, "#9A5E0A", OUT_W),
            F(star(24, 25.6, 20, 10, round_=0.42).tf(S(0.62, 0.62, 24, 25.6), T(-1.6, -1.8)),
              GOLD_L, fa=0.55),
            F(ellipse(17.6, 18.8, 2.2, 1.4).tf(R(-35, 17.6, 18.8)), WHITE, fa=0.9),
        ]
    return [
        F(base, "#E9E4DD", "#A49D96", OUT_W),
        F(star(24, 25.6, 20, 10, round_=0.42).tf(S(0.62, 0.62, 24, 25.6), T(-1.2, -1.4)),
          WHITE, fa=0.6),
    ]


# ------------------------------------------------------------------ wax seal
def seal_r(a):
    return 16 + 1.25 * math.sin(9 * a) + 0.6 * math.sin(4 * a + 1.0)


def sprig(cx, cy, s, color):
    """Tiny olive sprig emblem (two pairs of leaves)."""
    out = [ST(line((cx - 5 * s, cy + 6 * s), (cx + 4 * s, cy - 6 * s)), color, 1.3 * s)]
    for t, sz in [(0.3, 1.0), (0.62, 0.9)]:
        x = cx - 5 * s + 9 * s * t
        y = cy + 6 * s - 12 * s * t
        ang = math.degrees(math.atan2(-12, 9))
        out.append(F(leaf(x, y, ang - 55, 5.2 * s * sz, 1.9 * s * sz), color))
        out.append(F(leaf(x, y, ang + 55, 5.2 * s * sz, 1.9 * s * sz), color))
    out.append(F(leaf(cx + 4 * s, cy - 6 * s, -53, 3.6 * s, 1.5 * s), color))
    return out


def seal_icon():
    body = blob(24, 24, seal_r, 72)
    return [
        F(body.tf(T(0, 1.6)), SEAL_D, LINE, OUT_W),                     # thickness
        F(body, SEAL, LINE, OUT_W),
        F(circle(24, 24, 10.6), SEAL_D, fa=0.35),
        ST(circle(24, 24, 10.6), SEAL_L, 1.4),
        *sprig(24, 24, 0.95, SEAL_D),
        ST(arc(24, 24, 13.6, 200, 255), "#F08A7E", 1.6),               # gloss
    ]


def seal_broken_icon():
    n = 144
    contour = []
    for i in range(n):
        a = 2 * math.pi * i / n
        r = seal_r(a)
        contour.append((24 + r * math.cos(a), 24 + r * math.sin(a), math.degrees(a) % 360))
    top = (24, 24 - seal_r(math.radians(270)))
    bot = (24, 24 + seal_r(math.radians(90)))
    crack = [top, (25.8, 13), (22.6, 18), (26, 23), (22.4, 28), (25.4, 33), bot]
    right_side = [p[:2] for p in contour if p[2] < 90 or p[2] > 270]
    right_side.sort(key=lambda p: math.atan2(p[1] - 24, p[0] - 24))   # -90..90
    left_side = [p[:2] for p in contour if 90 < p[2] < 270]
    left_side.sort(key=lambda p: math.radians(math.degrees(math.atan2(p[1] - 24, p[0] - 24)) % 360))
    left_poly = poly([bot] + left_side + [top] + crack[1:-1])
    right_poly = poly([top] + right_side + [bot] + crack[-2:0:-1])
    ml = (R(-9, 24, 24), T(-3.2, 0.6))
    mr = (R(9, 24, 24), T(3.2, 0.6))
    out = []
    for shp, m, arcs in [(left_poly, ml, (105, 255)), (right_poly, mr, (-75, 75))]:
        out += [F(shp.tf(*m, T(0, 1.4)), SEAL_D, LINE, OUT_W),
                F(shp.tf(*m), SEAL, LINE, OUT_W),
                ST(arc(24, 24, 10.6, *arcs).tf(*m), SEAL_L, 1.4)]
    out += [F(poly([(23.2, 4.4), (26.2, 5.6), (24.2, 8.2)]), SEAL, LINE, 1.0),
            F(poly([(23.4, 42.2), (25.6, 43.2), (24.0, 45.2)]), SEAL_D, LINE, 1.0)]
    return out


# ------------------------------------------------------------------ settings
def gear_icon():
    pts = []
    teeth, ro, ri = 8, 20, 15.2
    for i in range(teeth):
        a = i * 360 / teeth
        for da, r in [(-15, ri), (-8.5, ro), (8.5, ro), (15, ri)]:
            t = math.radians(a + da)
            pts.append((24 + r * math.cos(t), 24 + r * math.sin(t)))
    g = smooth(pts, t=0.35)
    return [L(g + circle(24, 24, 6.4), fill=WHITE, evenodd=True)]


# ------------------------------------------------------------------ nav (monochrome, tintable)
def nav_map():
    panels = [poly([(5, 12), (16.4, 8), (16.4, 36), (5, 40)]),
              poly([(18, 8), (30, 12), (30, 40), (18, 36)]),
              poly([(31.6, 12), (43, 8), (43, 36), (31.6, 40)])]
    dots = [(10.6, 32.5), (13.2, 27.5), (21.6, 24.4), (26.4, 21), (35.4, 18.2), (38.6, 14.4)]
    sh = [panels[0] + circle(*dots[0], 1.5) + circle(*dots[1], 1.5),
          panels[1] + circle(*dots[2], 1.5) + circle(*dots[3], 1.5),
          panels[2] + circle(*dots[4], 1.5) + circle(*dots[5], 1.5)]
    return [L(sh[0], fill=INK, evenodd=True, stroke=INK, sw=1.2),
            L(sh[1], fill=INK, fa=0.72, evenodd=True, stroke=INK, sw=1.2, sa=0.72),
            L(sh[2], fill=INK, evenodd=True, stroke=INK, sw=1.2)]


def nav_endless():
    inf = P("M24,24 C19.5,16.5 8.5,15.6 8.5,24 C8.5,32.4 19.5,31.5 24,24 "
            "C28.5,16.5 39.5,15.6 39.5,24 C39.5,32.4 28.5,31.5 24,24 Z")
    return [ST(inf, INK, 5.2)]


def nav_badges():
    rib_l = poly([(16.2, 27), (22.8, 30.6), (19.4, 44), (16.4, 40.6), (12.4, 41.8)])
    rib_r = rib_l.tf(S(-1, 1, 24, 0))
    medal = circle(24, 19.5, 12.4) + star(24, 20.2, 7, 3.3, round_=0.3)
    return [L(rib_l, fill=INK, fa=0.72, stroke=INK, sw=1.4, sa=0.72),
            L(rib_r, fill=INK, fa=0.72, stroke=INK, sw=1.4, sa=0.72),
            L(medal, fill=INK, evenodd=True)]


def nav_profile():
    return [F(circle(24, 15.5, 8.6), INK),
            F(P("M8.5,41.5 C8.5,32 15.4,26.6 24,26.6 C32.6,26.6 39.5,32 39.5,41.5 "
                "C39.5,42.6 38.7,43 37.6,43 L10.4,43 C9.3,43 8.5,42.6 8.5,41.5 Z"), INK)]


# ------------------------------------------------------------------ power-ups
def two_tone_stroke(shape, base, light, w):
    return [ST(shape, LINE, w + 2.4), ST(shape, base, w)] + \
           ([ST(shape.tf(T(-0.5, -0.5)), light, w * 0.3)] if light else [])


def power_staff():
    wave = P("M3.5,42 C3.5,31 5.6,21 11.4,13.6 C14.4,9.8 19.6,10 19.4,14.4 "
             "C17.4,12.6 14.6,14.8 14.4,18.6 C14,26.6 15.8,34.6 17.6,42 Z")
    foam = P("M17.2,14.8 C15.6,14.4 14.6,16.4 14.4,19 C14.2,24 14.8,29 15.6,33")
    crook = P("M24,44.5 L24,14 C24,6 33.6,5 33.8,11.2 C33.9,14.6 30.8,15.6 29.6,13.6")
    out = []
    for m in [(), (S(-1, 1, 24, 0),)]:
        out += [F(wave.tf(*m), SKY, ROYAL, 1.5),
                ST(foam.tf(*m), WHITE, 1.6),
                F(circle(9, 37, 1.4).tf(*m), WHITE, fa=0.8),
                F(circle(11.2, 31, 1.0).tf(*m), WHITE, fa=0.8)]
    out += two_tone_stroke(crook, WOOD, WOOD_L, 3.4)
    out += [ST(line((22.4, 44.5), (25.6, 44.5)), LINE, 1.6)]
    return out


def power_sundial():
    dial_top = ellipse(24, 30, 18, 8.6)
    dial_side = P("M6,30 L6,34 C6,38.8 14,42.6 24,42.6 C34,42.6 42,38.8 42,34 L42,30 Z")
    out = [F(dial_side, STONE_D, STONE_LINE, OUT_W),
           F(dial_top, STONE_L, STONE_LINE, OUT_W)]
    for a in range(200, 341, 20):
        t = math.radians(a)
        out.append(ST(line((24 + 11 * math.cos(t), 30 + 5.2 * math.sin(t)),
                           (24 + 15.6 * math.cos(t), 30 + 7.4 * math.sin(t))), STONE_D, 1.3))
    for a in range(20, 161, 35):
        t = math.radians(a)
        out.append(ST(line((24 + 12 * math.cos(t), 30 + 5.7 * math.sin(t)),
                           (24 + 15.6 * math.cos(t), 30 + 7.4 * math.sin(t))), STONE_D, 1.3))
    out += [F(poly([(24, 30.6), (11.2, 33.6), (12.2, 30.4)]), INK, fa=0.35),   # shadow
            F(poly([(22.2, 31), (24.6, 15.2), (27.4, 31)]), GOLD, BROWN, 1.5),
            ST(line((24.4, 17.6), (25.4, 29.2)), GOLD_L, 1.0)]
    a_end = -152
    arr = arc(24, 25, 19.5, -28, a_end + 10)
    t = math.radians(a_end + 10)
    ex, ey = 24 + 19.5 * math.cos(t), 25 + 19.5 * math.sin(t)
    tang = math.degrees(t) - 90          # direction of travel (counter-clockwise)
    head = poly([(0, -4.2), (6.4, 0), (0, 4.2)]).tf(R(tang), T(ex, ey))
    out += [ST(arr, OLIVE, 3.2), F(head, OLIVE, OLIVE, 1.4)]
    return out


def tongue(x, y, ln, w, ang, curl=0.35):
    """Flame tongue rooted at (x,y), pointing at ang (deg)."""
    base = P(f"M0,{-w} C{ln*0.35},{-w*1.1} {ln*0.7},{-w*0.2-ln*curl*0.4} {ln},{-ln*curl} "
             f"C{ln*0.62},{w*0.3-ln*curl*0.15} {ln*0.3},{w*1.1} 0,{w} "
             f"C{-w*0.7},{w*0.6} {-w*0.7},{-w*0.6} 0,{-w} Z")
    return base.tf(R(ang), T(x, y))


def power_chariot():
    out = []
    flames = [(16, 21.5, 15, 4.6, 196, 0.45), (14, 27, 14, 4.2, 182, 0.3),
              (18, 15.5, 12, 3.8, 214, 0.55), (15.5, 31.5, 10, 3.2, 172, 0.2)]
    for x, y, ln, w, ang, c in flames:
        out.append(F(tongue(x, y, ln, w, ang, c), CORAL, "#B8392A", 1.1))
    for x, y, ln, w, ang, c in flames:
        out.append(F(tongue(x + 0.5, y, ln * 0.62, w * 0.55, ang, c), GOLD))
    for x, y, ln, w, ang, c in flames[:2]:
        out.append(F(tongue(x + 0.8, y, ln * 0.3, w * 0.3, ang, c), GOLD_L))
    body = P("M10.4,31.6 L36.6,31.6 C38.6,31.6 39.8,30 39.2,28 L35.6,15.6 "
             "C35.1,13.8 32.4,14 32.6,16.2 L33,23.6 L14.6,23.6 "
             "C12.2,23.6 10.4,25.4 10.4,27.8 Z")
    out += [F(body, GOLD, BROWN, OUT_W),
            ST(line((14.2, 27.6), (34.2, 27.6)), GOLD_D, 1.4),
            ST(P("M34.6,18 L36.4,25.4"), GOLD_L, 1.2),
            F(circle(23.4, 34.6, 8.8), WOOD, LINE, OUT_W),
            F(circle(23.4, 34.6, 6.2), "#E8C27A", WOOD_D, 1.0)]
    for a in range(0, 360, 45):
        t = math.radians(a)
        out.append(ST(line((23.4, 34.6), (23.4 + 6.2 * math.cos(t), 34.6 + 6.2 * math.sin(t))), WOOD_D, 1.3))
    out += [F(circle(23.4, 34.6, 1.9), GOLD, BROWN, 1.0),
            ST(line((3, 40.6), (11.4, 40.6)), ROYAL_L, 1.6),
            ST(line((6, 44.2), (13.6, 44.2)), ROYAL_L, 1.6)]
    return out


def power_scroll():
    paper = rrect(12, 9.6, 24, 29, 2)
    out = [F(paper, PARCH, LINE, OUT_W)]
    for i, wl in enumerate([16, 13, 15.6, 10]):
        y = 16.4 + i * 5.2
        out.append(ST(line((16, y), (16 + wl, y)), PARCH_DD, 1.8))
    for y in (6, 35.2):
        out += [F(rrect(8.6, y, 30.8, 6.8, 3.4), WOOD, LINE, OUT_W),
                ST(line((12, y + 2.2), (36, y + 2.2)), WOOD_L, 1.2),
                F(circle(7.4, y + 3.4, 2.4), GOLD, BROWN, 1.3),
                F(circle(40.6, y + 3.4, 2.4), GOLD, BROWN, 1.3)]
    out += [F(sparkle(40.5, 22, 5.6), GOLD_L, GOLD_D, 0.9),
            F(sparkle(43.4, 13.4, 2.8), GOLD_L, GOLD_D, 0.7)]
    return out


ICONS = [
    ("ic_star_filled", star_icon(True), 24),
    ("ic_star_empty", star_icon(False), 24),
    ("ic_seal", seal_icon(), 24),
    ("ic_seal_broken", seal_broken_icon(), 24),
    ("ic_settings", gear_icon(), 24),
    ("ic_nav_map", nav_map(), 24),
    ("ic_nav_endless", nav_endless(), 24),
    ("ic_nav_badges", nav_badges(), 24),
    ("ic_nav_profile", nav_profile(), 24),
    ("ic_power_staff", power_staff(), 40),
    ("ic_power_sundial", power_sundial(), 40),
    ("ic_power_chariot", power_chariot(), 40),
    ("ic_power_scroll", power_scroll(), 40),
]

if __name__ == "__main__":
    for name, layers, dp in ICONS:
        export_icon(name, layers, dp=dp)
    names = [n for n, _, _ in ICONS]
    sheet(names, "/home/claude/bibliq/cache/sheet_icons.png", title="Icons & power-ups")
    print("ok", len(names))
