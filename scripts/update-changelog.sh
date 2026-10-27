#!/usr/bin/env bash
set -euo pipefail

CHANGELOG="CHANGELOG.md"

if [ ! -f "$CHANGELOG" ]; then
  echo "::error::CHANGELOG.md not found in repo root" >&2
  exit 1
fi

# If an "Unreleased" section already exists, do nothing.
if grep -q '^## \[Unreleased\]' "$CHANGELOG"; then
  echo "Unreleased section already present; nothing to do."
  exit 0
fi

# Insert an Unreleased section right after the first line (the title).
perl -0777 -i -pe 's/^(# Changelog\n)/$1## [Unreleased] - YYYY-MM-DD\n\n### Added\n\n### Changed\n\n### Fixed\n\n/' "$CHANGELOG"

echo "Added '## [Unreleased]' section to $CHANGELOG"
