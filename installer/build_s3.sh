#!/bin/bash

aws s3 sync --cli-read-timeout 0 --cli-connect-timeout 0 --delete ./ACM/ s3://amplio-software-update/desktop/ACM/

aws s3 cp --cli-read-timeout 0 --cli-connect-timeout 0  s3://amplio-software-update/desktop/ACM/build.properties s3://amplio-software-update/desktop/build.properties
aws s3 cp --cli-read-timeout 0 --cli-connect-timeout 0 s3://amplio-software-update/desktop/ACM/update.bat       s3://amplio-software-update/desktop/update.bat

# Don't copy the installer to amplio-software-update; people won't run the installer from there.
# aws s3 sync --exclude '*' --include AmplioSetup.exe ./Output/ s3://amplio-software-update/desktop/

# Put a copy in downloads.amplio.org/software, for convenience
aws s3 sync --cli-read-timeout 0 --cli-connect-timeout 0 --exclude '*' --include AmplioSetup.exe ./Output/ s3://downloads.amplio.org/software/
