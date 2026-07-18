#!/usr/bin/env bash
# 严格模式：遇错即停，未定义变量报错
set -eu

# 尝试启用 pipefail（在 bash/zsh/Git Bash/WSL 等类 Unix shell 下可用）
PIPEFAIL_SUPPORTED=0
if set -o pipefail 2>/dev/null; then
  PIPEFAIL_SUPPORTED=1
fi

if [ "$PIPEFAIL_SUPPORTED" -eq 1 ]; then
  set -o pipefail
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"

# --- 用 tsx 直接执行 TypeScript ---
if command -v tsx >/dev/null 2>&1; then
  tsx "${REPO_ROOT}/.husky/scripts/commit-msg.ts"
  exit $?
fi
