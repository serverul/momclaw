#!/usr/bin/env bash
# download-model-v2.sh — Download Gemma 4E4B LiteRT model from HuggingFace
# Usage: ./download-model-v2.sh [output_dir]
#
# Downloads gemma-4-E4B-it.litertlm for on-device inference.
# Requires: wget or curl, ~4GB free space
#
# Changelog:
# - v2: Updated to Gemma 4 E4B-it (was incorrectly using Gemma 3)
# - Correct repository: litert-community/gemma-4-E4B-it-litert-lm
# - Correct file size: 3.65 GB (was 2.5 GB)
#
set -euo pipefail

OUTPUT_DIR="${1:-./models}"
MODEL_NAME="gemma-4-E4B-it"
MODEL_FILE="${MODEL_NAME}.litertlm"
HF_REPO="litert-community/gemma-4-E4B-it-litert-lm"
HF_URL="https://huggingface.co/${HF_REPO}/resolve/main/${MODEL_FILE}"
EXPECTED_SIZE="3654467584"  # 3.65 GB
EXPECTED_MD5="1b1e1b73f684f74b3fbecbaa419ec93d"

echo "=== MOMCLAW Model Download (v2) ==="
echo "Model: ${MODEL_NAME}"
echo "Source: ${HF_REPO}"
echo "Target: ${OUTPUT_DIR}/${MODEL_FILE}"
echo "Expected Size: 3.65 GB"
echo ""

mkdir -p "${OUTPUT_DIR}"

TARGET="${OUTPUT_DIR}/${MODEL_FILE}"

# Check if model already exists and is valid
if [ -f "${TARGET}" ]; then
    echo "Model file exists at ${TARGET}"
    
    # Verify file size
    ACTUAL_SIZE=$(stat -c%s "${TARGET}" 2>/dev/null || stat -f%z "${TARGET}" 2>/dev/null || echo "0")
    
    if [ "${ACTUAL_SIZE}" = "${EXPECTED_SIZE}" ]; then
        echo "✅ File size verified: ${ACTUAL_SIZE} bytes"
        
        # Optional: Verify MD5 (takes time for 3.65 GB)
        read -p "Verify MD5 checksum? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "Computing MD5 (this may take a minute)..."
            ACTUAL_MD5=$(md5sum "${TARGET}" | cut -d' ' -f1)
            if [ "${ACTUAL_MD5}" = "${EXPECTED_MD5}" ]; then
                echo "✅ MD5 checksum verified: ${ACTUAL_MD5}"
                echo ""
                echo "Model is ready to use!"
                exit 0
            else
                echo "❌ MD5 mismatch!"
                echo "Expected: ${EXPECTED_MD5}"
                echo "Got:      ${ACTUAL_MD5}"
                echo "Re-downloading..."
                rm -f "${TARGET}"
            fi
        else
            echo ""
            echo "Model is ready to use!"
            echo "To verify MD5, run: md5sum ${TARGET}"
            exit 0
        fi
    else
        echo "⚠️  File size mismatch (expected: ${EXPECTED_SIZE}, got: ${ACTUAL_SIZE})"
        echo "Re-downloading..."
        rm -f "${TARGET}"
    fi
fi

echo "Downloading ~3.65 GB model file..."
echo "URL: ${HF_URL}"
echo ""

# Download with progress and resume support
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

DOWNLOADED_SIZE=$(stat -c%s "${TARGET}.tmp" 2>/dev/null || stat -f%z "${TARGET}.tmp" 2>/dev/null || echo "0")
if [ "${DOWNLOADED_SIZE}" -lt 1000000 ]; then
    echo "ERROR: Downloaded file too small (${DOWNLOADED_SIZE} bytes) - likely an error page"
    rm -f "${TARGET}.tmp"
    exit 1
fi

# Verify expected size
if [ "${DOWNLOADED_SIZE}" != "${EXPECTED_SIZE}" ]; then
    echo "⚠️  Warning: File size mismatch"
    echo "Expected: ${EXPECTED_SIZE} bytes"
    echo "Got:      ${DOWNLOADED_SIZE} bytes"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Download cancelled"
        rm -f "${TARGET}.tmp"
        exit 1
    fi
fi

mv "${TARGET}.tmp" "${TARGET}"

echo ""
echo "✅ Model downloaded successfully: ${TARGET}"
echo "   Size: $(du -h "${TARGET}" | cut -f1)"
echo ""

# Optional MD5 verification
read -p "Verify MD5 checksum? (recommended, y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Computing MD5 (this may take a minute)..."
    ACTUAL_MD5=$(md5sum "${TARGET}" | cut -d' ' -f1)
    if [ "${ACTUAL_MD5}" = "${EXPECTED_MD5}" ]; then
        echo "✅ MD5 checksum verified: ${ACTUAL_MD5}"
    else
        echo "❌ WARNING: MD5 mismatch!"
        echo "Expected: ${EXPECTED_MD5}"
        echo "Got:      ${ACTUAL_MD5}"
        echo "The file may be corrupted. Consider re-downloading."
    fi
fi

echo ""
echo "To use on Android device:"
echo "  adb push ${TARGET} /sdcard/Android/data/com.loa.MOMCLAW/files/models/"
echo ""
echo "Or place in app assets for bundling."
echo ""
echo "See models/MODEL_SETUP.md for detailed deployment instructions."
