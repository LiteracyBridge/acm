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

mustBeInDir=ACM-template
targets=$(ls *.bat)

targets=$(find . -iname '*.bat' -or -iname 'control*intro*.txt')
echo "Updating files in ACM-* directories."
echo "-------------------------------------"
echo "Files to be updated/added:"
for f in ${targets}; do echo "    ${f#./}"; done
echo "-------------------------------------"

# Compute some handy paths
runningInDir=${scriptpath##*/}
dropbox=${scriptpath%/*}

# Require that this run from the ${mustBeInDir} directory. Prevents local changes
# getting deployed to other ACMs.
if [ ${runningInDir} != ${mustBeInDir} ]; then
    echo "Must run from the ${mustBeInDir} directory."
    exit 101
fi

# Examine all the potential ACM directories. Mustn't be this directory, nor contain a 'noupdate.txt' file.
for acmdir in $(ls -d ${dropbox}/ACM-*); do
    # Only visit actual ACMs (no template).
    if [ ${acmdir##*/} != ${runningInDir} ] && [ ! -e ${acmdir}/noupdate.txt ]; then
        fb=""
        # Examine all the files we may want to update.
        for f in ${targets}; do 
            # If the target is in a subdirectory, and that subdir doesn't exist, skip it.
            subdir=${f%/*}
            if [ -d ${acmdir}/${subdir} ] || [ ${subdir} == "" ]; then

                # If it is different, or doesn't exist, copy it.
                if ! cmp -s ${f} ${acmdir}/${f} || [ ! -e ${acmdir}/${f} ] ; then
                    # Only show the ACM name one time.
                    if [ "${fb}" == "" ]; then
                        echo "Updating in ${acmdir##*/}:"
                        fb=${acmdir}
                    fi
                    echo "    ${f#./}"
                    # Finally, copy the file!
                    cp ${f} ${acmdir}/${f}
                fi
            
            fi
        done
    fi
done

