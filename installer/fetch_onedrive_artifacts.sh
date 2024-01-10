#!/bin/bash

# ACM directory will be invalid after this, so make that really obvious
rm -rf ACM

cp -av ~/OneDrive\ -\ amplio.org/Artifacts/firmware.v2/TBookRev2b.hex     firmware.v2/
cp -av ~/OneDrive\ -\ amplio.org/Artifacts/firmware.v2/firmware_built.txt firmware.v2/

rm system.v2/control_def.*
cp -av ~/OneDrive\ -\ amplio.org/Artifacts/system.v2/control_def.csm      system.v2/
cp -av ~/OneDrive\ -\ amplio.org/Artifacts/system.v2/control_def.yaml     system.v2/
