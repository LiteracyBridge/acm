#!/bin/bash

# Clean directory
rm -rf ACM  
mkdir ACM

# ACM & TB-Loader
cp -av ../acm/dist/acm.jar ./ACM/
cp -av ../acm/dist/splash-acm.jpg ./ACM/
cp -av ../acm/dist/splash-tbl.png ./ACM/
cp -av ../acm/dist/lib ./ACM/
cp -av ./bats/*.bat ./ACM/

# Default v2 firmware, CSM
cp -av ./firmware.v2 ./ACM/
cp -av ./system.v2 ./ACM/

# S3Sync
cp -av ../acm/dist/S3Sync.jar ./ACM/
cp -av ../acm/dist/ctrl-all.jar ./ACM/

# Combined version properties
 cat ../acm/dist/build.properties ../../S3Sync/dist/s3sync.properties >./ACM/build.properties
# Include TBv2 artifacts
 if [ -e system.v2/control_def.csm ]; then
   shasum system.v2/control_def.csm >>./ACM/build.properties
 fi
 cat firmware.v2/firmware_built.txt >>./ACM/build.properties
 echo "$(shasum bats/*|shasum) bats" >>./ACM/build.properties

# Audio converters
cp -av ./converters ./ACM/
# When converters change, notice the change
echo "$(shasum ./ACM/converters/*/* 2>/dev/null|shasum) converters" >>./ACM/build.properties

# STM32 utilities
cp -av ./cube ./ACM/cube
# Qt5Core.dll
# Qt5SerialPort.dll
# Qt5Xml.dll
# cube.exe

# Desktop icons and splash screen
mkdir ./ACM/images
cp -av ./images/* ./ACM/images/

# JRE
# cp -av ./jre ./ACM/

# Utilities
cp -av ./utils/* ./ACM/
