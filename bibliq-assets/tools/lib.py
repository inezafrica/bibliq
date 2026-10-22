"""BibliQ asset toolkit.

Shapes are absolute-coordinate paths (M/L/C/Q/Z) that can be transformed, so every
icon can be exported both as an Android Vector Drawable and as SVG, and raster
assets can be rendered to PNG / WebP from the same SVG.
"""
import io
import math
import os
import re

import cairosvg
from PIL import Image, ImageDraw, ImageFont

# --------------------------------------------------------------------------- palette
ROYAL = "#2B4C9B"; ROYAL_D = "#1E3670"; ROYAL_L = "#4A6CC0"
SKY = "#8EC5FF"; SKY_L = "#CFE6FF"
GOLD = "#F5B82E"; GOLD_D = "#D99A12"; GOLD_L = "#FFE7A3"; GOLD_DD = "#A86F0A"
OLIVE = "#4E9A2E"; OLIVE_D = "#3D7F22"; OLIVE_L = "#7CC46A"
CORAL = "#F2665A"; CORAL_D = "#D9483D"
PARCH = "#FFF6E5"; PARCH_D = "#EED9B4"; PARCH_DD = "#D9BC8C"
INK = "#2D2A32"; WHITE = "#FFFFFF"
LINE = "#5A3220"          # warm dark-brown outline
STONE = "#D9D2C5"; STONE_D = "#B4AA99"; STONE_L = "#EFEAE0"; STONE_LINE = "#5E554A"
WOOD = "#B9783F"; WOOD_D = "#8A5329"; WOOD_L = "#D79A5E"
CLAY = "#C8743A"; CLAY_D = "#A85A2A"
GREY = "#B8B3B0"; GREY_D = "#8C8682"; GREY_L = "#DAD6D3"
SEAL = "#B23A2E"; SEAL_D = "#7E2219"; SEAL_L = "#D9584A"


def fmt(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


# --------------------------------------------------------------------------- affine
def T(dx=0, dy=0):
    return (1, 0, dx, 0, 1, dy)


def S(sx, sy=None, cx=0, cy=0):
    sy = sx if sy is None else sy
    return (sx, 0, cx - sx * cx, 0, sy, cy - sy * cy)


def R(deg, cx=0, cy=0):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return (c, -s, cx - c * cx + s * cy, s, c, cy - s * cx - c * cy)


def mul(m2, m1):
    """m2 after m1."""
    a, b, c, d, e, f = m1
    A, B, C, D, E, F = m2
    return (A * a + B * d, A * b + B * e, A * c + B * f + C,
            D * a + E * d, D * b + E * e, D * c + E * f + F)


def chain(*ms):
    out = (1, 0, 0, 0, 1, 0)
    for m in ms:
        out = mul(m, out)
    return out


def ap(m, p):
    return (m[0] * p[0] + m[1] * p[1] + m[2], m[3] * p[0] + m[4] * p[1] + m[5])


# --------------------------------------------------------------------------- shape
class Shape:
    def __init__(self, cmds=None):
        self.cmds = cmds or []

    def __add__(self, other):
        return Shape(self.cmds + other.cmds)

    def tf(self, *ms):
        m = chain(*ms)
        out = []
        for c in self.cmds:
            out.append((c[0],) + tuple(ap(m, p) for p in c[1:]))
        return Shape(out)

    def d(self):
        parts = []
        for c in self.cmds:
            if c[0] == "Z":
                parts.append("Z")
            else:
                parts.append(c[0] + " ".join(f"{fmt(p[0])},{fmt(p[1])}" for p in c[1:]))
        return " ".join(parts)


_tok = re.compile(r"[MmLlHhVvCcQqSsTtZz]|-?\d*\.?\d+(?:e-?\d+)?")


def P(d):
    """Parse an SVG path string (M L H V C Q S Z, abs/rel; no arcs) into a Shape."""
    toks = _tok.findall(d)
    i = 0
    cmds = []
    cur = (0.0, 0.0)
    start = (0.0, 0.0)
    last_ctrl = None
    cmd = None

    def num():
        nonlocal i
        v = float(toks[i]); i += 1
        return v

    while i < len(toks):
        if re.match(r"[A-Za-z]", toks[i]):
            cmd = toks[i]; i += 1
        rel = cmd.islower()
        C = cmd.upper()
        base = cur if rel else (0, 0)
        if C == "Z":
            cmds.append(("Z",)); cur = start; last_ctrl = None
            continue
        if C == "M":
            p = (base[0] + num(), base[1] + num())
            cmds.append(("M", p)); cur = start = p
            cmd = "l" if rel else "L"
        elif C == "L":
            p = (base[0] + num(), base[1] + num()); cmds.append(("L", p)); cur = p
        elif C == "H":
            x = num() + (cur[0] if rel else 0); p = (x, cur[1]); cmds.append(("L", p)); cur = p
        elif C == "V":
            y = num() + (cur[1] if rel else 0); p = (cur[0], y); cmds.append(("L", p)); cur = p
        elif C == "C":
            p1 = (base[0] + num(), base[1] + num())
            p2 = (base[0] + num(), base[1] + num())
            p3 = (base[0] + num(), base[1] + num())
            cmds.append(("C", p1, p2, p3)); cur = p3; last_ctrl = p2
            continue
        elif C == "S":
            p1 = (2 * cur[0] - last_ctrl[0], 2 * cur[1] - last_ctrl[1]) if last_ctrl else cur
            p2 = (base[0] + num(), base[1] + num())
            p3 = (base[0] + num(), base[1] + num())
            cmds.append(("C", p1, p2, p3)); cur = p3; last_ctrl = p2
            continue
        elif C == "Q":
            p1 = (base[0] + num(), base[1] + num())
            p2 = (base[0] + num(), base[1] + num())
            cmds.append(("Q", p1, p2)); cur = p2
        else:
            raise ValueError(cmd)
        last_ctrl = None
    return Shape(cmds)


K = 0.5522847498


def ellipse(cx, cy, rx, ry=None):
    ry = rx if ry is None else ry
    kx, ky = rx * K, ry * K
    return Shape([
        ("M", (cx + rx, cy)),
        ("C", (cx + rx, cy + ky), (cx + kx, cy + ry), (cx, cy + ry)),
        ("C", (cx - kx, cy + ry), (cx - rx, cy + ky), (cx - rx, cy)),
        ("C", (cx - rx, cy - ky), (cx - kx, cy - ry), (cx, cy - ry)),
        ("C", (cx + kx, cy - ry), (cx + rx, cy - ky), (cx + rx, cy)),
        ("Z",)])


def circle(cx, cy, r):
    return ellipse(cx, cy, r, r)


def rrect(x, y, w, h, r):
    r = min(r, w / 2, h / 2)
    k = r * (1 - K)
    return Shape([
        ("M", (x + r, y)), ("L", (x + w - r, y)),
        ("C", (x + w - k, y), (x + w, y + k), (x + w, y + r)),
        ("L", (x + w, y + h - r)),
        ("C", (x + w, y + h - k), (x + w - k, y + h), (x + w - r, y + h)),
        ("L", (x + r, y + h)),
        ("C", (x + k, y + h), (x, y + h - k), (x, y + h - r)),
        ("L", (x, y + r)),
        ("C", (x, y + k), (x + k, y), (x + r, y)), ("Z",)])


def rect(x, y, w, h):
    return poly([(x, y), (x + w, y), (x + w, y + h), (x, y + h)])


def poly(pts, closed=True):
    cmds = [("M", pts[0])] + [("L", p) for p in pts[1:]]
    if closed:
        cmds.append(("Z",))
    return Shape(cmds)


def line(*pts):
    return poly(list(pts), closed=False)


def star(cx, cy, ro, ri, n=5, rot=-90, round_=0.0):
    """Star; round_ in 0..1 softens the points with quadratic corners."""
    pts = []
    for i in range(2 * n):
        r = ro if i % 2 == 0 else ri
        a = math.radians(rot + i * 180 / n)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    if not round_:
        return poly(pts)
    cmds = []
    m = len(pts)
    for i in range(m):
        p0, p1, p2 = pts[i - 1], pts[i], pts[(i + 1) % m]
        a = (p1[0] + (p0[0] - p1[0]) * round_ / 2, p1[1] + (p0[1] - p1[1]) * round_ / 2)
        b = (p1[0] + (p2[0] - p1[0]) * round_ / 2, p1[1] + (p2[1] - p1[1]) * round_ / 2)
        cmds.append(("M" if i == 0 else "L", a))
        cmds.append(("Q", p1, b))
    cmds.append(("Z",))
    return Shape(cmds)


def leaf(x, y, ang, length, width):
    a = math.radians(ang)
    ux, uy = math.cos(a), math.sin(a)
    nx, ny = -uy, ux
    tip = (x + ux * length, y + uy * length)
    m1 = (x + ux * length * .5 + nx * width, y + uy * length * .5 + ny * width)
    m2 = (x + ux * length * .5 - nx * width, y + uy * length * .5 - ny * width)
    return Shape([("M", (x, y)), ("Q", m1, tip), ("Q", m2, (x, y)), ("Z",)])


def arc(cx, cy, r, a0, a1, ry=None):
    """Open arc as cubic segments (degrees, clockwise in screen coords)."""
    ry = r if ry is None else ry
    segs = max(1, int(math.ceil(abs(a1 - a0) / 90)))
    da = (a1 - a0) / segs
    cmds = []
    for s in range(segs):
        t0 = math.radians(a0 + s * da)
        t1 = math.radians(a0 + (s + 1) * da)
        k = 4 / 3 * math.tan((t1 - t0) / 4)
        p0 = (cx + r * math.cos(t0), cy + ry * math.sin(t0))
        p3 = (cx + r * math.cos(t1), cy + ry * math.sin(t1))
        c1 = (p0[0] - k * r * math.sin(t0), p0[1] + k * ry * math.cos(t0))
        c2 = (p3[0] + k * r * math.sin(t1), p3[1] - k * ry * math.cos(t1))
        if s == 0:
            cmds.append(("M", p0))
        cmds.append(("C", c1, c2, p3))
    return Shape(cmds)


def ring(cx, cy, ro, ri):
    """Annulus — use with evenodd=True."""
    return circle(cx, cy, ro) + circle(cx, cy, ri)


# --------------------------------------------------------------------------- layers
def L(shape, fill=None, stroke=None, sw=1.0, fa=None, sa=None, evenodd=False,
      cap="round", join="round"):
    if isinstance(shape, str):
        shape = P(shape)
    return dict(d=shape.d(), fill=fill, stroke=stroke, sw=sw, fa=fa, sa=sa,
                evenodd=evenodd, cap=cap, join=join)


def F(shape, fill, outline=None, sw=1.6, **kw):
    """Filled shape with optional outline."""
    return L(shape, fill=fill, stroke=outline, sw=sw, **kw)


def ST(shape, color, sw, **kw):
    """Stroke-only shape."""
    return L(shape, stroke=color, sw=sw, **kw)


def to_vd(layers, w, h, dp_w, dp_h, comment=""):
    out = ['<?xml version="1.0" encoding="utf-8"?>']
    if comment:
        out.append(f"<!-- {comment} -->")
    out += ['<vector xmlns:android="http://schemas.android.com/apk/res/android"',
            f'    android:width="{dp_w}dp"', f'    android:height="{dp_h}dp"',
            f'    android:viewportWidth="{w}"', f'    android:viewportHeight="{h}">']
    for l in layers:
        at = [f'android:pathData="{l["d"]}"']
        if l["fill"]:
            at.append(f'android:fillColor="{l["fill"]}"')
            if l["fa"] is not None:
                at.append(f'android:fillAlpha="{l["fa"]}"')
            if l["evenodd"]:
                at.append('android:fillType="evenOdd"')
        if l["stroke"]:
            at += [f'android:strokeColor="{l["stroke"]}"',
                   f'android:strokeWidth="{fmt(l["sw"])}"',
                   f'android:strokeLineCap="{l["cap"]}"',
                   f'android:strokeLineJoin="{l["join"]}"']
            if l["sa"] is not None:
                at.append(f'android:strokeAlpha="{l["sa"]}"')
        out.append("    <path\n        " + "\n        ".join(at) + " />")
    out.append("</vector>")
    return "\n".join(out) + "\n"


def layer_svg(l):
    e = f'<path d="{l["d"]}" fill="{l["fill"] or "none"}"'
    if l["fill"] and l["fa"] is not None:
        e += f' fill-opacity="{l["fa"]}"'
    if l["evenodd"]:
        e += ' fill-rule="evenodd"'
    if l["stroke"]:
        e += (f' stroke="{l["stroke"]}" stroke-width="{fmt(l["sw"])}" '
              f'stroke-linecap="{l["cap"]}" stroke-linejoin="{l["join"]}"')
        if l["sa"] is not None:
            e += f' stroke-opacity="{l["sa"]}"'
    return e + "/>"


def to_svg(layers, w, h, out_w=None, out_h=None, defs="", extra_top="", extra_bottom=""):
    out_w = out_w or w
    out_h = out_h or h
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{out_w}" height="{out_h}" '
         f'viewBox="0 0 {w} {h}">']
    if defs:
        s.append(f"<defs>{defs}</defs>")
    if extra_bottom:
        s.append(extra_bottom)
    for l in layers:
        s.append(l if isinstance(l, str) else layer_svg(l))
    if extra_top:
        s.append(extra_top)
    s.append("</svg>")
    return "\n".join(s)


def render(svg_text, bg=None):
    png = cairosvg.svg2png(bytestring=svg_text.encode(), background_color=bg)
    return Image.open(io.BytesIO(png)).convert("RGBA")


# --------------------------------------------------------------------------- export
ROOT = "/mnt/user-data/outputs/bibliq-assets"
DIRS = dict(
    drawable=f"{ROOT}/android/res/drawable",
    nodpi=f"{ROOT}/android/res/drawable-nodpi",
    anydpi=f"{ROOT}/android/res/mipmap-anydpi-v26",
    svg=f"{ROOT}/source-svg",
    prev=f"{ROOT}/previews",
    store=f"{ROOT}/play-store",
)
for _d in DIRS.values():
    os.makedirs(_d, exist_ok=True)

PREVIEW_CACHE = "/home/claude/bibliq/cache"
os.makedirs(PREVIEW_CACHE, exist_ok=True)


def export_icon(name, layers, w=48, h=48, dp=24, dp_h=None, comment=None):
    dp_h = dp_h or dp * h / w
    xml = to_vd(layers, w, h, fmt(dp), fmt(dp_h),
                comment or f"BibliQ — {name}. Viewport {w}x{h}.")
    with open(f"{DIRS['drawable']}/{name}.xml", "w") as f:
        f.write(xml)
    with open(f"{DIRS['svg']}/{name}.svg", "w") as f:
        f.write(to_svg(layers, w, h))
    img = render(to_svg(layers, w, h, out_w=256, out_h=int(256 * h / w)))
    img.save(f"{PREVIEW_CACHE}/{name}.png")
    return img


def export_raster(name, layers, w, h, out_w, out_h=None, defs="", folder="nodpi",
                  quality=88, lossless=False, extra_top="", extra_bottom="", bg=None):
    out_h = out_h or int(round(out_w * h / w))
    svg = to_svg(layers, w, h, out_w, out_h, defs=defs, extra_top=extra_top,
                 extra_bottom=extra_bottom)
    with open(f"{DIRS['svg']}/{name}.svg", "w") as f:
        f.write(to_svg(layers, w, h, defs=defs, extra_top=extra_top, extra_bottom=extra_bottom))
    img = render(svg, bg=bg)
    path = f"{DIRS[folder]}/{name}.webp"
    if bg:
        img.convert("RGB").save(path, "WEBP", quality=quality, method=6)
    else:
        img.save(path, "WEBP", quality=quality, method=6, lossless=lossless)
    img.save(f"{PREVIEW_CACHE}/{name}.png")
    return img, os.path.getsize(path)


def _font(size):
    for p in ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
              "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"]:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def sheet(names, out, cell=170, cols=6, bg="#F4F1EA", small=True, title=None):
    """Contact sheet: each asset large + at real 24dp@2x size."""
    rows = math.ceil(len(names) / cols)
    top = 44 if title else 0
    im = Image.new("RGB", (cols * cell, top + rows * (cell + 26)), bg)
    dr = ImageDraw.Draw(im)
    f = _font(12)
    if title:
        dr.text((12, 12), title, fill=INK, font=_font(18))
    for i, n in enumerate(names):
        src = Image.open(f"{PREVIEW_CACHE}/{n}.png").convert("RGBA")
        x0 = (i % cols) * cell
        y0 = top + (i // cols) * (cell + 26)
        big = src.copy()
        big.thumbnail((cell - 50, cell - 30))
        im.paste(big, (x0 + 6, y0 + 8 + (cell - 30 - big.height) // 2), big)
        if small:
            sm = src.copy()
            sm.thumbnail((48, 48))
            im.paste(sm, (x0 + cell - 52, y0 + cell - 56), sm)
        dr.text((x0 + 6, y0 + cell + 4), n, fill=INK, font=f)
    im.save(out)
    return im


def smooth(pts, closed=True, t=1.0):
    """Catmull-Rom spline through points -> cubic Shape."""
    n = len(pts)
    cmds = [("M", pts[0])]
    rng = range(n) if closed else range(n - 1)
    for i in rng:
        p0 = pts[i - 1] if (closed or i > 0) else pts[i]
        p1 = pts[i]
        p2 = pts[(i + 1) % n]
        p3 = pts[(i + 2) % n] if (closed or i + 2 < n) else p2
        c1 = (p1[0] + (p2[0] - p0[0]) * t / 6, p1[1] + (p2[1] - p0[1]) * t / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) * t / 6, p2[1] - (p3[1] - p1[1]) * t / 6)
        cmds.append(("C", c1, c2, p2))
    if closed:
        cmds.append(("Z",))
    return Shape(cmds)


def blob(cx, cy, rfun, n=48):
    """Closed smooth shape with radius r(theta) (theta in radians)."""
    pts = []
    for i in range(n):
        a = 2 * math.pi * i / n
        r = rfun(a)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return smooth(pts)


def tube(center, widths):
    """Polygon around a centerline with per-point half-widths (smooth)."""
    left, right = [], []
    n = len(center)
    for i, (x, y) in enumerate(center):
        a = center[max(i - 1, 0)]
        b = center[min(i + 1, n - 1)]
        dx, dy = b[0] - a[0], b[1] - a[1]
        ln = math.hypot(dx, dy) or 1
        nx, ny = -dy / ln, dx / ln
        w = widths[i]
        left.append((x + nx * w, y + ny * w))
        right.append((x - nx * w, y - ny * w))
    return smooth(left + right[::-1])


def bez_pts(p0, p1, p2, p3, n):
    out = []
    for i in range(n):
        t = i / (n - 1)
        mt = 1 - t
        out.append((mt**3*p0[0] + 3*mt*mt*t*p1[0] + 3*mt*t*t*p2[0] + t**3*p3[0],
                    mt**3*p0[1] + 3*mt*mt*t*p1[1] + 3*mt*t*t*p2[1] + t**3*p3[1]))
    return out


def outlined(shapes, fill, outline, w):
    """Stroke-under-fill: outline only on the outer silhouette of several shapes."""
    sh = shapes[0]
    for s in shapes[1:]:
        sh = sh + s
    return [L(sh, fill=outline, stroke=outline, sw=w * 2), L(sh, fill=fill)]


def sparkle(cx, cy, r):
    return P(f"M{cx},{cy-r} Q{cx+r*0.16},{cy-r*0.16} {cx+r},{cy} "
             f"Q{cx+r*0.16},{cy+r*0.16} {cx},{cy+r} Q{cx-r*0.16},{cy+r*0.16} {cx-r},{cy} "
             f"Q{cx-r*0.16},{cy-r*0.16} {cx},{cy-r} Z")
