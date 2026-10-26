#!/usr/bin/env python3
"""Generate the Android emoji picker catalog from Unicode's emoji-test.txt.

Usage:
  curl -sSO https://unicode.org/Public/emoji/latest/emoji-test.txt
  python3 scripts/generate_emoji_catalog.py emoji-test.txt

The catalog keeps fully-qualified sequences and drops skin-tone variants and
components, so every entry renders as one selectable emoji. Emoji data is
copyright Unicode, Inc. and used under https://www.unicode.org/terms_of_use.html
"""
import re
import sys

GROUPS = [
    ("Smileys & Emotion", "SMILEYS", "Smileys"),
    ("People & Body", "PEOPLE", "People"),
    ("Animals & Nature", "ANIMALS", "Animals"),
    ("Food & Drink", "FOOD", "Food"),
    ("Travel & Places", "TRAVEL", "Travel"),
    ("Activities", "ACTIVITIES", "Activities"),
    ("Objects", "OBJECTS", "Objects"),
    ("Symbols", "SYMBOLS", "Symbols"),
    ("Flags", "FLAGS", "Flags"),
]

OUT = "app/src/main/java/com/sj010/ghid/ui/combos/editor/EmojiCatalog.kt"


def main(source: str) -> None:
    entries = {group_name: [] for group_name, _, _ in GROUPS}
    group = None
    for raw in open(source, encoding="utf-8"):
        line = raw.strip()
        if line.startswith("# group:"):
            group = line.split(":", 1)[1].strip()
            continue
        match = re.match(
            r"^([0-9A-F ]+);\s*fully-qualified\s*#\s*(\S+)\s+E\d+\.\d+\s+(.+)$", line
        )
        if not match or group not in entries:
            continue
        code_points = [int(cp, 16) for cp in match.group(1).split()]
        if any(0x1F3FB <= cp <= 0x1F3FF for cp in code_points):
            continue  # skin tone variants
        name = match.group(3).replace("\\", "\\\\").replace('"', '\\"')
        entries[group].append((match.group(2), name))

    lines = [
        "package com.sj010.ghid.ui.combos.editor",
        "",
        "/**",
        " * Emoji picker catalog, generated from Unicode's emoji-test.txt (v18.0).",
        " *",
        " * Only fully-qualified sequences are kept and skin-tone variants are dropped,",
        " * so every entry is one tappable emoji. Regenerate with",
        " * `scripts/generate_emoji_catalog.py` instead of editing this file by hand.",
        " * Emoji data is copyright Unicode, Inc. and used under the Unicode terms of use.",
        " */",
        "internal enum class EmojiGroup(val label: String) {",
    ]
    for _, enum_name, label in GROUPS:
        lines.append(f'    {enum_name}("{label}"),')
    lines += [
        "}",
        "",
        "internal data class EmojiEntry(",
        "    val emoji: String,",
        "    val name: String,",
        "    val group: EmojiGroup",
        ")",
    ]
    for group_name, enum_name, _ in GROUPS:
        variable = enum_name.lower()
        lines += ["", f"private val {variable}: List<EmojiEntry> = listOf("]
        for emoji, name in entries[group_name]:
            lines.append(f'    EmojiEntry("{emoji}", "{name}", EmojiGroup.{enum_name}),')
        lines.append(")")
    lines += ["", "internal val EmojiCatalog: List<EmojiEntry> = buildList {"]
    for _, enum_name, _ in GROUPS:
        lines.append(f"    addAll({enum_name.lower()})")
    lines.append("}")
    with open(OUT, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")
    total = sum(len(v) for v in entries.values())
    print(f"wrote {OUT}: {total} emoji")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "emoji-test.txt")
