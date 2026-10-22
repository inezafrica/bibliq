"""Compose a home-screen mock from the real asset files, to check they work together."""
import math
import re
from PIL import Image, ImageDraw
from lib import *
from lib import _font

A = ROOT
DP = 2.75  # px per dp on a 1080 px wide phone


def icon(name, dp, color_swap=None):
    svg = open(f"{A}/source-svg/{name}.svg").read()
    if color_swap:
        for a, b in color_swap.items():
            svg = svg.replace(a, b)
    vb = re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"', svg)
    w, h = float(vb.group(1)), float(vb.group(2))
    px = int(dp * DP)
    svg = re.sub(r'width="[\d.]+" height="[\d.]+"', f'width="{px}" height="{int(px*h/w)}"', svg, count=1)
    return render(svg)


def paste_c(base, im, cx, cy):
    base.paste(im, (int(cx - im.width / 2), int(cy - im.height / 2)), im)


scr = Image.open(f"{A}/android/res/drawable-nodpi/bg_world1_02.webp").convert("RGBA")
d = ImageDraw.Draw(scr)
nodes = [(560, 1600, "done", 3), (360, 1390, "done", 2), (600, 1170, "current", 0),
         (420, 900, "locked", 0), (640, 680, "locked", 0)]
# footprints between nodes
foot = icon("path_footprint", 16)
for (x0, y0, *_), (x1, y1, *_) in zip(nodes, nodes[1:]):
    n = 6
    for i in range(1, n):
        t = i / n
        x = x0 + (x1 - x0) * t + math.sin(t * math.pi) * 60
        y = y0 + (y1 - y0) * t
        ang = math.degrees(math.atan2(x1 - x0, -(y1 - y0)))
        f = foot.rotate(-ang, expand=True, resample=Image.BICUBIC)
        if i % 2:
            f = f.transpose(Image.FLIP_LEFT_RIGHT)
        paste_c(scr, f, x + (12 if i % 2 else -12), y)
num_font = _font(64)
for i, (x, y, kind, stars) in enumerate(nodes):
    name = {"done": "node_stone_done", "current": "node_stone_current", "locked": "node_scroll_locked"}[kind]
    im = icon(name, 70 if kind == "current" else 64)
    paste_c(scr, im, x, y)
    if kind != "locked":
        txt = str(i + 1)
        w = d.textlength(txt, font=num_font)
        d.text((x - w / 2, y - 44), txt, fill=STONE_LINE if kind == "done" else ROYAL, font=num_font)
    if stars:
        for k in range(3):
            s = icon("ic_star_filled" if k < stars else "ic_star_empty", 22)
            paste_c(scr, s, x + (k - 1) * 66, y - 120 - (12 if k == 1 else 0))
oli = Image.open(f"{A}/android/res/drawable-nodpi/oli_idle.webp").convert("RGBA").resize((240, 240))
paste_c(scr, oli, 820, 1130)
rib = Image.open(f"{A}/android/res/drawable-nodpi/ribbon_world.webp").convert("RGBA").resize((760, 194))
paste_c(scr, rib, 540, 470)
tf = _font(46)
t = "WORLD 1 · Creation"
d.text((540 - d.textlength(t, font=tf) / 2, 440), t, fill="#5A3220", font=tf)
chest = Image.open(f"{A}/android/res/drawable-nodpi/chest_treasure_closed.webp").convert("RGBA").resize((210, 185))
paste_c(scr, chest, 190, 1520)
# top bar
d.rounded_rectangle((-40, -40, 1120, 200), radius=60, fill=ROYAL)
badge = icon("badge_rank_1_sandals", 40)
paste_c(scr, badge, 110, 110)
f2 = _font(40)
d.text((180, 84), "Seeker", fill=WHITE, font=f2)
for x, ic, val in [(560, "ic_lamp_lit", "5"), (780, "ic_talent", "240")]:
    d.rounded_rectangle((x - 95, 72, x + 95, 148), radius=38, fill=WHITE)
    paste_c(scr, icon(ic, 26), x - 48, 110)
    d.text((x - 8, 86), val, fill=INK, font=f2)
paste_c(scr, icon("ic_settings", 26), 990, 110)
# play button + nav
d.rounded_rectangle((170, 1600 + 60, 910, 1600 + 60 + 4), radius=4, fill=None)
d.rounded_rectangle((180, 1676, 900, 1790), radius=50, fill=OLIVE_D)
d.rounded_rectangle((180, 1664, 900, 1776), radius=50, fill=OLIVE)
t = "PLAY LEVEL 3"
f3 = _font(52)
d.text((540 - d.textlength(t, font=f3) / 2, 1690), t, fill=WHITE, font=f3)
d.rectangle((0, 1810, 1080, 1920), fill=WHITE)
for i, n in enumerate(["ic_nav_map", "ic_nav_endless", "ic_nav_badges", "ic_nav_profile"]):
    sw = {INK: ROYAL} if i == 0 else {INK: "#9A96A0"}
    paste_c(scr, icon(n, 26, sw), 135 + i * 270, 1865)
scr.convert("RGB").save(f"{A}/previews/home_mock.png")
scr.convert("RGB").resize((540, 960)).save("/home/claude/bibliq/cache/home_mock_small.png")
print("ok")
