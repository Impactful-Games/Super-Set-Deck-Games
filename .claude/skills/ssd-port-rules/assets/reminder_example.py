#!/usr/bin/env python3
"""The program that made games/Fool/reminder.designcraft: one reference card per player count.

Copy it to your scratchpad, change TITLE, the icons, PAGES and the layout of build(), then run it from the
repository's root:

    python3 -I reminder_example.py --icons ICONS_DIR --out games/<Id>/reminder.designcraft

It generates a designcraft-cli script (written to --script if given, to debug it) and runs it on a copy of an
existing aid (--template), so that the page size, layers, swatches and styles are the house ones.
See references/reminders.md.
"""
import argparse
import json
import subprocess
import sys
import zipfile
from pathlib import Path

TITLE = "Fool!"

WHITE = "R=255 G=255 B=255"
GREY = "R=199 G=199 B=199"
SUIT_COLOR = {
    "♠": "R=87 G=255 B=255", "♥": "R=255 G=171 B=171", "♣": "R=171 G=255 B=171", "♦": "R=255 G=255 B=87",
    "★": GREY, "✿": "R=255 G=87 B=255", "⎈": "R=171 G=171 B=255",
}
BOLD = {"fontFamily": "LCT Picon Extended", "fontStyle": "Bold", "fill": WHITE, "ligatures": False}
REGULAR = {"fontFamily": "LCT Picon", "fill": WHITE, "ligatures": False}

# One page per player count: players, cards per suit, Butterflies, Monkeys, figures in order.
PAGES = [
    (4, [("★", "0-15"), ("♠", "0-11"), ("♥", "0-9"), ("♣", "0-6")], 3, 1, []),
    (5, [("★", "0-18"), ("♠", "0-14"), ("♥", "0-12"), ("♣", "0-8")], 4, 1, []),
    (6, [("★", "0-18 J Q S"), ("♠", "0-17"), ("♥", "0-15"), ("♣", "0-10")], 5, 1, ["J", "Q", "S"]),
    (7, [("★", "0-18 J-A"), ("♠", "0-18 J Q"), ("♥", "0-18"), ("♣", "0-12")], 6, 2, ["J", "Q", "S", "K", "C", "A"]),
    (8, [("★", "0-18 J-‽"), ("♠", "0-18 J-S"), ("♥", "0-18 J"), ("♣", "0-13")], 6, 2, ["J", "Q", "S", "K", "C", "A", "-‽"]),
]

steps = []


def step(cmd, params=None):
    """Adds a script step; returns its index, for "$N.story" / "$N.id" references."""
    steps.append(cmd + ("" if params is None else " " + json.dumps(params, ensure_ascii=False)))
    return len(steps) - 1


def text(spread, rect, txt, attrs, align, size, leading=None):
    """A text frame, its whole text styled, vertically centred. Returns the frame.create step."""
    i = step("frame.create", {"spread": spread, "rect": rect, "content": "text", "text": txt})
    step("text.select", {"story": f"${i}.story", "anchor": 0, "focus": len(txt.encode())})  # UTF-8 byte offsets
    a = dict(attrs, size=size)
    if leading:
        a["leading"] = {"kind": "points", "value": leading}
    step("type.char", {"attrs": a})
    step("type.para", {"attrs": {"align": align, "hyphenate": False}})
    step("object.textFrameOptions", {"ids": [f"${i}.id"], "verticalJustification": "center", "inset": 0})
    return i


def restyle(frame_step, start, end, attrs):
    """Restyles the UTF-8 byte range [start, end[ of a frame's text."""
    step("text.select", {"story": f"${frame_step}.story", "anchor": start, "focus": end})
    step("type.char", {"attrs": attrs})


def icon(spread, path, x, y, size):
    step("file.place", {"path": str(path), "spread": spread, "x": x, "y": y, "width": size})


def template_page(template):
    """The template's items to clear, and the story of its title: everything but the background and the title."""
    with zipfile.ZipFile(template) as z:
        doc = json.loads(z.read("document.json"))
    bg = {layer["id"] for layer in doc["layers"] if layer["name"] == "BG"}
    items = doc["spreads"][0]["items"]
    texts = [it for it in items if it.get("content", {}).get("type") == "text" and it["layer"] not in bg]
    title = min(texts, key=lambda it: it["xf"][5] + min(a["p"][1] for a in it["path"]["subpaths"][0]["anchors"]))
    clear = [it["id"] for it in items if it["layer"] not in bg and it is not title]
    return clear, title["content"]["story"]


def build(icons):
    col = 94  # The separator column: left elements end before it, values start after it.
    for spread, (players, suits, butterflies, monkeys, figures) in enumerate(PAGES):
        row = 27
        height = 30 + row * len(suits) + 32 + (10 + 34 if figures else 0)
        y = 46 + (222 - height) / 2  # Centred between the title and the bottom margin.

        # Players
        icon(spread, icons / "person.svg", col - 30, y + 3, 24)
        text(spread, [col - 4, y, col + 8, y + 30], ":", REGULAR, "center", 22)
        text(spread, [col + 8, y, 200, y + 30], str(players), BOLD, "left", 26)
        y += 32

        # Suits
        for suit, cards in suits:
            s = text(spread, [col - 40, y, col - 6, y + row], suit, REGULAR, "right", 24)
            restyle(s, 0, len(suit.encode()), {"fill": SUIT_COLOR[suit]})
            text(spread, [col - 4, y, col + 8, y + row], ":", REGULAR, "center", 18)
            text(spread, [col + 8, y, 200, y + row], cards, BOLD, "left", 16)
            y += row

        # Butterflies and Monkeys, on one row
        y += 4
        icon(spread, icons / "butterfly_white.svg", 40, y + 4, 26)
        text(spread, [68, y, 78, y + 30], ":", REGULAR, "center", 20)
        text(spread, [78, y, 104, y + 30], str(butterflies), BOLD, "left", 22)
        icon(spread, icons / "monkey.svg", 116, y + 3, 26)
        text(spread, [144, y, 154, y + 30], ":", REGULAR, "center", 20)
        text(spread, [154, y, 180, y + 30], str(monkeys), BOLD, "left", 22)
        y += 30

        # Figures: their order in big, with their value (for the scoring only) small and grey below
        if figures:
            y += 10
            width = min(30, 178 / len(figures))
            x = 109 - width * len(figures) / 2
            for n, letter in enumerate(figures):
                t = f"{letter}\n{19 + n}"
                f = text(spread, [x, y, x + width, y + 34], t, BOLD, "center", 20, 20)
                restyle(f, len(letter.encode()) + 1, len(t.encode()),
                        {"size": 8, "leading": {"kind": "points", "value": 11}, "fill": GREY})
                x += width


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--template", default="games/Apocalypse/reminder.designcraft")
    p.add_argument("--icons", required=True, type=Path, help="folder of the white SVG icons")
    p.add_argument("--out", required=True, type=Path)
    p.add_argument("--script", type=Path, help="also write the generated script there")
    args = p.parse_args()

    clear, title_story = template_page(args.template)
    step("edit.clear", {"ids": clear})
    step("story.setText", {"story": title_story, "text": TITLE})
    for _ in range(len(PAGES) - 1):
        step("layout.pages.duplicateSpread", {"spread": 0, "atEnd": True})
    build(args.icons.resolve())

    script = "\n".join(steps) + "\n"
    if args.script:
        args.script.write_text(script)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    r = subprocess.run(["designcraft-cli", "script", "-", "--in", args.template, "--save", str(args.out)],
                       input=script, text=True, capture_output=True)
    if r.returncode != 0:
        print(r.stdout[-3000:], r.stderr[-3000:])
        sys.exit(1)
    print(f"saved {args.out} ({len(steps)} steps)")


if __name__ == "__main__":
    main()
