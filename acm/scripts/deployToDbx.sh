#!/bin/bash
# Script to deploy the built ACM
set -eu
traditionalIFS="$IFS"
IFS="`printf '\n\t'`"


# Find dropbox.
if [ -z ${dropbox-} ]; then
    dropbox=$(java -cp acm.jar:lib/* org.literacybridge.acm.utils.DropboxFinder)
    if [ $? -ne 0 ]; then
        if [ -e ~/Dropbox\ \(Literacy\ Bridge\) ]; then
            dropbox=~/Dropbox\ \(Literacy\ Bridge\)
        elif [ -e ~/Dropbox ]; then
            dropbox=~/Dropbox
        else
            echo "Can't find Dropbox."
            exit 100
        fi
    fi
    export dropbox=$dropbox
fi
echo "Dropbox is in $dropbox"

# Convenience shortcuts.
installDir=$dropbox/LB-software/ACM-install
acmDir=$installDir/ACM/software

# Add any missing libs
for f in $(ls lib); do
    if [ ! -e ${acmDir}/lib/${f} ]; then
        echo cp lib/${f} ${acmDir}/lib/${f} 
        cp lib/${f} ${acmDir}/lib/${f} 
    fi
done

# Update the acm.jar. 
# TODO: Make it exist in one place only
for f in $(find ${dropbox} -iname acm.jar); do
    echo cp acm.jar $f
    cp acm.jar $f
done

# Remove any obsolete libs
for f in $(ls ${acmDir}/lib); do
    if [ ! -e lib/${f} ]; then
        echo rm ${acmDir}/lib/${f}
        # rm ${acmDir}/lib/${f}
    fi
done

revision=$(ls ${installDir}/*.rev)
# strip .rev, leading path and '/r'
revision=${revision%.rev}
revision=${revision##*/r}
let newRevision=revision+1
echo mv ${installDir}/r${revision}.rev ${installDir}/r${newRevision}.rev
mv ${installDir}/r${revision}.rev ${installDir}/r${newRevision}.rev

