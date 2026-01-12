#!/bin/bash

# Build script for AdygGIS-KT with 16KB page size support
# Usage: ./build-release.sh

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   AdygGIS-KT Build Script (16KB Support)  ║${NC}"
echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo ""

# Set JAVA_HOME to Android Studio's JDK
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Check if JAVA_HOME exists
if [ ! -d "$JAVA_HOME" ]; then
    echo -e "${RED}❌ Android Studio JDK not found at: $JAVA_HOME${NC}"
    echo -e "${RED}Please install Android Studio or set JAVA_HOME manually${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Using Java from: $JAVA_HOME${NC}"
echo ""

# Clean build
echo -e "${BLUE}🧹 Cleaning previous builds...${NC}"
./gradlew clean --no-daemon

# Build AAB (recommended for Google Play)
echo ""
echo -e "${BLUE}📦 Building AAB (Android App Bundle)...${NC}"
./gradlew bundleFullRelease --no-daemon

# Build APK (for testing)
echo ""
echo -e "${BLUE}📱 Building APK...${NC}"
./gradlew assembleFullRelease --no-daemon

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║            BUILD SUCCESSFUL! ✅            ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Output files:${NC}"
echo -e "  AAB: ${GREEN}app/build/outputs/bundle/fullRelease/app-full-release.aab${NC}"
echo -e "  APK: ${GREEN}app/build/outputs/apk/full/release/${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "  1. Test on Android 15+ device or emulator"
echo "  2. Verify 16KB compatibility with bundletool"
echo "  3. Upload AAB to Google Play Console"
echo ""
