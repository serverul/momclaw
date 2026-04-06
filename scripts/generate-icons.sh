#!/bin/bash
# MomClAW Icon Generator
# Generates all required app icons from a base image

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RES_DIR="$PROJECT_ROOT/android/app/src/main/res"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${BLUE}ℹ ${NC}$1"; }
print_success() { echo -e "${GREEN}✓${NC} $1"; }
print_warning() { echo -e "${YELLOW}! $1"; }
print_error() { echo -e "${RED}✗${NC} $1"; }

# Icon configurations
# Format: "density:size"
MIPMAP_SIZES=(
    "mipmap-mdpi:48x48"
    "mipmap-hdpi:72x72"
    "mipmap-xhdpi:96x96"
    "mipmap-xxhdpi:144x144"
    "mipmap-xxxhdpi:192x192"
)

# Adaptive icon foreground sizes
FOREGROUND_SIZES=(
    "mipmap-mdpi:108x108"
    "mipmap-hdpi:162x162"
    "mipmap-xhdpi:216x216"
    "mipmap-xxhdpi:324x324"
    "mipmap-xxxhdpi:432x432"
)

# Play Store sizes
PLAY_STORE_SIZES=(
    "play-store:512x512"
)

# Notification icon sizes
NOTIFICATION_SIZES=(
    "drawable-mdpi:24x24"
    "drawable-hdpi:36x36"
    "drawable-xhdpi:48x48"
    "drawable-xxhdpi:72x72"
    "drawable-xxxhdpi:96x96"
)

# Check dependencies
check_dependencies() {
    print_info "Checking dependencies..."
    
    local missing=()
    
    # Check for ImageMagick
    if ! command -v convert &> /dev/null; then
        missing+=("ImageMagick (convert)")
    fi
    
    # Check for optipng (optional)
    if ! command -v optipng &> /dev/null; then
        print_warning "optipng not found - PNGs won't be optimized"
    fi
    
    if [ ${#missing[@]} -gt 0 ]; then
        print_error "Missing dependencies:"
        printf '  - %s\n' "${missing[@]}"
        echo ""
        echo "Install with:"
        echo "  Ubuntu/Debian: sudo apt-get install imagemagick optipng"
        echo "  macOS: brew install imagemagick optipng"
        echo "  Fedora: sudo dnf install ImageMagick optipng"
        exit 1
    fi
    
    print_success "All dependencies found"
}

# Generate icons from base image
generate_icons() {
    local base_image=$1
    local output_name=${2:-"ic_launcher"}
    
    if [ ! -f "$base_image" ]; then
        print_error "Base image not found: $base_image"
        exit 1
    fi
    
    print_info "Generating icons from: $base_image"
    
    # Generate mipmap icons
    for config in "${MIPMAP_SIZES[@]}"; do
        local density="${config%%:*}"
        local size="${config##*:}"
        local width="${size%x*}"
        local height="${size#*x}"
        
        mkdir -p "$RES_DIR/$density"
        
        local output="$RES_DIR/$density/$output_name.png"
        
        print_info "  Generating $density ($size)..."
        
        convert "$base_image" \
            -resize "${width}x${height}" \
            -gravity center \
            -background none \
            -extent "${width}x${height}" \
            "$output"
        
        # Optimize PNG
        if command -v optipng &> /dev/null; then
            optipng -quiet "$output" 2>/dev/null || true
        fi
    done
    
    print_success "Generated mipmap icons"
}

# Generate adaptive icon foreground
generate_foreground() {
    local base_image=$1
    
    if [ ! -f "$base_image" ]; then
        print_error "Base image not found: $base_image"
        exit 1
    fi
    
    print_info "Generating adaptive icon foreground..."
    
    for config in "${FOREGROUND_SIZES[@]}"; do
        local density="${config%%:*}"
        local size="${config##*:}"
        local width="${size%x*}"
        local height="${size#*x}"
        
        mkdir -p "$RES_DIR/$density"
        
        local output="$RES_DIR/$density/ic_launcher_foreground.png"
        
        print_info "  Generating $density ($size)..."
        
        # Add padding for adaptive icon (72% of size is safe area)
        local safe_size=$((width * 72 / 100))
        local padding=$((width * 14 / 100))
        
        convert "$base_image" \
            -resize "${safe_size}x${safe_size}" \
            -gravity center \
            -background none \
            -extent "${width}x${height}" \
            "$output"
        
        if command -v optipng &> /dev/null; then
            optipng -quiet "$output" 2>/dev/null || true
        fi
    done
    
    print_success "Generated adaptive icon foreground"
}

# Generate Play Store icon
generate_play_store() {
    local base_image=$1
    
    if [ ! -f "$base_image" ]; then
        print_error "Base image not found: $base_image"
        exit 1
    fi
    
    print_info "Generating Play Store icon..."
    
    mkdir -p "$PROJECT_ROOT/assets"
    local output="$PROJECT_ROOT/assets/icon.png"
    
    convert "$base_image" \
        -resize 512x512 \
        -gravity center \
        -background none \
        -extent 512x512 \
        "$output"
    
    if command -v optipng &> /dev/null; then
        optipng -quiet "$output" 2>/dev/null || true
    fi
    
    print_success "Generated Play Store icon: $output"
}

# Generate notification icon
generate_notification_icon() {
    local base_image=$1
    local output_name=${2:-"ic_notification"}
    
    if [ ! -f "$base_image" ]; then
        print_error "Base image not found: $base_image"
        exit 1
    fi
    
    print_info "Generating notification icons..."
    
    for config in "${NOTIFICATION_SIZES[@]}"; do
        local density="${config%%:*}"
        local size="${config##*:}"
        local width="${size%x*}"
        local height="${size#*x}"
        
        mkdir -p "$RES_DIR/$density"
        
        local output="$RES_DIR/$density/$output_name.png"
        
        print_info "  Generating $density ($size)..."
        
        # Notification icons should be white with transparency
        convert "$base_image" \
            -resize "${width}x${height}" \
            -gravity center \
            -background none \
            -extent "${width}x${height}" \
            -colorspace Gray \
            -level 50%,100% \
            "$output"
        
        if command -v optipng &> /dev/null; then
            optipng -quiet "$output" 2>/dev/null || true
        fi
    done
    
    print_success "Generated notification icons"
}

# Generate round icons
generate_round_icons() {
    local base_image=$1
    
    if [ ! -f "$base_image" ]; then
        print_error "Base image not found: $base_image"
        exit 1
    fi
    
    print_info "Generating round icons..."
    
    for config in "${MIPMAP_SIZES[@]}"; do
        local density="${config%%:*}"
        local size="${config##*:}"
        local width="${size%x*}"
        local height="${size#*x}"
        
        mkdir -p "$RES_DIR/$density"
        
        local output="$RES_DIR/$density/ic_launcher_round.png"
        
        print_info "  Generating $density ($size)..."
        
        convert "$base_image" \
            -resize "${width}x${height}" \
            -gravity center \
            -background none \
            -extent "${width}x${height}" \
            \( +clone -fill white -colorize 100 \) \
            -alpha off -compose CopyOpacity -composite \
            "$output"
        
        if command -v optipng &> /dev/null; then
            optipng -quiet "$output" 2>/dev/null || true
        fi
    done
    
    print_success "Generated round icons"
}

# Generate all icons
generate_all() {
    local base_image=$1
    
    if [ -z "$base_image" ]; then
        print_error "Base image path required"
        echo "Usage: $0 generate-all <base-image.png>"
        exit 1
    fi
    
    generate_icons "$base_image"
    generate_round_icons "$base_image"
    generate_foreground "$base_image"
    generate_notification_icon "$base_image"
    generate_play_store "$base_image"
    
    echo ""
    print_success "All icons generated successfully!"
    print_info "Icon locations:"
    echo "  - Mipmap icons: android/app/src/main/res/mipmap-*/"
    echo "  - Foreground: android/app/src/main/res/mipmap-*/ic_launcher_foreground.png"
    echo "  - Notification: android/app/src/main/res/drawable-*/ic_notification.png"
    echo "  - Play Store: assets/icon.png"
}

# Verify icons exist
verify_icons() {
    print_info "Verifying icons..."
    
    local missing=()
    
    # Check mipmap icons
    for config in "${MIPMAP_SIZES[@]}"; do
        local density="${config%%:*}"
        local icon="$RES_DIR/$density/ic_launcher.png"
        
        if [ ! -f "$icon" ]; then
            missing+=("$density/ic_launcher.png")
        fi
    done
    
    # Check adaptive icon
    if [ ! -f "$RES_DIR/drawable/ic_launcher_foreground.xml" ]; then
        missing+=("drawable/ic_launcher_foreground.xml")
    fi
    
    if [ ! -f "$RES_DIR/drawable/ic_launcher_background.xml" ]; then
        missing+=("drawable/ic_launcher_background.xml")
    fi
    
    # Check Play Store icon
    if [ ! -f "$PROJECT_ROOT/assets/icon.png" ]; then
        missing+=("assets/icon.png")
    fi
    
    if [ ${#missing[@]} -gt 0 ]; then
        print_error "Missing icons:"
        printf '  - %s\n' "${missing[@]}"
        echo ""
        echo "Generate with: $0 generate-all <base-image.png>"
        exit 1
    fi
    
    print_success "All required icons present"
}

# Show help
show_help() {
    cat << EOF
MomClAW Icon Generator

Generates all required Android app icons from a base image.

Usage: $0 <command> [options]

Commands:
    generate-all <image>       Generate all icon variants from base image
    generate-mipmap <image>    Generate mipmap icons only
    generate-round <image>     Generate round icons only
    generate-foreground <image> Generate adaptive icon foreground
    generate-notification <image> Generate notification icons
    generate-play-store <image> Generate Play Store icon
    verify                     Verify all required icons exist
    help                       Show this help message

Base Image Requirements:
    - Format: PNG (preferred) or any ImageMagick-supported format
    - Size: At least 512x512 pixels
    - Style: Square or adaptive-safe (center 72% visible)
    - Background: Transparent or solid color

Examples:
    $0 generate-all logo.png
    $0 generate-mipmap logo.png
    $0 verify

Output Locations:
    android/app/src/main/res/
    ├── mipmap-mdpi/ic_launcher.png (48x48)
    ├── mipmap-hdpi/ic_launcher.png (72x72)
    ├── mipmap-xhdpi/ic_launcher.png (96x96)
    ├── mipmap-xxhdpi/ic_launcher.png (144x144)
    ├── mipmap-xxxhdpi/ic_launcher.png (192x192)
    ├── mipmap-mdpi/ic_launcher_round.png (48x48)
    ├── mipmap-hdpi/ic_launcher_foreground.png (108x108)
    └── ...
    
    assets/icon.png (512x512)

Adaptive Icon:
    MomClAW uses adaptive icons (Android 8.0+)
    - Foreground: Your logo (72% safe area)
    - Background: Solid color or gradient
    
    Configured in:
    - res/mipmap-*/ic_launcher_foreground.png
    - res/drawable/ic_launcher_background.xml
    - res/mipmap-*/ic_launcher.xml

Dependencies:
    - ImageMagick (convert)
    - optipng (optional, for PNG optimization)

Install Dependencies:
    Ubuntu/Debian: sudo apt-get install imagemagick optipng
    macOS: brew install imagemagick optipng
    Fedora: sudo dnf install ImageMagick optipng

EOF
}

# Main
case "${1:-}" in
    generate-all)
        generate_all "${2:-}"
        ;;
    
    generate-mipmap)
        generate_icons "${2:-}"
        ;;
    
    generate-round)
        generate_round_icons "${2:-}"
        ;;
    
    generate-foreground)
        generate_foreground "${2:-}"
        ;;
    
    generate-notification)
        generate_notification_icon "${2:-}"
        ;;
    
    generate-play-store)
        generate_play_store "${2:-}"
        ;;
    
    verify)
        check_dependencies
        verify_icons
        ;;
    
    help|--help|-h)
        show_help
        ;;
    
    *)
        print_error "Unknown command: ${1:-}"
        echo ""
        show_help
        exit 1
        ;;
esac
