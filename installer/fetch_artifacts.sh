#!/bin/bash

# ACM directory will be invalid after this, so make that really obvious
rm -rf ACM

cp -av ../../tbv2/Artifacts/TBookRev2b.hex     firmware.v2/
cp -av ../../tbv2/Artifacts/firmware_built.txt firmware.v2/

cp -av ../../tbv2/TBFiles/SDCard/system/control_def.txt system.v2/
cp -av ../../tbv2/TBFiles/SDCard/system/csm_data.txt    system.v2/
