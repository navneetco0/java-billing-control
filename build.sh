#!/bin/bash
set -e

APP_NAME="MyApp"
MAIN_CLASS="App"
MAC_ID="com.myapp.billing"
VERSION="1.0"

echo "🧹 Cleaning..."
rm -rf bin input output

echo "📁 Setting up directories..."
mkdir -p bin input/lib

echo "⚙️  Compiling..."
javac -cp "lib/ojdbc11.jar" -d bin $(find src -name "*.java")

echo "🖼️  Copying assets..."
cp -r src/assets bin/

echo "📦 Creating JAR..."
jar cfm app.jar manifest.txt -C bin .

echo "🔍 Checking assets in JAR..."
jar tf app.jar | grep assets

echo "📂 Preparing input..."
cp app.jar input/
cp lib/ojdbc11.jar input/lib/

echo "🏗️  Packaging with jpackage..."
jpackage \
  --input input \
  --dest output \
  --name "$APP_NAME" \
  --main-jar app.jar \
  --main-class "$MAIN_CLASS" \
  --app-version "$VERSION" \
  --type dmg \
  --mac-package-identifier "$MAC_ID" \
  --java-options "-Dapple.laf.useScreenMenuBar=true" \
  --java-options "-Dapple.awt.application.name=$APP_NAME"

echo "✅ Verifying config..."
hdiutil attach output/${APP_NAME}-${VERSION}.dmg -quiet
cat /Volumes/${APP_NAME}/MyApp.app/Contents/app/${APP_NAME}.cfg
hdiutil detach /Volumes/${APP_NAME} -quiet

echo "🎉 Done! DMG is at output/${APP_NAME}-${VERSION}.dmg"