#!/usr/bin/env bash
set -euo pipefail

echo "============================================"
echo "  GIT SNAPSHOT - CREATE AND PUSH BRANCH"
echo "============================================"
echo

if ! command -v git >/dev/null 2>&1; then
    echo "[ERROR] Git not found in PATH."
    exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "[ERROR] Not a Git repository."
    exit 1
fi

# Check if working tree is clean
if git diff-index --quiet HEAD -- && git diff-files --quiet -- && [ -z "$(git ls-files --others --exclude-standard)" ]; then
    echo "[INFO] Working tree clean. Nothing to commit."
    exit 0
fi

TS=$(date +'%Y-%m-%d_%H-%M-%S')
BRANCH="snapshot/${TS}"

echo "[STEP 1/4] Staging all changes..."
git add -A

echo "[STEP 2/4] Committing..."
git commit -m "Snapshot ${TS}"

echo "[STEP 3/4] Creating branch: ${BRANCH}"
git checkout -b "${BRANCH}"

echo "[STEP 4/4] Pushing to origin..."
if ! git push -u origin "${BRANCH}"; then
    echo "[ERROR] Push failed. Check network and remote."
    exit 1
fi

echo
echo "============================================"
echo "  SUCCESS"
echo "============================================"
echo "Branch: ${BRANCH}"
echo "Remote: origin/${BRANCH}"
echo
