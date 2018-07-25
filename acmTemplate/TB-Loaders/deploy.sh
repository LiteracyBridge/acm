#!/usr/bin/env bash
set -eu
traditionalIFS="$IFS"
IFS="`printf '\n\t'`"

# Must run from where it is located, just to make it simpler.
scriptpath=$( cd $(dirname $0) ; pwd -P )
currentpath=$(pwd -P)
if [ $(pwd -P) != ${scriptpath} ]; then
    echo "Must run where it is located, $(printf '%q' ${scriptpath})."
    exit 101
fi

requiredParent=ACM-template
targets=$(ls *.bat)
deltargets="*.vbs"

# Compute some handy paths
scriptdir=${scriptpath##*/}
tmp=${scriptpath%/*}
parentdir=${tmp##*/}
dropbox=${tmp%/*}

# Require that this run from the ${requiredParent} directory. Prevents local changes
# getting deployed to other ACMs.
if [ ${parentdir} != ${requiredParent} ]; then
    echo "Must run from the ${requiredParent} directory."
    exit 101
fi

# Examine all the potential ACM directories.
for acmdir in $(ls -d ${dropbox}/ACM-*); do
    # Only visit actual ACMs (no user feedback, no template).
    if [ -d ${acmdir}/TB-Loaders ] && [ ${acmdir##*/} != ${parentdir} ] ; then
        fb=""
        # Examine all the files we may want to update.
        for f in ${targets}; do 
            # If it is different, or doesn't exist, copy it.
            if ! cmp -s ${f} ${acmdir}/TB-Loaders/${f} || [ ! -e ${acmdir}/TB-Loaders/${f} ] ; then
                # Only show the ACM name one time.
                if [ "${fb}" == "" ]; then
                    echo "Updating ${acmdir##*/}"
                    fb=${acmdir}
                fi
                echo "    ${f}"
                # Finally, copy the file!
                cp ${f} ${acmdir}/TB-Loaders/${f}
            fi
        done
        # Look for the files we want to delete
        for f in $(cd ${acmdir}/TB-Loaders;ls ${deltargets} 2>/dev/null); do
            if [ -e ${acmdir}/TB-Loaders/$f ]; then
                echo "Removing ${acmdir}/TB-Loaders/$f" 
                rm ${acmdir}/TB-Loaders/$f
            fi
        done
    fi
done

