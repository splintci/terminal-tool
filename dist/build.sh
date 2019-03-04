#!/usr/bin/env bash

echo "[*] Building Distributable..."

echo ""

# Obfuscate output jar
java -jar C:/Bin/proguard.jar @obfuscate.pro

echo "[*] Done Obfuscating."

echo ""

echo "[*] Generating exe..."

echo "X64 exe"

echo ""

# Convert to exe X64
launch4jc wrapperSplint.xml
launch4jc wrapperSplintUpdate.xml

echo "[*] X86 exe"

echo ""

# Convert to exe X86
launch4jc wrapperSplintX86.xml
launch4jc wrapperSplintUpdateX86.xml

echo "[*] Done generating exe"

echo ""

echo "[*] Generating Installers..."

echo ""

echo "64bit Installer"

# Create 64bit installer
iscc "C:\Repositories\SplintGUI\dist\installerX64.iss"

echo "32bit Installer"

# Create 32bit installer
iscc "C:\Repositories\SplintGUI\dist\installerX86.iss"

echo ""

echo "Done generating distributable :-)"