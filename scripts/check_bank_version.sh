#!/usr/bin/env bash
# 檢查 data/questions.json 有變動時，version 是否比前一版大。
# 用法：scripts/check_bank_version.sh <base-commit>
# （CI 會傳入 push 前的 commit 或 PR 的 base；本機可用 HEAD~1 測試）
set -euo pipefail

BASE="${1:-HEAD~1}"
FILE="data/questions.json"

if ! git cat-file -e "$BASE" 2>/dev/null; then
  echo "找不到基準 commit $BASE（可能是第一次 push），略過版本檢查"
  exit 0
fi
if ! git cat-file -e "$BASE:$FILE" 2>/dev/null; then
  echo "基準版本沒有 $FILE，略過版本檢查"
  exit 0
fi
if git diff --quiet "$BASE" -- "$FILE"; then
  echo "$FILE 沒有變動"
  exit 0
fi

old=$(git show "$BASE:$FILE" | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')
new=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["version"])' "$FILE")

if [ "$new" -le "$old" ]; then
  echo "::error file=$FILE::題庫內容有變動，但 version 沒有遞增（$old → $new）。App 只會下載 version 較大的題庫。"
  exit 1
fi
echo "題庫版本 $old → $new ✓"
