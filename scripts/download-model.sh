#!/usr/bin/env bash
# download-model.sh — Download LiteRT model from HuggingFace
# Usage: ./download-model.sh [output_dir]
#
# Downloads gemma-4-E4B-it-litertlm for on-device inference.
# Requires: wget or curl, ~2.5GB free space
#
set -euo pipefail

OUTPUT_DIR="${1:-./models}"
MODEL_NAME="gemma-4-E4B-it"
MODEL_FILE="${MODEL_NAME}.litertlm"
HF_REPO="litert-community/gemma-4-E4B-it-litertlm"
HF_URL="https://huggingface.co/${HF_REPO}/resolve/main/${MODEL_FILE}"

echo "=== MOMCLAW Model Download ==="
echo "Model: ${MODEL_NAME}"
echo "Source: ${HF_REPO}"
echo "Target: ${OUTPUT_DIR}/${MODEL_FILE}"
echo ""

mkdir -p "${OUTPUT_DIR}"

TARGET="${OUTPUT_DIR}/${MODEL_FILE}"

if [ -f "${TARGET}" ]; then
    echo "Model already exists at ${TARGET}"
    echo "To re-download, delete it first: rm ${TARGET}"
    exit 0
fi

echo "Downloading ~2.5GB model file..."
echo "URL: ${HF_URL}"
echo ""

if command -v wget &>/dev/null; then
    wget --continue --progress=bar:force:noscroll -O "${TARGET}.tmp" "${HF_URL}"
elif command -v curl &>/dev/null; then
    curl -L -C - --progress-bar -o "${TARGET}.tmp" "${HF_URL}"
else
    echo "ERROR: wget or curl required"
    exit 1
fi

# Verify download
if [ ! -f "${TARGET}.tmp" ]; then
    echo "ERROR: Download failed - file not found"
    exit 1
fi

FILESIZE=$(stat -f%z "${TARGET}.tmp" 2>/dev/null || stat -c%s "${TARGET}.tmp" 2>/dev/null || echo "0")
if [ "${FILESIZE}" -lt 1000000 ]; then
    echo "ERROR: Downloaded file too small (${FILESIZE} bytes) - likely an error page"
    rm -f "${TARGET}.tmp"
    exit 1
fi

mv "${TARGET}.tmp" "${TARGET}"
echo ""
echo "✅ Model downloaded successfully: ${TARGET}"
echo "   Size: $(du -h "${TARGET}" | cut -f1)"
echo ""
echo "To use on Android device:"
echo "  adb push ${TARGET} /sdcard/Android/data/com.loa.MOMCLAW/files/models/"
echo ""
echo "Or place in app assets for bundling."
