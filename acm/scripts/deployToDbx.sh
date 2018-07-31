#!/bin/bash
# Script to deploy the built ACM
set -eu
traditionalIFS="$IFS"
IFS="`printf '\n\t'`"

function setDefaults() {
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
    $verbose && echo "Dropbox is in $dropbox"
}

function configure() {
    # Convenience shortcuts.
    installDir=$dropbox/LB-software/ACM-install
    acmDir=$installDir/ACM/software
    report=/dev/stdout
}

function main() {
    readArguments "$@"
    setDefaults
    configure

    updateLibs
    updateJar
    updateBuildProps
    $updated && updateMarker
}

# Make the libs directory in Dropbox look like the one produced by the build.
# TODO: we should replicate this logic in the batch file that copies to the
# local computer. Unfortunately that script is an msdos batch file.
function updateLibs() {
    # Copy any missing or changed libs
    for f in $(ls lib); do
        if ! cmp -s lib/${f} ${acmDir}/lib/${f} ; then
            updated=true

            # Make the commands, so that they can be displayed and/or executed
            cpcmd=(cp -v "lib/${f}" "${acmDir}/lib/${f}")

            $verbose && echo "${cpcmd[@]}">>${report}
            $execute && "${cpcmd[@]}"
        fi
    done

    # Remove any obsolete libs
    for f in $(ls ${acmDir}/lib); do
        if [ ! -e lib/${f} ]; then
            updated=true

            rmcmd=(rm "${acmDir}/lib/${f}")

            #$verbose && echo "${rmcmd[@]}">>${report}
            $execute && "${rmcmd[@]}"
        fi
    done
}

# Update the acm.jar file wherever it exists in Dropbox. By now, really only 1 place.
function updateJar() {
    # Update the acm.jar. 
    for f in $(find ${dropbox} -iname acm.jar); do
        if ! cmp -s acm.jar "$f" ; then
            updated=true
            cpcmd=(cp -v "acm.jar" "$f")

            $verbose && echo "${cpcmd[@]}">>${report}
            $execute && "${cpcmd[@]}"
        fi
    done
}

# Update the build.properties file. 
function updateBuildProps() {
    if ! cmp -s "build.properties" "${acmDir}/build.properties" ; then
        updated=true
        cpcmd=(cp -v "build.properties" "${acmDir}/build.properties")

        $verbose && echo "${cpcmd[@]}">>${report}
        $execute && "${cpcmd[@]}"
    fi
}

# The marker file changes to let scripts know to update the .jar & libs
function updateMarker() {
    revision=$(ls ${installDir}/*.rev)
    # strip .rev, leading path and '/r'
    revision=${revision%.rev}
    revision=${revision##*/r}
    let newRevision=revision+1
    mvcmd=(mv "${installDir}/r${revision}.rev" "${installDir}/r${newRevision}.rev")

    $verbose && echo "${mvcmd[@]}">>${report}
    $execute && "${mvcmd[@]}"
    # without this echo, execution seems to just stop here.
    echo >/dev/null
}

declare -a remainingArgs=()
function readArguments() {
    local readopt='getopts $opts opt;rc=$?;[ $rc$opt == 0? ]&&exit 1;[ $rc == 0 ]||{ shift $[OPTIND-1];false; }'
    quiet=false
	dryrun=false
    updated=false
	
    # limit:, no-execute, quiet, summary, trace, verbose, input-dir:, output-dir:
    opts=nqu

    # Enumerating options
    while eval $readopt
    do
        #echo OPT:$opt ${OPTARG+OPTARG:$OPTARG}
        case "${opt}" in
	    n) dryrun=true;;
        q) quiet=true;;
        u) updated=true;;
        *) printf "OPT:$opt ${OPTARG+OPTARG:$OPTARG}" >&2;;
        esac
   done
   
    # Enumerating arguments, collect into an array accessible outside the function
	remainingArgs=()
    for arg
    do
	    remainingArgs+=("$arg")
    done
    # When the function returns, the following sets the unprocessed arguments back into $1 .. $9
    # set -- "${remainingArgs[@]}"

    # execute is the opposite of dryrun
    execute=true
    $dryrun && execute=false
    $dryrun && echo "Dry run, no files will be changed."

    verbose=true
    $quiet && verbose=false

    # bizarre -- need an echo here
    echo >/dev/null
}

main "$@"

