"""Section 7 + 8: layered Oli master (for Lottie), static Oli, adaptive app icon, Play Store icon."""
import math
from lib import *

OL = "#4A3428"          # Oli outline
OW = 7                  # outline width in the 512 artboard
SHADE = "#EFE5D6"
FEET = "#F2A07A"; FEET_D = "#C9714A"
CHEEK = "#F7A9A0"

HEAD_C = (262, 198); HEAD_R = 108
BODY_C = (256, 334); BODY_R = (136, 118)


def g_shadow():
    return [F(ellipse(256, 470, 120, 16), INK, fa=0.14)]


def g_tail():
    out = []
    for ang, (x, y) in [(158, (176, 392)), (138, (184, 410)), (118, (198, 424))]:
        f = rrect(-6, -17, 70, 34, 17).tf(R(ang), T(x, y))
        out += [F(f, SHADE, OL, OW)]
    return out


def g_feet():
    out = []
    for fx in (222, 292):
        for da in (-28, 0, 28):
            a = math.radians(90 + da)
            toe = rrect(-7, -4, 14, 34, 7).tf(R(da), T(fx, 444))
            out.append(F(toe, FEET, OL, 5))
    return out


def g_body():
    body = ellipse(*BODY_C, *BODY_R)
    out = [F(body, WHITE, OL, OW),
           F(ellipse(256, 380, 98, 62), SHADE),
           ST(arc(256, 334, 118, 30, 150, 100), SHADE, 10, sa=0.9)]
    return out


def g_head():
    cx, cy = HEAD_C
    return [F(circle(cx, cy, HEAD_R), WHITE),
            ST(arc(cx, cy, HEAD_R, 128, 412), OL, OW),
            ST(arc(cx, cy, HEAD_R - 18, 205, 250), SHADE, 9, sa=0.0),
            F(ellipse(205, 240, 20, 12), CHEEK, fa=0.75),
            F(ellipse(322, 240, 20, 12), CHEEK, fa=0.75),
            F(P("M232,104 C238,82 256,74 266,86 C270,72 290,74 290,94 C280,92 270,96 266,102 "
                "C256,94 242,96 232,104 Z"), WHITE, OL, 5)]


WING = P("M170,268 C128,272 94,312 90,354 C88,374 104,384 116,374 C118,390 136,394 144,380 "
         "C150,394 168,392 170,374 C180,342 194,302 170,268 Z")


def wing(mirror):
    m = (S(-1, 1, 256, 0),) if mirror else ()
    return [F(WING.tf(*m), WHITE, OL, OW),
            ST(P("M156,292 C134,304 118,326 110,350").tf(*m), SHADE, 6),
            ST(P("M164,312 C150,330 140,352 136,370").tf(*m), SHADE, 6),
            ST(P("M96,360 C98,372 108,378 116,374").tf(*m), GOLD, 6),
            ST(P("M118,378 C124,390 138,392 144,382").tf(*m), GOLD, 6),
            ST(P("M146,382 C152,392 166,392 170,378").tf(*m), GOLD, 6)]


EYES = [(214, 194), (310, 194)]


def g_eyes_open():
    out = []
    for x, y in EYES:
        out += [F(ellipse(x, y, 22, 28), INK),
                F(circle(x - 7, y - 10, 8), WHITE),
                F(circle(x + 7, y + 9, 3.6), WHITE, fa=0.85)]
    return out


def g_eyes_happy():
    return [ST(P(f"M{x-22},{y+6} Q{x},{y-26} {x+22},{y+6}"), INK, 10) for x, y in EYES]


def g_eyes_soft():
    """Kind, smiling eyes: round eyes with a soft upward-curved lower lid."""
    out = []
    for x, y in EYES:
        out += [F(ellipse(x, y, 21, 25), INK),
                F(circle(x - 7, y - 9, 8), WHITE),
                F(circle(x + 7, y + 3, 3.4), WHITE, fa=0.85),
                F(P(f"M{x-26},{y+14} Q{x},{y-2} {x+26},{y+14} L{x+26},{y+34} L{x-26},{y+34} Z"), WHITE),
                ST(P(f"M{x-20},{y+12} Q{x},{y+1} {x+20},{y+12}"), INK, 4, sa=0.5)]
    return out


def g_brows_soft():
    (lx, ly), (rx, ry) = EYES
    return [ST(P(f"M{lx-22},{ly-38} Q{lx-4},{ly-52} {lx+18},{ly-46}"), INK, 7, sa=0.85),
            ST(P(f"M{rx+22},{ry-38} Q{rx+4},{ry-52} {rx-18},{ry-46}"), INK, 7, sa=0.85)]


def g_branch():
    p0, p1, p2, p3 = (262, 252), (300, 252), (352, 238), (408, 206)
    out = [ST(P(f"M{p0[0]},{p0[1]} C{p1[0]},{p1[1]} {p2[0]},{p2[1]} {p3[0]},{p3[1]}"), "#7A5A2A", 8)]
    pts = bez_pts(p0, p1, p2, p3, 9)
    for i, side in [(3, 1), (4, -1), (5, 1), (6, -1), (7, 1)]:
        x, y = pts[i]
        a, b = pts[i - 1], pts[i + 1]
        ang = math.degrees(math.atan2(b[1] - a[1], b[0] - a[0]))
        out.append(F(leaf(x, y, ang - 50 * side, 46, 13), OLIVE_L if side > 0 else OLIVE, OLIVE_D, 4))
    out.append(F(leaf(408, 206, -36, 40, 12), OLIVE_L, OLIVE_D, 4))
    out += [F(ellipse(338, 262, 9, 11), "#5E4A7A", OL, 3)]
    return out


def g_beak_closed():
    return [F(P("M240,232 C248,226 276,226 284,232 C282,246 270,258 262,262 "
                "C254,258 242,246 240,232 Z"), GOLD, OL, 5),
            ST(P("M248,240 C256,244 268,244 276,240"), GOLD_D, 3),
            F(ellipse(254, 234, 6, 3), GOLD_L, fa=0.9)]


def g_beak_open():
    return [F(ellipse(262, 258, 15, 13), "#7A2E2E", OL, 4),
            F(P("M240,230 C248,224 276,224 284,230 C282,240 270,248 262,250 "
                "C254,248 242,240 240,230 Z"), GOLD, OL, 5),
            F(P("M250,262 C256,262 268,262 274,262 C272,272 266,278 262,280 "
                "C258,278 252,272 250,262 Z"), GOLD_D, OL, 4)]


def g_smile():
    return [ST(P("M230,240 Q234,250 242,250"), INK, 4, sa=0.7),
            ST(P("M294,240 Q290,250 282,250"), INK, 4, sa=0.7)]


def g_sparkle(i):
    spots = [(112, 120, 24), (408, 112, 30), (440, 300, 20)]
    x, y, r = spots[i]
    return [F(sparkle(x, y, r), GOLD_L, GOLD_D, 4)]


GROUPS = [
    ("shadow", g_shadow(), True),
    ("feet", g_feet(), True),
    ("body", g_body(), True),
    ("head", g_head(), True),
    ("wing_left", wing(False), True),
    ("wing_right", wing(True), True),
    ("eyes_open", g_eyes_open(), True),
    ("eyes_happy", g_eyes_happy(), False),
    ("eyes_soft", g_eyes_soft(), False),
    ("brows_soft", g_brows_soft(), False),
    ("branch", g_branch(), True),
    ("beak_closed", g_beak_closed(), True),
    ("beak_open", g_beak_open(), False),
    ("smile", g_smile(), False),
    ("sparkle_1", g_sparkle(0), False),
    ("sparkle_2", g_sparkle(1), False),
    ("sparkle_3", g_sparkle(2), False),
]

PIVOTS = {"body": (256, 452), "head": (262, 300), "wing_left": (170, 278),
          "wing_right": (342, 278), "branch": (262, 252)}


def master_svg(visible=None, size=512):
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 512 512">',
         "<!-- Oli master. Each <g> is one animation layer. Hidden layers have display=none. -->"]
    for name, layers, vis in GROUPS:
        show = vis if visible is None else (name in visible)
        attr = "" if show else ' display="none"'
        piv = f' data-pivot="{PIVOTS[name][0]},{PIVOTS[name][1]}"' if name in PIVOTS else ""
        s.append(f'<g id="{name}"{attr}{piv}>')
        s += [layer_svg(l) for l in layers]
        s.append("</g>")
    s.append("</svg>")
    return "\n".join(s)


def pose(swap):
    vis = {n for n, _, v in GROUPS if v}
    for a, b in swap:
        vis.discard(a); vis.add(b)
    return vis


# ------------------------------------------------------------------ app icon
def scaled(layers, m):
    k = m_scale(m)
    return [dict(l, d=P(l["d"]).tf(*m).d(), sw=l["sw"] * k) for l in layers]


def m_scale(m):
    mm = chain(*m)
    return math.sqrt(abs(mm[0] * mm[4] - mm[1] * mm[3]))


ICON_M = (T(-262, -250), S(0.2), T(52, 62))


def fg_layers():
    out = []
    for name in ["body", "head", "wing_left", "wing_right", "eyes_open",
                 "branch", "beak_closed"]:
        out += scaled(dict((n, l) for n, l, _ in GROUPS)[name], ICON_M)
    return out


def bg_layers():
    out = [F(rect(0, 0, 108, 108), ROYAL)]
    for i in range(12):
        a0 = math.radians(i * 30 - 6); a1 = math.radians(i * 30 + 6)
        out.append(F(poly([(54, 54), (54 + 90 * math.cos(a0), 54 + 90 * math.sin(a0)),
                           (54 + 90 * math.cos(a1), 54 + 90 * math.sin(a1))]), ROYAL_L, fa=0.35))
    out += [F(circle(54, 54, 34), SKY, fa=0.18), F(circle(54, 54, 26), GOLD_L, fa=0.18)]
    return out


def mono_layers():
    gr = dict((n, l) for n, l, _ in GROUPS)
    sil = []
    for name in ["body", "head", "wing_left", "wing_right", "branch", "beak_closed"]:
        for l in scaled(gr[name], ICON_M):
            if l["fill"]:
                sil.append(dict(l, fill="#FFFFFF", stroke="#FFFFFF" if l["stroke"] else None, fa=None))
            elif l["stroke"]:
                sil.append(dict(l, stroke="#FFFFFF", sa=None))
    eyes = [dict(l, fill="#000000", fa=None) for l in scaled(g_eyes_open()[0::3], ICON_M)]
    return sil + eyes


def masked(img, kind):
    from PIL import ImageDraw
    m = Image.new("L", img.size, 0)
    d = ImageDraw.Draw(m)
    w, h = img.size
    if kind == "round":
        d.ellipse((0, 0, w - 1, h - 1), fill=255)
    else:
        d.rounded_rectangle((0, 0, w - 1, h - 1), radius=int(w * 0.2), fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), m)
    return out


if __name__ == "__main__":
    os.makedirs(f"{ROOT}/oli", exist_ok=True)
    with open(f"{ROOT}/oli/oli_master.svg", "w") as f:
        f.write(master_svg())
    poses = {
        "oli_idle": pose([]),
        "pose_correct": pose([("eyes_open", "eyes_happy"), ("beak_closed", "beak_open")])
        | {"sparkle_1", "sparkle_2", "sparkle_3"},
        "pose_wrong": pose([("eyes_open", "eyes_soft")]) | {"brows_soft", "smile"},
    }
    for n, vis in poses.items():
        img = render(master_svg(vis))
        img.save(f"/home/claude/bibliq/cache/{n}.png")
    idle = Image.open("/home/claude/bibliq/cache/oli_idle.png")
    idle.save(f"{DIRS['nodpi']}/oli_idle.webp", "WEBP", quality=92, method=6)

    # adaptive icon
    with open(f"{DIRS['drawable']}/ic_launcher_foreground.xml", "w") as f:
        f.write(to_vd(fg_layers(), 108, 108, 108, 108, "BibliQ launcher foreground (Oli)."))
    with open(f"{DIRS['drawable']}/ic_launcher_background.xml", "w") as f:
        f.write(to_vd(bg_layers(), 108, 108, 108, 108, "BibliQ launcher background."))
    with open(f"{DIRS['drawable']}/ic_launcher_monochrome.xml", "w") as f:
        f.write(to_vd(mono_layers(), 108, 108, 108, 108, "BibliQ themed (monochrome) icon."))
    adaptive = ('<?xml version="1.0" encoding="utf-8"?>\n'
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                '    <background android:drawable="@drawable/ic_launcher_background" />\n'
                '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
                '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
                '</adaptive-icon>\n')
    for n in ("ic_launcher", "ic_launcher_round"):
        with open(f"{DIRS['anydpi']}/{n}.xml", "w") as f:
            f.write(adaptive)
    full = to_svg(bg_layers() + fg_layers(), 108, 108, 512, 512)
    with open(f"{DIRS['svg']}/ic_launcher_full.svg", "w") as f:
        f.write(to_svg(bg_layers() + fg_layers(), 108, 108))
    play = render(full).convert("RGB")
    play.save(f"{DIRS['store']}/ic_play_store_512.png")
    # legacy mipmaps (API 24-25) — icon art shown at 72/108 of the canvas like adaptive icons
    for dens, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
        d = f"{ROOT}/android/res/mipmap-{dens}"
        os.makedirs(d, exist_ok=True)
        big = render(to_svg(bg_layers() + fg_layers(), 108, 108, px * 4, px * 4))
        crop = int(px * 4 * 18 / 108)
        big = big.crop((crop, crop, px * 4 - crop, px * 4 - crop)).resize((px, px), Image.LANCZOS)
        masked(big, "square").save(f"{d}/ic_launcher.png")
        masked(big, "round").save(f"{d}/ic_launcher_round.png")
    render(to_svg(mono_layers(), 108, 108, 256, 256), bg="#2D2A32").save("/home/claude/bibliq/cache/mono.png")
    play.save("/home/claude/bibliq/cache/play.png")
    print("ok")

    # pose files (only the visible layers) + previews for the team
    for n, vis in poses.items():
        nm = n.replace("pose_", "oli_pose_") if n.startswith("pose") else "oli_pose_idle"
        with open(f"{ROOT}/oli/{nm}.svg", "w") as f:
            f.write(master_svg(vis))
        Image.open(f"/home/claude/bibliq/cache/{n}.png").save(f"{ROOT}/oli/{nm}.png")
