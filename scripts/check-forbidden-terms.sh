#!/usr/bin/env bash
# The assessment requires that the repo carry no reference to the assessing company.
# The pattern is spelled so that this file does not match itself.
set -u
cd "$(dirname "$0")/.."
pattern='money[-_ ]?for''ward'
hits=$(grep -rIniE "$pattern" . \
  --exclude-dir=node_modules --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git \
  --exclude-dir=.idea --exclude-dir=.kotlin --exclude-dir=.next --exclude-dir=uploads \
  --exclude="$(basename "$0")" 2>/dev/null)
if [ -n "$hits" ]; then
  echo "Forbidden reference found:"
  echo "$hits"
  exit 1
fi
echo "forbidden-terms: clean"
