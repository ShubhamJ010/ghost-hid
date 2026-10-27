#!/usr/bin/env bash
set -euo pipefail

TAG="${1:?usage: scripts/update-changelog.sh <tag>}"
CHANGELOG="CHANGELOG.md"

if [ ! -f "$CHANGELOG" ]; then
  echo "::error::CHANGELOG.md not found at repo root"
  exit 1
fi

# Add a new release section, dropping any existing "Unreleased" block.
perl -0777 -i -pe '
  s/## \[Unreleased\].*?(?=\n## \[)/## [Unreleased]\n\n### Added\n\n### Changed\n\n### Fixed\n\n\n/s;
  # Insert the new release section right before the first ## [version] line,
  # or before the ## [Unreleased] line if that is the only header.
  s/^(## \[Unreleased\]\n)/## ['"${TAG}"'] - '"$(date +%Y-%m-%d)"'\n\n### Added\n\n### Changed\n\n### Fixed\n\n\n$1/s;
' "$CHANGELOG"

echo "CHANGELOG.md updated for $TAG"
