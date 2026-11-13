# The Super-Set Deck

Everything an agent needs to know about the deck to map a game onto it and to name its cards.
The rules of the site cite these cards, and only these cards.

## Contents

- [Composition](#composition)
- [Which suits have a value](#which-suits-have-a-value)
- [Names of the cards](#names-of-the-cards)
- [Mapping heuristics](#mapping-heuristics)
- [Presenting the mapping](#presenting-the-mapping)


## Composition

198 cards: seven suits and the specials.

| `game.yaml` key | Symbol | English | French | Values | Cards |
| --- | --- | --- | --- | --- | --- |
| `stars` | ★ | Stars | Étoiles | 0–21, J, Q, K ×3, S, C, A, ×2, -‽ | 32 |
| `spades` | ♠ | Spades | Piques | 0–18, J, Q, K ×3, S, A | 26 |
| `hearts` | ♥ | Hearts | Cœurs | 0–18, J, Q, K ×3, S, A | 26 |
| `clubs` | ♣ | Clubs | Trèfles | 0–18, J, Q, K ×3, S, A | 26 |
| `diamonds` | ♦ | Diamonds | Carreaux | 0–18, J, Q, K ×3, S, A | 26 |
| `florettes` | ✿ | Florettes | Fleurettes | 0–15, J, Q, K ×3, A | 22 |
| `wheels` | ⎈ | Wheels | Roues | 0–8, J, Q, K ×2, A, ► | 15 |
| `specials` | — | Specials | Spéciales | Butterfly ×6, Wolf ×6, Phoenix ×4, Snake ×4, Dragon ×2, Monkey ×2, Ø | 25 |

- Every value exists once per suit, except K: three copies in every suit but ⎈, which has two.
- J, Q, K, S and C cards show an animal illustration, and A shows the suit symbol.
Rules name them by the letter printed on them, never by a translated name such as "Jack", "Valet" or "Roi".
- ×2, -‽ (both ★) and ► (⎈) are unique cards, printed with their glyph instead of a value.
- Ø is a unique special card, with no suit.
- The suit colors on the cards are ♠ cyan, ♥ pink, ♣ green, ♦ yellow, ★ grey, ✿ magenta and ⎈ lavender.
Rules never designate a suit by its color, only by its symbol.


## Which suits have a value

| Values | Suits | Count |
| --- | --- | --- |
| 0–8, J, Q, A | ♠ ♥ ♣ ♦ ✿ ⎈ ★ | 7 |
| 9–15 | ♠ ♥ ♣ ♦ ✿ ★ | 6 |
| 16–18, S | ♠ ♥ ♣ ♦ ★ | 5 |
| 19–21, C, ×2, -‽ | ★ | 1 |
| K | 3 copies in ♠ ♥ ♣ ♦ ✿ ★, 2 in ⎈ | 20 copies |
| ► | ⎈ | 1 |

A game with *n* suits of values 1 to *m* fits when *n* suits have every value up to *m*: up to 7 suits of 0–8, 6 suits of 0–15, 5 suits of 0–18.


## Names of the cards

| Card | In English rules | In French rules |
| --- | --- | --- |
| A numbered card | ★4, ♥12 (suit, then value) | 4★, 12♥ (value, then suit) |
| A figure | ♠K, ★S | K♠, S★ |
| Several values | 1-9 in ♠, ♥ & ♣ | 1-9 à ♠, ♥ & ♣ |
| The word "suit" | suit | couleur (never « enseigne ») |
| A suit | the ♠ cards | les cartes ♠ |
| Butterfly | Butterfly, Butterflies | Papillon, Papillons |
| Wolf | Wolf, Wolves | Loup, Loups |
| Phoenix | Phoenix, Phoenixes | Phénix |
| Snake | Snake, Snakes | Serpent, Serpents |
| Dragon | Dragon, Dragons | Dragon, Dragons |
| Monkey | Monkey, Monkeys | Singe, Singes |
| Ø | the Ø card | la carte Ø |
| ×2, -‽, ► | the ×2 card, the -‽ card, the ► card | la carte ×2, la carte -‽, la carte ► |

- Specials take a capital letter: they are names, like the suits' symbols.
- List suits in the order ♠, ♥, ♣, ♦, ✿, ⎈, ★, with `&` before the last one.
- Write ranges with a hyphen, lowest first: `1-9`, `0-15`.


## Mapping heuristics

The mapping turns every card and component of the original game into SSD cards.
These heuristics come from the games already ported; they are defaults, not laws, and the user validates the result.

**Numbered suits.**
An original suit becomes one SSD suit, and its values keep their numbers whenever the SSD suit has them, so that the printed value is the value in play.
Take ♠, ♥, ♣ and ♦ first, then ✿, ★ and ⎈, skipping any suit that lacks a needed value (see the table above).
Coloretto's 7 colors of 0-8 use all seven suits; Schotten Totten's 6 clans of 1-9 use ♠, ♥, ♣, ♦, ✿ & ★.

**A distinct suit or card.**
★ stands out and has the most values: it suits a trump suit, a suit of point cards, or a lone card with a role.
Tichu's 1, which starts the game, is 1★; the point cards of Bids are ★.

**Cards with powers.**
Map them to specials, to figures (J, Q, K, S, C, A) or to unique cards whose glyph matches the role.
Keep the theme when the original has one: Tichu's Dragon and Phoenix stay Dragon and Phoenix, its dog becomes the Wolf.
Identical copies map to identical cards: Coloretto's 3 jokers are 3 Wolves, its 10 "+2" cards 6 Butterflies & 4 Phoenixes.
Whot!'s suit-changing cards are Butterflies.

**Components that are not cards.**
Replace them with SSD cards the game does not use otherwise: Schotten Totten's 9 stones are the 3 K of ♠, ♥ & ♣, Coloretto's "last round" card is Ø, and a first-player marker can be a Monkey.
Scores go on paper with a pencil.
A board or a score sheet becomes the printable `board.designcraft` of the game's directory: the user makes it, so describe what it needs.

**Readability at the table.**
Two groups of cards with different roles should differ at a glance: by suit, or by kind (numbers, figures, specials).
Avoid giving a card a value other than the one printed on it.
When printed information is irrelevant to the game, the rule warns about it with a `CAUTION` (Coloretto: "only the color of the cards matters").

**Copies.**
Never ask for more copies than the deck holds: 6 Butterflies, 6 Wolves, 4 Phoenixes, 4 Snakes, 2 Dragons, 2 Monkeys, 1 Ø, 3 K per suit (2 in ⎈), one of everything else.

**When the deck cannot cover the game.**
Say so and offer options (fewer players, a variant without some cards, a printable), and let the user decide.
Never drop a card or a rule silently.

To keep the ports consistent, look at how other games used a card before choosing it: `grep -l Phoenix games/*/game.yaml`.


## Presenting the mapping

Present the mapping in the user's language, and ask for its explicit validation.

```markdown
| Original | Copies | SSD cards | Why |
| --- | --- | --- | --- |
| Red cards 1-9 | 9 | ♠1-9 | first numbered suit |
| Blue cards 1-9 | 9 | ♥1-9 | |
| Dragon | 1 | Dragon | same creature |
| Jokers | 3 | 3 Wolves | identical copies, any color |
| Last round card | 1 | Ø | marker |
| Score pad | — | paper & pencil | |

Totals: 3-4 players: 22 cards; 5 players: 31 cards (adds ♣1-9).
Never cited in the rules: Red, Blue, Joker, Last round card.
```

- Cover every variant and every player count, with the total of SSD cards for each.
- List the original names that the rules will never use: they become the `--forbid` list of `scripts/check_rule.py`.
- Flag every choice you are unsure of, and every place where the deck falls short.
