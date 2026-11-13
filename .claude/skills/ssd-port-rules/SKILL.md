---
name: ssd-port-rules
description: Port a card game to the Super-Set Deck (SSD) in this repository — map every card and component of the original game to SSD cards and get that mapping explicitly validated, then write games/<Id>/game.yaml and the English and French rules (en.adoc, fr.adoc) with the same structure, citing only SSD cards, and check them. Use whenever the user asks to add, port, adapt or translate a game for the Super-Set Deck or the super-set-deck.games site ("porte Love Letter au SSD", "ajoute The Crew", "adapt this game's rules"), to write or fix a rule or a game.yaml under games/, or to find which SSD cards can replace a game's cards — even if they only name the game.
---

# Porting a game to the Super-Set Deck

The Super-Set Deck (SSD) is a single deck of 198 cards, designed to play a large number of existing card games.
This repository is its companion site: each game is a directory `games/<Id>/` with the game's properties (`game.yaml`) and its rule in English (`en.adoc`) and French (`fr.adoc`), in AsciiDoc.

A reader of these rules owns a Super-Set Deck, not the original game.
Everything below follows from that: the rule must be playable with the SSD alone, it names SSD cards only, and the English and French rules are the same document in two languages.

Read `references/deck.md` before mapping, and `references/asciidoc.md` before writing a rule.


## Workflow

1. Read the original rules.
2. Build the mapping, and **wait for its explicit validation**.
3. Write `game.yaml`.
4. Write `en.adoc` and `fr.adoc`.
5. Add the game to the changelog.
6. Check.
7. Report.

Talk to the user in their language; the files follow the conventions below whatever that language is.


## 1. Read the original rules

- Port from the rules the user gives (a file, a text, a URL).
Without them, find the publisher's official rules, in English and in French: the French edition's vocabulary is the one French players know.
If you find neither, ask.
Do not port from memory: a ported rule must be complete and exact.
- Ideally, the rules come in both languages, an English rulebook and a French one.
Then your job is to extract them and port them to the SSD, not to translate: each rule is ported from the rulebook in its own language.
- **If you only have the rules in one language, ask the user explicitly for permission to translate them**, and offer them the alternative of giving the rulebook in the other language.
Ask now, together with the mapping validation at the latest, and never write a rule in a language you have no rulebook for without a clear yes.
- Check that the game is not already in `games/`, by directory and by rule title.
- List what the mapping needs: the player counts, the variants and expansions, every card with its number of copies, every component that is not a card (tokens, boards, markers, score pads), every card power, and the scoring.


## 2. Build the mapping, and wait for its validation

The mapping says which SSD cards stand for each card and component of the original game.
Build it with the deck's composition and the heuristics of `references/deck.md`, and present it as the table described there: every card and component, per variant and player count, the totals, the doubts, and the original names that the rules will never cite.

**Then stop, and ask the user to validate the mapping explicitly.**
Do not create or edit any file of the game before they do.
The mapping drives everything that follows — `game.yaml`, both rules, every example — so a late change means rewriting all of it, and the user knows the deck and the existing ports better than any heuristic.
If the answer changes the mapping, present the updated table and ask again; only a clear approval counts.


## 3. Write game.yaml

`assets/game.yaml` is an annotated example.

- **Directory**: `games/<Id>/`, where `<Id>` is the game's English name in ASCII letters and digits, with spaces and hyphens as `_` and other punctuation dropped: `Fox_in_Forest`, `Jalape_No` (Jalape-No!), `Whot` (Whot!).
The site's URLs use it, so it never changes once published.
- **`bggId`**: the BoardGameGeek identifier, from the game's URL (`boardgamegeek.com/boardgame/<bggId>/…`).
The site takes the game's picture, rating, year, duration and minimum age from BoardGameGeek.
If you cannot find it for sure, ask.
- **`authors`**: comma separated.
A traditional game has `tradition: French` (or `Chinese`) instead; `adaptedBy:` names whoever adapted it.
- **`type`**: one or more of the types the site translates: Auction, Bluffing, Cooperative, Deduction, Draw & Write, Drafting, Ladder Climbing, Memory, Player Elimination, Predictive Bid, Push Your Luck, Set Collection, Single Loser, Team Based, Trick Taking.
A new type needs a French translation in `app/src/webMain/kotlin/games/supersetdeck/i18n/FrStrings.kt`: ask before adding one.
- **`players`**: every supported player count, comma separated: `2, 3, 4, 5`.
- **`cards`**: the validated mapping.
  * First level: the variant. `Base` holds the cards always needed; other variants (`Advanced`, `Tactic`…) are optional and add to `Base`.
  * Second level: the player counts the block applies to: `all-players`, `5-players`, `4,5-players`, `4-6-players`.
  * Third level: the suits, `stars`, `spades`, `hearts`, `clubs`, `diamonds`, `florettes`, `wheels` and `specials`, each with comma-separated values: ranges (`1-12`), single values (`J`, `A`, `×2`, `-‽`, `►`, `Ø`, `Phoenix`), and `V*n` for n copies (`K*3`, `Butterfly*4`).
  * Blocks are merged by union: for a player count, the site takes every block that applies, and a card listed in several blocks takes the **largest** count, never the sum.
So a block for some player counts lists the cards it adds, and for a card already in `all-players`, its total count.

The site's "Show cards" button and its travel packer read these cards: they must be exactly the cards the rule's components block lists.


## 4. Write the rules

Start from `assets/en.adoc` and `assets/fr.adoc`, and read `references/asciidoc.md` first.
The sections of the templates are placeholders: the rule follows the structure of the original rules.

### Structure

1. **Header**: the title, which is the game's name in the language of the file (its official French title when there is one), then the fixed attributes.
2. **Components block**, right after the header (see below).
3. **Preamble**: one or two short paragraphs before the first section, presenting the theme and the goal of the game.
The table of contents comes after it (`:toc: preamble`).
4. **The sections of the original rules**, in their order: setup, play, end of the game, scoring, variants, player-count adaptations, FAQ… with their examples.
Keep the original's content complete — every rule, every variant, every player count, every tie-breaker — and add nothing it does not say, apart from the adaptation to the SSD.

### The components block

```adoc
[.ssd-components]
.Components
****
* 54 Clan cards: 1-9 in ♠, ♥, ♣, ♦, ✿ & ★.
* 10 Tactic cards (_Tactic variant_):
** 4 Elite Troops: 7, J & 2 K in ⎈.
** 6 specials: Wolf, Dragon, Monkey, Butterfly, Snake & Phoenix.
* 9 Stone cards: the 3 K of ♠, ♥ & ♣.
****
```

- Titled `.Components` in English and `.Matériel` in French; the site adds a "Show cards" button to it.
- Each item gives a count, the role of the cards when the original game names one, and the exact SSD cards.
- The cards of a variant or of some player counts say so; a table can show an irregular set (see `games/Whot/en.adoc`).
- Components that are not cards are listed too: a printable, paper and a pencil.
- The counts add up to the cards of `game.yaml`.

### Naming cards

The rules cite SSD cards, and never the cards of the original game.
A reader has never seen the original cards: a name like "the Mermaid", "the red 7" or "the Dog" means nothing to them, while ★7 or the Wolf is in their hand.

- Name every card by its SSD identity: a suit and a value, a special's name, or a unique card, with the notation of `references/deck.md`: **★4 in English, 4★ in French**.
- Name suits by their symbol (♠ ♥ ♣ ♦ ✿ ⎈ ★), never by their printed color (cyan, pink…) or by the original suit's name.
- Name figures by the letter printed on them (J, Q, K, S, C, A).
- A role name of the original game (Clan cards, Tactic cards, trumps) is fine when the components block defines it with SSD cards.
The proper name of an original card is never fine, even in passing ("the Phoenix, the original game's Mermaid").
- Where SSD cards carry printed information the game ignores, or that could mislead, warn with a `CAUTION` after the preamble: "For this game, the numbers on the cards do not matter."
- Where an SSD card replaces a component (a marker, a stone), say in the setup what it is used for.

### The two languages

The English and French rules are the same document: same sections in the same order, same IDs, same paragraphs, lists, tables, examples and admonitions.
Readers switch between them, and a later fix must be made in both: matching structures make both easy.

- With a rulebook in each language, port each rule from its own rulebook, block by block, keeping each paragraph, item and cell aligned with the other rule.
Where the two editions differ (a missing example, a reworded rule), follow their common content and tell the user about the difference.
- With a rulebook in one language only, and only once the user has allowed it (see step 1), write the rule in that language, then translate it block by block, keeping each paragraph, item and cell aligned.
- IDs (`[#scoring]`) are English kebab-case, identical in both files.
- Any change to one file is made to the other in the same change.

**English**

- Address the players with the imperative and "you": "Shuffle the cards", "you draw a card".
- For a player in the third person, use the singular "they": "the player who wins the trick leads; they play any card".
- Card notation: suit then value, ★4, ♥K; ranges "1-9 in ♠, ♥ & ♣".

**French**

- Address the players with the imperative and "vous": « Mélangez les cartes », « vous piochez une carte ».
- Use inclusive writing with the middle dot: « joueur·euse », « joueur·euse·s », « chacun·e », « celui ou celle ».
Rephrase rather than pile up forms: « la personne qui gagne le pli », « vous ».
- Card notation: value then suit, 4★, K♥; ranges « 1-9 à ♠, ♥ & ♣ ».
- A suit is « une couleur », as in every French card game: « la dernière carte d’une couleur », « 6 couleurs ».
Never « enseigne ».
- Specials: Papillon, Loup, Phénix, Serpent, Dragon, Singe; « la carte Ø ».
- French typography: a space before `:`, `;`, `!` and `?`, and « » quotes.
- Use the official French edition's vocabulary when it exists (« Borne », « Clan »).

### Printables, game aids, other links

- A board or a score sheet the game needs is the game directory's `board.designcraft`, whose PDF `<Id>-Board.pdf` the rule links relatively: `link:Nyet-Board.pdf[the board]`.
You cannot make it: describe what it must contain, and tell the user.
- The game directory's `reminder.designcraft` is the game aid the site shows on its own; the rule never links to it.
- Link another game with `link:/#/games/<Id>[Name]`.


## 5. Add the game to the changelog

`docs/changelog.adoc` lists the new games by month, newest first.
Add the game in the current month's section, creating it at the top when it does not exist yet, with the spacing rules of `references/asciidoc.md`:

```adoc
= Changelog


== 11/2026

* Games:
** link:/#/games/Coloretto[Coloretto]
* Other changes of the month.
```

The link text is the game's English title.


## 6. Check

1. Run the checker, with the original names from the mapping:

   ```bash
   python3 .claude/skills/ssd-port-rules/scripts/check_rule.py games/<Id> --forbid "Mermaid" "Skull King" "Tigress"
   ```

   It checks `game.yaml` against the deck, the headers and components blocks, the card notation, the cards cited against `game.yaml`, the forbidden names, the AsciiDoc and typography rules, and compares the structures of `en.adoc` and `fr.adoc`.
   Fix every error.
   Fix the warnings too, unless one is plainly a false positive (an abbreviation, a game name with a "!"), and say which ones you left.
   Compare the card summary it prints with both components blocks.
2. Render the rules with `./gradlew :app:docs`: the build must pass without Asciidoctor warnings.
   The HTML lands in `app/build/generated/site/docs/games/<Id>/`.
3. Read both rules once more against the original rules: no rule, variant, player count or end condition missing, every card with a power described, every example consistent with the mapping and with the rules.


## 7. Report

Tell the user, in their language: the files created or changed, a reminder of the mapping, which rule was translated rather than ported from its own rulebook (if any), the differences found between the two editions, the checker's result (and the warnings you left, with why), and anything left to them — an uncertain `bggId`, a printable to make, a new type, a doubt about the original rules.


## Editing an existing game

The same conventions apply to whatever you write in an existing game, and the two languages stay in sync.
Older rules predate some of them (`[[id]]` anchors, `-` lists, « joueurs », mixed notations): do not rewrite parts you were not asked to touch, but mention the inconsistencies you notice.
Changing a mapping that is already published needs the same explicit validation as a new one.
