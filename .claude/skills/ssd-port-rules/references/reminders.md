# Making the game aids with designcraft-cli

A game's `reminder.designcraft` holds its game aids: reference cards that players keep on the table, shown by the site on its own (the rule never links to them).
You can make it with `designcraft-cli`, the command-line DesignCraft (`~/.local/bin/designcraft-cli`).

## Contents

- [When to make one](#when-to-make-one)
- [The house style](#the-house-style)
- [How to build it](#how-to-build-it)
- [Checking it](#checking-it)
- [Gotchas](#gotchas)


## When to make one

- The original game has aid or overview cards (a card per player count, a scoring summary, a turn summary).
- Players need at the table information that the SSD cards do not print: which cards to use per player count, the order or value of figures, a scoring table.

Make one when the user asks, and offer it when one of these applies.
Each page is one aid card: one page per player count, per role, or per topic.
The aid is language-neutral: symbols, icons, numbers and SSD card names (J, Q, ★…), not English or French words.


## The house style

Read two or three existing aids first, by rendering them (see [Checking it](#checking-it)): `games/Apocalypse`, `games/Whot`, `games/Kobayakawa`, `games/Tarot`.

- **Page**: 77 × 102 mm (218.27 × 289.13 pt), which includes a 3.5 mm print bleed on every side, removed by the site's export: the visible card is 70 × 95 mm.
Keep the content inside the 7 mm margins (x 19.84-198.43 pt, y 19.84-269.29 pt).
- **Background**: a black rectangle covering the whole page, on the locked `BG` layer; the content goes on the `Card` layer.
- **Title**: the game's name, centred at the top, `LCT Picon Extended` Bold 24 pt, white.
- **Text**: white; numbers and card names in `LCT Picon Extended` Bold; separators (`:`) in `LCT Picon` regular, smaller.
Turn off ligatures (`ligatures: false`).
- **Suit symbols**: `LCT Picon` regular, in the suit's colour, with the swatches of the existing aids:

  | Suit | Swatch |
  | --- | --- |
  | ♠ | `R=87 G=255 B=255` |
  | ♥ | `R=255 G=171 B=171` |
  | ♣ | `R=171 G=255 B=171` |
  | ♦ | `R=255 G=255 B=87` |
  | ★ | `R=199 G=199 B=199` |
  | ✿ | `R=255 G=87 B=255` |
  | ⎈ | `R=171 G=171 B=255` |

  Check the swatch names in the template you start from (`document.json`, `swatches`).
- **Icons**: white SVG icons from game-icons.net (CC BY 3.0), placed as images: `https://game-icons.net/icons/ffffff/transparent/1x1/<author>/<name>.svg` (for example `lorc/monkey`, `delapouite/person`).
Downloading needs the network, outside the sandbox.
Icons already used by the aids are inside their archives (`assets/<id>.png`, which may hold SVG data): the Butterfly (`butterfly_white.svg`) is in `games/Apocalypse/reminder.designcraft`.
Tell the user which icons you added and their authors.
- **Layout**: rows of `icon or symbol : value`, aligned on a common separator column, read top to bottom.
What players must see first is the biggest; secondary information (a value only used for scoring) is small and grey under or beside it.


## How to build it

Generate a `designcraft-cli` script from a Python program kept in your scratchpad, and run it on a copy of an existing aid used as template, so that the page size, layers, swatches and styles are the house ones.
`assets/reminder_example.py` is the program that made `games/Fool/reminder.designcraft`: copy it, change its pages and layout, and run it.

```bash
python3 -I /path/to/scratchpad/reminder.py --icons /path/to/icons --out games/<Id>/reminder.designcraft [--script /path/to/scratchpad/steps.txt]
```

It reads the template's `document.json` to keep only its background and title, sets the title, duplicates the page once per aid, then fills each page.

```bash
designcraft-cli describe frame.create          # the parameters of a command
designcraft-cli commands text.                 # the commands matching a filter
designcraft-cli script steps.txt --in games/Apocalypse/reminder.designcraft --save /path/to/out.designcraft
```

The commands it needs:

- `edit.clear {ids}` to remove the template's content (keep the background and the title frame), `story.setText {story, text}` to change the title.
- `layout.pages.duplicateSpread {spread: 0, atEnd: true}` to make one page per aid, background and title included, before filling them.
- `frame.create {spread, rect: [x0, y0, x1, y1], content: "text", text}` → `{id, story}`; then `text.select {story, anchor, focus}`, `type.char {attrs}` and `type.para {attrs}` style the selection; `object.textFrameOptions {ids, verticalJustification: "center", inset: 0}` centres it vertically.
- `file.place {path, spread, x, y, width}` places an image in a new frame.

In a script, `$N.story` or `$N.id` refers to the result of step N (0-based, comments not counted).


## Checking it

```bash
designcraft-cli run --in games/<Id>/reminder.designcraft --scale 2 --all-pages /path/to/dir   # page-001.png…
./gradlew :app:designExport          # the site's export: app/build/generated/site/designs/games/<Id>/R-<n>.png
```

Look at every page: nothing outside the margins, rows aligned, nothing overset (`story.get` reports `overset`).
The export task fails if a font is missing.
Show the user the rendered pages, and adjust until they approve.


## Gotchas

- `text.select` offsets are **UTF-8 byte offsets**: ★, ♠ or ‽ count 3, "é" counts 2.
- `type.char` and `type.para` apply to the current selection: select first.
- The LCT Picon fonts draw ‽ exactly like "?": `-‽` reads `-?` on an aid (the Tarot aid has it too).
Say so to the user.
- `designcraft-cli run --page N` counts pages from 0, while `--all-pages` names files from `page-001.png`.
- Parameters a command does not know are ignored silently: check their names with `designcraft-cli describe`.
