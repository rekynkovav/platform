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

# Check if working tree is clean (ignoring submodule changes)
if git diff-index --quiet HEAD -- && git diff-files --quiet -- && [ -z "$(git ls-files --others --exclude-standard)" ]; then
    # Double check: maybe only submodules are dirty
    SUBMODULE_CHANGES=$(git status --porcelain | grep -c '^ m ' || true)
    TOTAL_CHANGES=$(git status --porcelain | wc -l)
    if [ "$TOTAL_CHANGES" -eq "$SUBMODULE_CHANGES" ] && [ "$TOTAL_CHANGES" -gt 0 ]; then
        echo "[INFO] Only submodule changes detected. Nothing to commit in main repo."
        exit 0
    elif [ "$TOTAL_CHANGES" -eq 0 ]; then
        echo "[INFO] Working tree clean. Nothing to commit."
        exit 0
    fi
fi

TS=$(date +'%Y-%m-%d_%H-%M-%S')
BRANCH="snapshot/${TS}"

echo "[STEP 1/4] Staging all changes..."
# Stage everything except submodule pointer changes unless explicitly intended
git add -A
# Unstage submodule pointer changes to avoid empty commits or errors
# This finds entries with mode 160000 (gitlinks) that are modified and unstages them
git diff --cached --name-only --diff-filter=M | while read -r file; do
    if [ "$(git ls-files --stage "$file" 2>/dev/null | awk '{print $1}')" = "160000" ]; then
        echo "[INFO] Skipping submodule change: $file"
        git reset HEAD "$file" >/dev/null 2>&1 || true
    fi
done

# Check if index is empty after filtering
if git diff --cached --quiet; then
    echo "[INFO] No changes to commit (only submodule changes or clean)."
    exit 0
fi

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
