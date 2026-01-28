#!/bin/bash

SOURCE_ICON="/Users/mac/Downloads/Contacts Cleaner/app_icon_512.png"
ASSET_DIR="/Users/mac/Downloads/CleanContactsAI/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"

mkdir -p "$ASSET_DIR"

# Standard sizes for iOS
# size, scale, name
# 20x20
sips -z 40 40 "$SOURCE_ICON" --out "$ASSET_DIR/icon-40.png"
sips -z 60 60 "$SOURCE_ICON" --out "$ASSET_DIR/icon-60.png"
# 29x29
sips -z 58 58 "$SOURCE_ICON" --out "$ASSET_DIR/icon-58.png"
sips -z 87 87 "$SOURCE_ICON" --out "$ASSET_DIR/icon-87.png"
# 40x40
sips -z 80 80 "$SOURCE_ICON" --out "$ASSET_DIR/icon-80.png"
sips -z 120 120 "$SOURCE_ICON" --out "$ASSET_DIR/icon-120.png"
# 60x60
sips -z 120 120 "$SOURCE_ICON" --out "$ASSET_DIR/icon-120.png"
sips -z 180 180 "$SOURCE_ICON" --out "$ASSET_DIR/icon-180.png"
# 76x76 (iPad)
sips -z 152 152 "$SOURCE_ICON" --out "$ASSET_DIR/icon-152.png"
# 83.5x83.5 (iPad Pro)
sips -z 167 167 "$SOURCE_ICON" --out "$ASSET_DIR/icon-167.png"
# 1024x1024 (App Store - Upscaling since source is 512, but user asked to use it)
sips -z 1024 1024 "$SOURCE_ICON" --out "$ASSET_DIR/icon-1024.png"

# Generate Contents.json
cat <<EOF > "$ASSET_DIR/Contents.json"
{
  "images" : [
    { "size" : "20x20", "idiom" : "iphone", "filename" : "icon-40.png", "scale" : "2x" },
    { "size" : "20x20", "idiom" : "iphone", "filename" : "icon-60.png", "scale" : "3x" },
    { "size" : "29x29", "idiom" : "iphone", "filename" : "icon-58.png", "scale" : "2x" },
    { "size" : "29x29", "idiom" : "iphone", "filename" : "icon-87.png", "scale" : "3x" },
    { "size" : "40x40", "idiom" : "iphone", "filename" : "icon-80.png", "scale" : "2x" },
    { "size" : "40x40", "idiom" : "iphone", "filename" : "icon-120.png", "scale" : "3x" },
    { "size" : "60x60", "idiom" : "iphone", "filename" : "icon-120.png", "scale" : "2x" },
    { "size" : "60x60", "idiom" : "iphone", "filename" : "icon-180.png", "scale" : "3x" },
    { "size" : "20x20", "idiom" : "ipad", "filename" : "icon-40.png", "scale" : "2x" },
    { "size" : "29x29", "idiom" : "ipad", "filename" : "icon-58.png", "scale" : "2x" },
    { "size" : "40x40", "idiom" : "ipad", "filename" : "icon-80.png", "scale" : "2x" },
    { "size" : "76x76", "idiom" : "ipad", "filename" : "icon-152.png", "scale" : "2x" },
    { "size" : "83.5x83.5", "idiom" : "ipad", "filename" : "icon-167.png", "scale" : "2x" },
    { "size" : "1024x1024", "idiom" : "ios-marketing", "filename" : "icon-1024.png", "scale" : "1x" }
  ],
  "info" : {
    "version" : 1,
    "author" : "xcode"
  }
}
EOF
