#!/bin/bash
# Script to deploy everything that makes up the ACM.
set -eu
traditionalIFS="$IFS"
IFS="`printf '\n\t'`"

# Find dropbox.
if [ -z ${dropbox-} ]; then
    if [ -e ~/LiteracyBridge/ACM/software/dbfinder.jar ]; then
        dropbox=$(java -jar ~/LiteracyBridge/ACM/software/dbfinder.jar)
    elif [ -e ~/Dropbox\ \(Literacy\ Bridge\) ]; then
        dropbox=~/Dropbox\ \(Literacy\ Bridge\)
    elif [ -e ~/Dropbox ]; then
        dropbox=~/Dropbox
    else
        echo "Can't find Dropbox."
        exit 100
    fi
    export dropbox=$dropbox
fi
echo "Dropbox is in $dropbox"

# Convenience shortcuts.
installDir=$dropbox/LB-software/ACM-install
acmDir=$installDir/ACM/software

# Make sure we know where we are, in the directory tree.
if [ ! -e $installDir/r*.rev ]; then
    echo "Can't find the revision number file"
    exit 100
fi
if [ ! -e ../build/package/acm.jar ]; then
    echo "Can't find the built acm.jar"
    exit 100
fi
coreWithDeps=../../../dashboard-core/target/core-with-deps.jar 
if [ ! -e $coreWithDeps ]; then
    echo "Can't find the core-with-deps.jar"
    exit 100
fi
dbfinder=../../../DropboxFinder/target/dbfinder.jar
if [ ! -e $dbfinder ]; then
    echo "Can't find dbfinder.jar"
    exit 100
fi

oldRev=0
for r in $(ls $installDir/r*.rev); do
    r=${r%.rev}
    r=${r##*r}
    if [ $r -gt $oldRev ]; then
        oldRev=$r
    fi
done
newRev=$((oldRev+1))
echo "Revision $oldRev -> $newRev"
echo "mv $installDir/r${oldRev}.rev $installDir/r${newRev}.rev"

echo "Copying relative to $(pwd)"
echo "Installation directiory is $installDir"
echo "ACM directory is $acmDir"
echo "coreWithDeps is in $coreWithDeps"
echo "dbfinder is in $dbfinder"

read -p "Press [enter] to continue..."

# Copy the ACM itself.
set -x
mkdir -p $acmDir
cp -r ../build/package/* $acmDir/
cp acm.bat $acmDir/
cp acm.sh $acmDir/
mv $installDir/r${oldRev}.rev $installDir/r${newRev}.rev

# "other" software: core-with-deps.jar, dbfinder.jar
# This assumes the project structure places acm, dashboard-core, and DropboxFinder at the same level.
cp $coreWithDeps $acmDir/
cp $dbfinder $acmDir/
