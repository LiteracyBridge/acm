
# Clean directory
rm -rf ACM
mkdir ACM

# ACM & TB-Loader
cp -av ../acm/dist/acm.jar ./ACM/
cp -av ../acm/dist/splash-acm.jpg ./ACM/
cp -av ../acm/dist/lib ./ACM/
cp -av ./bats/*.bat ./ACM/

# Acm splash screen
cp -av ./images/splash.jpg ./ACM/

# S3Sync
cp -av ../acm/dist/S3Sync.jar ./ACM/
cp -av ../acm/dist/ctrl-all.jar ./ACM/

# Combined version properties
 cat ../acm/dist/build.properties ../../S3Sync/dist/s3sync.properties >./ACM/build.properties

# Audio converters
cp -av ./converters ./ACM/

# Desktop icons
mkdir ./ACM/images
cp -av ./images/*.ico ./ACM/images/

# JRE
# cp -av ./jre ./ACM/