#!/usr/bin/env python3
"""Check a ported game directory (games/<Id>/) against the Super-Set Deck rule conventions.

Usage:
    python3 check_rule.py games/<Id> [--forbid NAME ...]

--forbid takes the names of the original game's cards (and components) that the rules must
never mention, as listed in the validated mapping. Matching is case-insensitive, on whole words.

Prints errors (exit code 1) and warnings (exit code 0 when there are only warnings), then a
summary of the cards game.yaml declares, to compare by hand with the components blocks.
Standard library only.
"""

import argparse
import difflib
import re
import sys
from collections import Counter
from pathlib import Path

# --------------------------------------------------------------------------------------------
# The deck
# --------------------------------------------------------------------------------------------


def _numbers(a, b):
    return [str(i) for i in range(a, b + 1)]


_CLASSIC = _numbers(0, 18) + ["J", "Q", "K", "K", "K", "S", "A"]

DECK = {
    "stars": Counter(_numbers(0, 21) + ["J", "Q", "K", "K", "K", "S", "C", "A", "×2", "-‽"]),
    "spades": Counter(_CLASSIC),
    "hearts": Counter(_CLASSIC),
    "clubs": Counter(_CLASSIC),
    "diamonds": Counter(_CLASSIC),
    "florettes": Counter(_numbers(0, 15) + ["J", "Q", "K", "K", "K", "A"]),
    "wheels": Counter(_numbers(0, 8) + ["J", "Q", "K", "K", "A", "►"]),
    "specials": Counter(
        ["Butterfly"] * 6 + ["Wolf"] * 6 + ["Phoenix"] * 4 + ["Snake"] * 4
        + ["Dragon"] * 2 + ["Monkey"] * 2 + ["Ø"]
    ),
}
assert sum(sum(c.values()) for c in DECK.values()) == 198

SUIT_ORDER = list(DECK)
SYMBOLS = {"★": "stars", "♠": "spades", "♥": "hearts", "♣": "clubs", "♦": "diamonds", "✿": "florettes", "⎈": "wheels"}
SUIT_SYMBOL = {v: k for k, v in SYMBOLS.items()}

SPECIAL_NAMES = {
    "en": {"Butterfly": r"Butterfl(?:y|ies)", "Wolf": r"Wol(?:f|ves)", "Phoenix": r"Phoenix(?:es)?",
           "Snake": r"Snakes?", "Dragon": r"Dragons?", "Monkey": r"Monkeys?"},
    "fr": {"Butterfly": r"Papillons?", "Wolf": r"Loups?", "Phoenix": r"Phénix",
           "Snake": r"Serpents?", "Dragon": r"Dragons?", "Monkey": r"Singes?"},
}

KNOWN_TYPES = {
    "Auction", "Bluffing", "Cooperative", "Deduction", "Draw & Write", "Drafting", "Ladder Climbing",
    "Memory", "Player Elimination", "Predictive Bid", "Push Your Luck", "Set Collection",
    "Single Loser", "Team Based", "Trick Taking",
}

HEADERS = {
    "en": [":toc: preamble", ":toclevels: 4", ":icons: font"],
    "fr": [":toc: preamble", ":toclevels: 4", ":toc-title: Sommaire", ":icons: font"],
}
COMPONENTS_TITLE = {"en": ".Components", "fr": ".Matériel"}

errors, warnings = [], []


def error(msg):
    errors.append(msg)


def warn(msg):
    warnings.append(msg)


# --------------------------------------------------------------------------------------------
# game.yaml (the restricted YAML subset the games use)
# --------------------------------------------------------------------------------------------


def parse_yaml(text):
    lines = []
    for raw in text.splitlines():
        if raw.lstrip().startswith("#"):
            continue
        line = re.sub(r"\s+#.*$", "", raw).rstrip()
        if line.strip():
            lines.append((len(line) - len(line.lstrip(" ")), line.strip()))

    def block(i, indent):
        if lines[i][1].startswith("- "):
            items = []
            while i < len(lines) and lines[i][0] == indent and lines[i][1].startswith("- "):
                items.append(lines[i][1][2:].strip().strip("\"'"))
                i += 1
            return items, i
        result = {}
        while i < len(lines) and lines[i][0] == indent:
            key, _, value = lines[i][1].partition(":")
            key, value = key.strip(), value.strip()
            if key in result:
                error(f"game.yaml: duplicate key '{key}'")
            i += 1
            if value:
                result[key] = value.strip("\"'")
            elif i < len(lines) and lines[i][0] > indent:
                result[key], i = block(i, lines[i][0])
            else:
                result[key] = None
        return result, i

    if not lines:
        return {}
    data, i = block(0, lines[0][0])
    if i != len(lines):
        error(f"game.yaml: could not parse from line '{lines[i][1]}' (bad indentation?)")
    return data


def expand_values(text):
    values = []
    for item in (v.strip() for v in str(text).split(",")):
        if m := re.fullmatch(r"([0-9]+)-([0-9]+)", item):
            values += _numbers(int(m[1]), int(m[2]))
        elif m := re.fullmatch(r"([0-9a-zA-Z]+)\*([0-9]+)", item):
            values += [m[1]] * int(m[2])
        elif item:
            values.append(item)
    return values


def expand_players(key, all_players):
    players = key.removesuffix("-players")
    if players == "all":
        return list(all_players)
    if m := re.fullmatch(r"([0-9]+)-([0-9]+)", players):
        return list(range(int(m[1]), int(m[2]) + 1))
    return [int(p) for p in players.split(",")]


def check_yaml(path):
    """Returns {variant: [(players, {suit: Counter})]} and the player counts."""
    data = parse_yaml(path.read_text(encoding="utf-8"))

    if not str(data.get("bggId", "")).isdigit():
        error("game.yaml: bggId is missing or not a number")
    if not (data.get("authors") or data.get("tradition")):
        warn("game.yaml: needs 'authors' (or 'tradition' for a traditional game)")
    types = data.get("type")
    if not isinstance(types, list) or not types:
        error("game.yaml: 'type' must be a non-empty list")
    else:
        for t in types:
            if t not in KNOWN_TYPES:
                warn(f"game.yaml: type '{t}' is not one the site translates; ask before adding it")
    try:
        players = [int(p) for p in str(data.get("players", "")).split(",")]
    except ValueError:
        error("game.yaml: 'players' must list player counts, like '2, 3, 4'")
        players = []

    cards = data.get("cards")
    if not isinstance(cards, dict) or "Base" not in cards:
        error("game.yaml: 'cards' must have a 'Base' variant")
        return {}, players

    configs = {}
    for variant, by_players in cards.items():
        configs[variant] = []
        if not isinstance(by_players, dict):
            error(f"game.yaml: cards.{variant} must map player keys to suits")
            continue
        for key, suits in by_players.items():
            if not key.endswith("-players"):
                error(f"game.yaml: cards.{variant}.{key}: player keys end with '-players'")
                continue
            try:
                counts = expand_players(key, players)
            except ValueError:
                error(f"game.yaml: cards.{variant}.{key}: bad player key")
                continue
            for p in counts:
                if p not in players:
                    error(f"game.yaml: cards.{variant}.{key}: {p} players is not in 'players'")
            parsed = {}
            for suit, values in (suits or {}).items():
                if suit not in DECK:
                    error(f"game.yaml: cards.{variant}.{key}: unknown suit '{suit}' (use {', '.join(SUIT_ORDER)})")
                    continue
                counter = Counter(expand_values(values))
                for value, n in counter.items():
                    available = DECK[suit][value]
                    if not available:
                        error(f"game.yaml: cards.{variant}.{key}: {suit} has no '{value}' card")
                    elif n > available:
                        error(f"game.yaml: cards.{variant}.{key}: {n} × {suit} {value}, the deck has {available}")
                parsed[suit] = counter
            configs[variant].append((counts, parsed))
    return configs, players


def union(configs, variants, players):
    """The cards needed for a player count and a set of variants: copies are a maximum, never a sum."""
    result = {}
    for variant in variants:
        for counts, suits in configs.get(variant, []):
            if players in counts:
                for suit, counter in suits.items():
                    result[suit] = result.get(suit, Counter()) | counter
    return result


def describe(cards):
    parts = []
    for suit in SUIT_ORDER:
        if suit not in cards or not cards[suit]:
            continue
        values = sorted(cards[suit].items(), key=lambda kv: (not kv[0].isdigit(), int(kv[0]) if kv[0].isdigit() else 0, kv[0]))
        parts.append(f"{suit}: " + ", ".join(v if n == 1 else f"{v}×{n}" for v, n in values))
    return "; ".join(parts)


def total(cards):
    return sum(sum(c.values()) for c in cards.values())


# --------------------------------------------------------------------------------------------
# AsciiDoc rules
# --------------------------------------------------------------------------------------------

DELIMITERS = {"====", "****", "|===", "----", "....", "--", "____", "++++", "////"}
VERBATIM = {"----", "....", "++++", "////"}
ADMONITIONS = "NOTE|TIP|IMPORTANT|WARNING|CAUTION"
VALUE = r"(?:1[0-9]|2[01]|[0-9]|[JQKSCA]|►|×2|-‽)"
SUIT = r"[★♠♥♣♦✿⎈]"
SUIT_VALUE = re.compile(rf"(?<![0-9A-Za-z×‽-])({SUIT})({VALUE})(?![0-9A-Za-z])")
VALUE_SUIT = re.compile(rf"(?<![0-9A-Za-z×‽-])({VALUE})({SUIT})")


class Rule:
    def __init__(self, path, lang):
        self.path, self.lang = path, lang
        self.lines = path.read_text(encoding="utf-8").splitlines()
        self.tokens = []  # (token, line number) describing the structure, for the comparison
        self.text_lines = []  # (line number, text) of prose, outside verbatim blocks and tables
        self.scan()

    def where(self, n):
        return f"{self.path.parent.name}/{self.path.name}:{n}"

    def scan(self):
        lines, stack = self.lines, []
        body_start = next((i for i, l in enumerate(lines) if not l.strip()), len(lines))
        prev_kind = "blank"
        for i in range(body_start, len(lines)):
            line, n = lines[i], i + 1
            stripped = line.strip()
            if stack and stack[-1] in VERBATIM:
                if stripped == stack[-1]:
                    stack.pop()
                continue
            if stripped.startswith("//") and stripped != "////":
                continue  # a comment line
            if stripped in DELIMITERS:
                if stack and stack[-1] == stripped:
                    stack.pop()
                else:
                    stack.append(stripped)
                    self.tokens.append((f"block {stripped}", n))
                prev_kind = "delimiter"
                continue
            in_table = "|===" in stack
            if not stripped:
                prev_kind = "blank"
                continue
            if in_table:
                cells = len(re.findall(r"(?<!\\)\|", line))
                self.tokens.append((f"cells {cells}", n))
                continue
            if "\t" in line:
                warn(f"{self.where(n)}: tab character, indent with spaces")
            kind = "text"
            if m := re.match(r"^(=+)\s+\S", line):
                self.tokens.append((f"heading {len(m[1])}", n))
                kind = "heading"
            elif m := re.match(r"^\[#([\w-]+)", stripped):
                self.tokens.append((f"id {m[1]}", n))
                kind = "attribute"
            elif m := re.match(r"^\[\[([\w-]+)\]\]$", stripped):
                warn(f"{self.where(n)}: '[[{m[1]}]]' is the legacy anchor syntax, use '[#{m[1]}]'")
                self.tokens.append((f"id {m[1]}", n))
                kind = "attribute"
            elif m := re.match(rf"^\[({ADMONITIONS})\]$", stripped):
                self.tokens.append((f"admonition {m[1]}", n))
                kind = "attribute"
            elif re.match(r"^\[.*\]$", stripped) and not stripped.startswith("[["):
                self.tokens.append((f"attributes {stripped}", n))
                kind = "attribute"
            elif re.match(r"^\.[^\s.]", stripped):
                self.tokens.append(("block title", n))
                kind = "attribute"
            elif stripped == "+":
                self.tokens.append(("continuation +", n))
                kind = "continuation"
            elif m := re.match(rf"^({ADMONITIONS}):\s", stripped):
                self.tokens.append((f"admonition {m[1]}", n))
                self.text_lines.append((n, stripped[len(m[1]) + 1:]))
            elif m := re.match(r"^(\*+|\.+|-|\d+\.|[A-Za-z]\.)\s+\S", stripped):
                marker = m[1]
                if marker == "-":
                    warn(f"{self.where(n)}: list item with '-', use '*'")
                    marker = "*"
                elif re.fullmatch(r"\d+\.", marker):
                    marker = "1."
                elif re.fullmatch(r"[A-Za-z]\.", marker):
                    marker = "A."
                self.tokens.append((f"item {marker}", n))
                self.text_lines.append((n, stripped[len(m[1]):].strip()))
            elif m := re.match(r"^image::?([^\[]+)\[", stripped):
                self.tokens.append((f"image {m[1]}", n))
            else:
                if prev_kind in ("blank", "delimiter", "attribute", "heading", "continuation"):
                    self.tokens.append(("paragraph", n))
                self.text_lines.append((n, stripped))
            for target in re.findall(r"link:([^\[\s]+\.pdf)\[", line):
                self.tokens.append((f"link {target}", n))
            prev_kind = kind

    def check_header(self):
        lines = self.lines
        if not lines or not re.match(r"^= \S", lines[0]):
            error(f"{self.where(1)}: the first line must be the '= Title' of the game")
            return
        attrs = []
        for line in lines[1:]:
            if not line.strip():
                break
            attrs.append(line.strip())
        if attrs != HEADERS[self.lang]:
            error(f"{self.where(2)}: header attributes must be exactly: {' / '.join(HEADERS[self.lang])} "
                  f"(found: {' / '.join(attrs) or 'none'})")

    def check_components(self):
        found = [i for i, l in enumerate(self.lines) if l.strip() == "[.ssd-components]"]
        if len(found) != 1:
            error(f"{self.where(1)}: needs exactly one '[.ssd-components]' block (found {len(found)})")
            return
        i = found[0]
        title = self.lines[i + 1].strip() if i + 1 < len(self.lines) else ""
        delimiter = self.lines[i + 2].strip() if i + 2 < len(self.lines) else ""
        if title != COMPONENTS_TITLE[self.lang] or delimiter != "****":
            error(f"{self.where(i + 1)}: the components block must be '[.ssd-components]', "
                  f"'{COMPONENTS_TITLE[self.lang]}', then '****'")
        first_heading = next((j for j, l in enumerate(self.lines) if re.match(r"^==\s", l)), len(self.lines))
        if i > first_heading:
            error(f"{self.where(i + 1)}: the components block must come before the first section")

    def check_spacing(self):
        lines = self.lines
        previous_heading_end = -1
        for i, line in enumerate(lines):
            if not re.match(r"^=+\s+\S", line) or i == 0:
                continue
            n = i + 1
            # The heading's attribute lines ([#id], [.role], [discrete]) are part of it.
            start = i
            while start > 0 and re.match(r"^\[[^\]]*\]$|^\[\[[\w-]+\]\]$", lines[start - 1].strip()):
                start -= 1
            blanks, j = 0, start - 1
            while j >= 0 and not lines[j].strip():
                blanks, j = blanks + 1, j - 1
            follows_heading = j == previous_heading_end
            expected = 1 if follows_heading else 2
            if j >= 0 and blanks != expected:
                what = "directly under another heading" if follows_heading else "after content"
                warn(f"{self.where(start + 1)}: {blanks} blank line(s) before a heading {what}, expected {expected}")
            if i + 1 < len(lines) and lines[i + 1].strip():
                warn(f"{self.where(n)}: a heading is followed by one blank line")
            if i + 2 < len(lines) and not lines[i + 1].strip() and not lines[i + 2].strip():
                warn(f"{self.where(n)}: more than one blank line after a heading")
            previous_heading_end = i
        levels = [(len(re.match(r"^(=+)", l)[1]), k + 1) for k, l in enumerate(lines[1:], 1) if re.match(r"^=+\s+\S", l)]
        last = 1
        for level, n in levels:
            if level > last + 1:
                warn(f"{self.where(n)}: heading level jumps from {last} to {level}")
            last = level

    def check_prose(self):
        abbreviation = re.compile(r"\b(?:e\.g|i\.e|etc|ex|cf|vs|p|n°|M|Mr|Mme|Dr|St)\.$", re.I)
        for n, text in self.text_lines:
            for m in re.finditer(r"[.!?…][\"»”)]?\s+(?=[A-ZÀÂÉÈÊÎÔÛÇ«\"“])", text):
                if not abbreviation.search(text[:m.start() + 1]):
                    warn(f"{self.where(n)}: two sentences on one line? write one sentence per line")
                    break
            if self.lang == "fr":
                cleaned = re.sub(r"`[^`]*`|https?://\S+|link:\S+|stem:\[[^\]]*\]|<<[^>]*>>|\b\w+!", "", text)
                if re.search(r"[^\s«(\[][:;!?](\s|$)", cleaned):
                    warn(f"{self.where(n)}: French typography puts a space before ':', ';', '!' and '?'")
                if re.search(r"[\"“”]", cleaned):
                    warn(f"{self.where(n)}: French quotes are « »")
                if re.search(r"\bjoueurs?\b(?!·)|\bjou·eur", text):
                    warn(f"{self.where(n)}: write 'joueur·euse' / 'joueur·euse·s'")
                if re.search(r"\bchacun\b(?!·)", text):
                    warn(f"{self.where(n)}: write 'chacun·e'")
            else:
                if re.search(r"\b(?:he|him|his|himself|she|her|hers|herself)\b", text, re.I):
                    warn(f"{self.where(n)}: gendered pronoun, use the singular 'they'")
            # An opening marker follows a space or an opening punctuation, so a closing one is never taken for it.
            for m in re.finditer(r"(?:^|(?<=[\s(\[«“]))\*([^*\s][^*]*?)\*(?=[A-Za-z0-9_])", text):
                warn(f"{self.where(n)}: '*{m[1]}*' touches a word character, double the markers ('**{m[1]}**')")
            for m in re.finditer(r"(?:^|(?<=[\s(\[«“]))`([^`\s][^`]*?)`(?=[A-Za-z0-9_'\"])", text):
                warn(f"{self.where(n)}: '`{m[1]}`' touches a word character or a quote, double the backticks")

    def card_mentions(self):
        """(line, suit, value, written) for every card mentioned with the notation of either language."""
        mentions = []
        for n, text in self.text_lines:
            for m in SUIT_VALUE.finditer(text):
                mentions.append((n, SYMBOLS[m[1]], m[2], "suit-value", m[0]))
            for m in VALUE_SUIT.finditer(text):
                mentions.append((n, SYMBOLS[m[2]], m[1], "value-suit", m[0]))
        return mentions


def check_cards(rule, all_cards):
    expected = "suit-value" if rule.lang == "en" else "value-suit"
    example = "★4, ♥K" if rule.lang == "en" else "4★, K♥"
    for n, suit, value, notation, written in rule.card_mentions():
        if notation != expected:
            warn(f"{rule.where(n)}: '{written}' — in {rule.lang}, write cards as {example}")
        if not DECK[suit][value]:
            error(f"{rule.where(n)}: '{written}' is not a card of the Super-Set Deck")
        elif all_cards is not None and not all_cards.get(suit, Counter())[value]:
            warn(f"{rule.where(n)}: '{written}' is not in game.yaml")
    specials = all_cards.get("specials", Counter()) if all_cards is not None else None
    title = rule.lines[0] if rule.lines else ""
    for n, text in rule.text_lines:
        for special, pattern in SPECIAL_NAMES[rule.lang].items():
            if re.search(rf"\b{pattern}\b", title):
                continue  # the word belongs to the game's name (Hungry Monkey)
            if specials is not None and re.search(rf"\b{pattern}\b", text) and not specials[special]:
                warn(f"{rule.where(n)}: mentions the {special} card, which is not in game.yaml")
        if specials is not None and "Ø" in text and not specials["Ø"]:
            warn(f"{rule.where(n)}: mentions the Ø card, which is not in game.yaml")


def check_forbidden(rule, names):
    for n, line in enumerate(rule.lines, 1):
        for name in names:
            # A short name (a letter, "Q2") is matched case-sensitively, and not as an elided word
            # ("S'il") nor as an inclusive-writing suffix ("joueur·euse·s").
            short = len(name) <= 2
            pattern = rf"(?<![\w·]){re.escape(name)}(?![\w'’])" if short else rf"(?<!\w){re.escape(name)}(?!\w)"
            if re.search(pattern, line, 0 if short else re.I):
                error(f"{rule.where(n)}: mentions '{name}', a card of the original game; name the SSD card")


def compare(en, fr):
    a, b = [t for t, _ in en.tokens], [t for t, _ in fr.tokens]
    matcher = difflib.SequenceMatcher(a=a, b=b, autojunk=False)
    for op, i1, i2, j1, j2 in matcher.get_opcodes():
        if op == "equal":
            continue
        left = ", ".join(f"{t} (l.{n})" for t, n in en.tokens[i1:i2]) or "nothing"
        right = ", ".join(f"{t} (l.{n})" for t, n in fr.tokens[j1:j2]) or "nothing"
        error(f"structure differs: en.adoc has {left} where fr.adoc has {right}")


# --------------------------------------------------------------------------------------------


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("game_dir", type=Path)
    parser.add_argument("--forbid", nargs="*", default=[], help="names of the original cards, never to be cited")
    args = parser.parse_args()
    game = args.game_dir

    if not re.fullmatch(r"[A-Za-z0-9_]+", game.name):
        error(f"{game.name}: the directory name uses only ASCII letters, digits and '_'")
    for name in ("game.yaml", "en.adoc", "fr.adoc"):
        if not (game / name).is_file():
            error(f"{game.name}/{name} is missing")
    if errors:
        report()

    configs, players = check_yaml(game / "game.yaml")
    variants = [v for v in configs if v != "Base"]
    all_cards = {}
    for variant in configs:
        for counts, suits in configs[variant]:
            for suit, counter in suits.items():
                all_cards[suit] = all_cards.get(suit, Counter()) | counter

    rules = {lang: Rule(game / f"{lang}.adoc", lang) for lang in ("en", "fr")}
    for rule in rules.values():
        rule.check_header()
        rule.check_components()
        rule.check_spacing()
        rule.check_prose()
        check_cards(rule, all_cards if configs else None)
        check_forbidden(rule, args.forbid)
    compare(rules["en"], rules["fr"])

    # Player counts that need the same cards are grouped on one line.
    groups = {}
    for p in players:
        base = union(configs, ["Base"], p)
        groups.setdefault(("Base", f"{total(base)} cards — {describe(base)}"), []).append(p)
        for variant in variants:
            both = union(configs, ["Base", variant], p)
            extra = {s: both[s] - base.get(s, Counter()) for s in both}
            if total(extra):
                groups.setdefault((f"+ {variant}", f"{total(extra)} more cards — {describe(extra)}"), []).append(p)
    summary = [f"  {', '.join(map(str, ps))} players, {what}: {cards}" for (what, cards), ps in groups.items()]
    report(summary)


def report(summary=None):
    for msg in errors:
        print(f"ERROR   {msg}")
    for msg in warnings:
        print(f"WARNING {msg}")
    if summary:
        print("\nCards declared in game.yaml (compare with both components blocks):")
        print("\n".join(summary))
    print(f"\n{len(errors)} error(s), {len(warnings)} warning(s)")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
