#!/bin/bash
# MomClAW Version Manager
# Manages version numbers across the project

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GRADLE_BUILD_FILE="$PROJECT_ROOT/android/app/build.gradle.kts"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print functions
print_info() { echo -e "${BLUE}ℹ ${NC}$1"; }
print_success() { echo -e "${GREEN}✓${NC} $1"; }
print_warning() { echo -e "${YELLOW}! $1"; }
print_error() { echo -e "${RED}✗${NC} $1"; }

# Get current version from build.gradle.kts
get_current_version() {
    local version_name=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$GRADLE_BUILD_FILE")
    local version_code=$(grep -oP 'versionCode\s*=\s*\K\d+' "$GRADLE_BUILD_FILE")
    echo "$version_name ($version_code)"
}

get_version_name() {
    grep -oP 'versionName\s*=\s*"\K[^"]+' "$GRADLE_BUILD_FILE"
}

get_version_code() {
    grep -oP 'versionCode\s*=\s*\K\d+' "$GRADLE_BUILD_FILE"
}

# Parse semantic version
parse_version() {
    local version=$1
    local major=$(echo "$version" | cut -d. -f1)
    local minor=$(echo "$version" | cut -d. -f2)
    local patch=$(echo "$version" | cut -d. -f3 | cut -d- -f1)
    local prerelease=""
    
    if [[ "$version" == *"-"* ]]; then
        prerelease="-$(echo "$version" | cut -d- -f2)"
    fi
    
    echo "$major $minor $patch $prerelease"
}

# Increment version
increment_version() {
    local version=$1
    local part=$2
    
    read major minor patch prerelease <<< $(parse_version "$version")
    
    case $part in
        major)
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        minor)
            minor=$((minor + 1))
            patch=0
            ;;
        patch)
            patch=$((patch + 1))
            ;;
        *)
            print_error "Invalid part: $part (use: major, minor, patch)"
            return 1
            ;;
    esac
    
    echo "$major.$minor.$patch$prerelease"
}

# Update version in build.gradle.kts
update_version() {
    local new_version=$1
    local new_version_code=$2
    
    if [ -z "$new_version_code" ]; then
        # Auto-increment version code
        local current_code=$(get_version_code)
        new_version_code=$((current_code + 1))
    fi
    
    print_info "Updating version to $new_version ($new_version_code)..."
    
    # Update versionName
    sed -i.bak "s/versionName = \"[^\"]*\"/versionName = \"$new_version\"/" "$GRADLE_BUILD_FILE"
    
    # Update versionCode
    sed -i.bak "s/versionCode = [0-9]*/versionCode = $new_version_code/" "$GRADLE_BUILD_FILE"
    
    rm -f "${GRADLE_BUILD_FILE}.bak"
    
    print_success "Updated version to $new_version ($new_version_code)"
}

# Update other version references
update_version_references() {
    local new_version=$1
    
    # Update README.md badge
    local readme="$PROJECT_ROOT/README.md"
    if [ -f "$readme" ]; then
        print_info "Updating README.md..."
        sed -i.bak "s/github/v/release/serverul/MOMCLAW?[^\"]*/github\/v\/release\/serverul\/MOMCLAW?v=$new_version/" "$readme"
        rm -f "${readme}.bak"
    fi
    
    # Update CHANGELOG.md
    local changelog="$PROJECT_ROOT/CHANGELOG.md"
    if [ -f "$changelog" ]; then
        print_info "Adding version header to CHANGELOG.md..."
        # Check if version already exists
        if ! grep -q "## \[$new_version\]" "$changelog"; then
            # Add new version section after the header
            sed -i.bak "/## \[Unreleased\]/a \\\n## [$new_version] - $(date +%Y-%m-%d)" "$changelog"
            rm -f "${changelog}.bak"
        fi
    fi
    
    # Update DOCUMENTATION-INDEX.md
    local doc_index="$PROJECT_ROOT/DOCUMENTATION-INDEX.md"
    if [ -f "$doc_index" ]; then
        print_info "Updating DOCUMENTATION-INDEX.md..."
        sed -i.bak "s/^\*\*Version\*\*: .*/\*\*Version\*\*: $new_version/" "$doc_index"
        sed -i.bak "s/^\*\*Last Updated\*\*: .*/\*\*Last Updated\*\*: $(date +%Y-%m-%d)/" "$doc_index"
        rm -f "${doc_index}.bak"
    fi
    
    print_success "Updated all version references"
}

# Create git tag
create_git_tag() {
    local version=$1
    local tag_name="v$version"
    
    print_info "Creating git tag $tag_name..."
    
    if git rev-parse "$tag_name" >/dev/null 2>&1; then
        print_error "Tag $tag_name already exists"
        return 1
    fi
    
    git add -A
    git commit -m "chore: release version $version" || true
    git tag -a "$tag_name" -m "Release version $version"
    
    print_success "Created tag $tag_name"
    print_info "Push with: git push && git push --tags"
}

# Generate version info file
generate_version_info() {
    local version_name=$(get_version_name)
    local version_code=$(get_version_code)
    local build_number=${GITHUB_RUN_NUMBER:-$(date +%s)}
    local git_sha=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    local build_date=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    
    cat > "$PROJECT_ROOT/version.json" <<EOF
{
  "versionName": "$version_name",
  "versionCode": $version_code,
  "buildNumber": $build_number,
  "gitSha": "$git_sha",
  "buildDate": "$build_date"
}
EOF
    
    print_success "Generated version.json"
}

# Show version info
show_version() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    echo -e "${BLUE}         MomClAW Version Info${NC}"
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    echo ""
    print_info "Version Name: $(get_version_name)"
    print_info "Version Code: $(get_version_code)"
    echo ""
    echo -e "${BLUE}───────────────────────────────────────${NC}"
    echo ""
}

# Help
show_help() {
    cat << EOF
MomClAW Version Manager

Usage: $0 <command> [options]

Commands:
    current                 Show current version
    show                    Alias for 'current'
    
    set <version> [code]    Set version to specific value
                            Optionally set version code
    
    bump <part>             Increment version part
                            Part: major | minor | patch
    
    release <version>       Prepare release version
                            Updates all references and creates git tag
    
    snapshot                Create snapshot version (-SNAPSHOT suffix)
    
    info                    Generate version.json file
    
    help                    Show this help message

Examples:
    $0 current
    $0 set 1.2.0 10200
    $0 bump minor
    $0 release 1.0.0
    $0 snapshot

Version Format:
    Semantic versioning: MAJOR.MINOR.PATCH[-PRERELEASE]
    Examples: 1.0.0, 1.0.1, 1.1.0, 2.0.0, 1.0.0-beta.1
EOF
}

# Main
case "${1:-}" in
    current|show)
        show_version
        ;;
    
    set)
        if [ -z "${2:-}" ]; then
            print_error "Version required"
            echo "Usage: $0 set <version> [version-code]"
            exit 1
        fi
        update_version "$2" "${3:-}"
        update_version_references "$2"
        show_version
        ;;
    
    bump)
        if [ -z "${2:-}" ]; then
            print_error "Part required (major|minor|patch)"
            echo "Usage: $0 bump <part>"
            exit 1
        fi
        
        current=$(get_version_name)
        new_version=$(increment_version "$current" "$2")
        update_version "$new_version"
        update_version_references "$new_version"
        show_version
        ;;
    
    release)
        if [ -z "${2:-}" ]; then
            print_error "Version required"
            echo "Usage: $0 release <version>"
            exit 1
        fi
        
        version=$2
        
        print_info "Preparing release $version..."
        update_version "$version"
        update_version_references "$version"
        generate_version_info
        create_git_tag "$version"
        
        echo ""
        print_success "Release $version prepared!"
        print_info "Next steps:"
        echo "  1. Review changes: git log"
        echo "  2. Push to remote: git push && git push --tags"
        echo "  3. CI/CD will automatically build and deploy"
        ;;
    
    snapshot)
        current=$(get_version_name)
        snapshot_version="${current}-SNAPSHOT"
        
        print_info "Creating snapshot version..."
        update_version "$snapshot_version"
        show_version
        ;;
    
    info)
        generate_version_info
        cat "$PROJECT_ROOT/version.json"
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
