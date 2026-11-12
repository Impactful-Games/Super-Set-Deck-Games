#!/usr/bin/env bash
# Encrypts the fonts of the design files, installed on this Mac, into .github/fonts.tar.gz.enc, and stores
# its new passphrase in the FONTS_PASSPHRASE secret of the repository: the CI installs them from there.
# Run it again whenever the design files use a new font, adding the font's files below.
# The fonts are encrypted rather than stored in a secret, which is limited to 48 KB.
set -euo pipefail

cd "$(dirname "$0")/.."

FONTS_PASSPHRASE="$(openssl rand -base64 48 | tr -d '\n')"
export FONTS_PASSPHRASE

# COPYFILE_DISABLE: no ._* files for the extended attributes of macOS.
(cd ~/Library/Fonts && COPYFILE_DISABLE=1 tar -czf - LCTPicon-*.ttf) \
    | openssl enc -aes-256-cbc -pbkdf2 -iter 600000 -salt -pass env:FONTS_PASSPHRASE -out .github/fonts.tar.gz.enc

printf %s "$FONTS_PASSPHRASE" | gh secret set FONTS_PASSPHRASE --repo Impactful-Games/Super-Set-Deck-Games
