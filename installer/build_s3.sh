
aws s3 sync --delete ./ACM/ s3://amplio-software-update/desktop/ACM/

aws s3 cp  s3://amplio-software-update/desktop/ACM/build.properties s3://amplio-software-update/desktop/build.properties
aws s3 cp  s3://amplio-software-update/desktop/ACM/update.bat       s3://amplio-software-update/desktop/update.bat

aws s3 sync --exclude '*' --include AmplioSetup.exe ./Output/ s3://amplio-software-update/desktop/

# Make a copy in downloads.amplio.org/software, for convenience
aws s3 cp s3://amplio-software-update/desktop/AmplioSetup.exe s3://downloads.amplio.org/software/AmplioSetup.exe
