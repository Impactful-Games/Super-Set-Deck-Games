# Writing the rules in AsciiDoc

The house rules for the `.adoc` rules of the site.
They apply to new rules and to everything you write in an existing one.
Older rules deviate from them in places: when editing one, leave what you don't touch as it is.

## Contents

- [Document header](#document-header)
- [Prose](#prose)
- [Inline formatting: single vs doubled markers](#inline-formatting-single-vs-doubled-markers)
- [Headings](#headings)
- [Admonitions](#admonitions)
- [Lists](#lists)
- [Examples and sidebars](#examples-and-sidebars)
- [Tables](#tables)
- [Cross-references, links and images](#cross-references-links-and-images)
- [Formulas](#formulas)


## Document header

The file starts with the document title as a level-0 heading, immediately followed by its attribute entries, then a blank line.

```adoc
= Schotten Totten
:toc: preamble
:toclevels: 4
:icons: font
```

```adoc
= Schotten Totten
:toc: preamble
:toclevels: 4
:toc-title: Sommaire
:icons: font
```

- One `=` title per file, and it is the first line: the game's name in the language of the file.
The site reads the game's displayed name from it.
- These attributes, in this order, and no others: the build sets the rest (`stem`, `lang`).
- No blank line between the title and the attributes.


## Prose

**Write one sentence per line.**
Never wrap a sentence across lines, and never put two sentences on one line.
This keeps diffs readable, and lets a reviewer compare the two languages sentence by sentence.

```adoc
Shuffle the cards and deal 7 cards to each player.
The remaining cards form the draw pile, face down.
```

Line length is a consequence of the sentence, not a target: never reflow to a column.

A trailing ` +` forces a line break inside a paragraph; use it only when the rendering needs it.

```adoc
♣ = +21 points +
♦ + Wolf = +15 points
```


## Inline formatting: single vs doubled markers

A single marker (`` ` ``, `*`, `_`, `#`) is **constrained**: Asciidoctor only recognises it at a word boundary.
Doubling it (```` `` ````, `**`, `__`, `##`) makes it **unconstrained**, which works anywhere.
Double the marker whenever the formatted span touches a word character or, for backticks, a quote.

```adoc
Collect the *K*s          // BROKEN — renders literally
Collect the **K**s        // correct

`Header`'s option         // BROKEN — renders as `Header's, backtick left visible
``Header``'s option       // correct

un*mid*believable         // BROKEN
un**mid**believable       // correct
```

When to double — the marker must **not** sit directly against:

| Side | Forbidden neighbour |
| --- | --- |
| After the closing marker | a letter, a digit, or `_` |
| Before the opening marker | a letter, a digit, `_`, `:`, `;`, `}`, `&`, `<`, `>` |

The backtick has one extra trap on **both** sides: `'` and `"`.
`` `' `` and `` `" `` are Asciidoctor's curly-quote syntax, so `` `Header`'s `` is parsed as a smart apostrophe and the backtick survives into the output.
A possessive after inline code therefore needs `` ``Header``'s ``, while `*Bold*'s` and `_Ital_'s` are fine as single markers.

- Whitespace directly inside a single-marker pair is never recognised (`* x*`); the doubled form accepts it, but trim the span instead.
- A preceding `\` escapes the marker; doubling does not override that.

Default to the single form, and reach for the doubled one for plurals, possessives and mid-word fragments.
Use bold for what the reader must not miss (a key restriction, a term being defined), and sparingly.


## Headings

- `=` document title, `==` section, `===` subsection, `====` sub-subsection: never skip a level.
- Never use Markdown `#` headings.
- Sentence case (`== End of the game`, not `== End Of The Game`), no trailing period or colon.
A question mark is fine when the heading is a question, as in a FAQ.
- One blank line **after** a heading, always.
- **Two blank lines before a heading that follows content, one before a heading that follows another heading.**
The two blank lines separate a section from the one that ended; where nothing ended, there is nothing to separate.

```adoc
== Setup

text


== Playing the game

=== Drawing a card

text
```

`Playing the game` closes the `Setup` section, so it gets two.
`Drawing a card` opens immediately under `Playing the game`, so it gets one: a gap there would suggest a missing body.

Give a heading an ID only when it is a cross-reference target.
**Set it with an attribute list — `[#id]`, never `[[id]]`.**
The double-bracket form is the legacy block-anchor syntax: it still renders, but `[#id]` is one syntax for IDs, roles and options instead of two.
IDs are English kebab-case, and the same in both languages, so that both files have the same anchors.

**An attribute list on a heading is part of that heading.**
There is never a blank line between the two, and the spacing rule counts the blank lines before the attribute list:

```adoc
text


[#two-players]
== Two-player game

text
```


## Admonitions

Single-line form for one sentence, block form for more:

```adoc
TIP: Begin with the scoring scheme A.

[CAUTION]
====
For this game, the numbers on the cards *do not* matter.
Only the suit of the cards matters.
====
```

- `NOTE` for context, `TIP` for optional advice, `IMPORTANT` for a rule that breaks expectations, `WARNING` for a rule whose omission ruins the game, `CAUTION` for something easy to get wrong — such as SSD cards whose printed information does not count.
- The label is AsciiDoc syntax, so it stays in English in both languages (`NOTE:`, `[CAUTION]`).
- Do not stack admonitions back to back to emphasise ordinary prose.


## Lists

- Unordered lists use `*`, nested with `**` — never `-`.
- Ordered lists use `.` (Asciidoctor numbers them), or explicit `1.`, `2.`, … when the text refers to the numbers.
- Attach a paragraph, a block or a nested sample to a list item with a `+` continuation line.

```adoc
. Shuffle the cards.
. Deal the cards:
+
NOTE: With 2 players, deal 9 cards each.
. The youngest player starts.
```


## Examples and sidebars

An example is an example block, titled `.Example` (`.Exemple` in French) or with a specific title, delimited by `====`.
Examples use SSD cards only.

```adoc
.Example
====
The leader plays ♥7, the next player ♥12, and the last player the Phoenix.
The Phoenix wins the trick.
====
```

The components block is the only sidebar of a rule: a `****` block with the `ssd-components` role (see `SKILL.md`).


## Tables

```adoc
[%autowidth, cols="^,^,^"]
|===
h| Cards
h| Points +
(scheme A)
h| Points +
(scheme B)

h| 1 | 1 | 1
h| 2 | 3 | 4
|===
```

- `[%autowidth]` keeps narrow tables narrow; declare `[cols=…]` when the default widths read badly.
- `h|` makes a header cell, `^` centres a column.
- Put each cell on its own line when cells hold prose; a table of short values may keep one row per line.
- Both languages have the same tables, with the same cells.


## Cross-references, links and images

- `<<anchor>>` or `<<anchor,link text>>` within a rule.
- Another game: `link:/#/games/<Id>[Game name]`.
- The game's printable board (`<Id>-Board.pdf`, made from the `board.designcraft` of its directory): a relative link, `link:Nyet-Board.pdf[the board]`.
- External URLs get an explicit label: `https://boardgamegeek.com/boardgame/5782[Coloretto on BoardGameGeek]`.
- Images: `image::file.png[Alt text, 700]` as a block, `image:file.png[Alt]` inline.
Rules rarely need one, and never for the game aid (`reminder.designcraft`), which the site shows by itself.


## Formulas

Write formulas with `stem:[…]`, in AsciiMath: `stem:[n xx 5]`, `stem:[(a + b) / 2]`.
The build converts them to MathML.
