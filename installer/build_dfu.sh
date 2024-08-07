#!/bin/bash
#/usr/bin/env bash
dbg=""
#dbg="echo "

# This assumes that software is installed into random subdirectories of "/c/Program\ Files" and "/c/Program\ Files\ \(x86\)"

inno="$(find /c/Program\ Files\ \(x86\)/Inno\ Setup\ 6/ -iname iscc.exe)"
${dbg} "${inno}" AmplioDfuSetup.iss 
if [ "$?" -ne "0" ]; then
    exit 1
fi

# All signtool.exe (ignore errors) -> x64 architecture -> order by version -> latest one. 
signtool="$(find /c/Program\ Files\ \(x86\)/ -iname signtool.exe 2>/dev/null|grep x64|sort|tail -n1)"
sacmonitor="$(find /c/Program\ Files/SafeNet/ -iname '*monitor*exe')"

# Query whether the user wants to sign the code, and remind them to insert the certificate dongle.
echo "Code signing needs the EV certificate dongle. To sign AmplioSetup.exe, plug in the EV certificate dongle, and reply 'y'."
read -r -p "Do you want to proceed with signing? [Y/n] " response
if [[ $response =~ ^([nN][oO]|[nN])$ ]]; then
    exit 0 
fi
# This is the interface between signtool.exe and the dongle.
"${sacmonitor}" &

# It may be possible to automate the prompt for password. See https://stackoverflow.com/questions/17927895/automate-extended-validation-ev-code-signing/54439759#54439759
# the resulting command might look something like this:
# pwd=$(acquire password)
# name=$(acquire name)
# "${signtool}" sign /tr http://timestamp.sectigo.com /td sha256 /fd sha256 /f CodeSigning.cer /csp "eToken Base Cryptographic Provider" /k "[{{${pwd}}}]=${name} /a AmplioSetup.exe

pushd OutputDfu
# ${signtool} sign ...
# For some reason the sign tool is difficult to run correctly from bash, so create a MSDOS bat file and run that.
# Fortunately windows recognizes '/' as the path separator in some places, and launching an application is one of
# those places. Replace first '/c' with 'c:' in the command line and we're ok to go. Enclose the command in quotes
# because "Program Files".
echo We will now attempt to sign the AmplioSetup.exe program. NOTE: If you are not prompted for a password,
echo the signing may appear to work, and may report that it has worked, but it WILL NOT HAVE WORKED! Yay Microsoft!
wintool=${signtool/#\/c/c:}
echo \""${wintool}"\" sign /tr http://timestamp.sectigo.com /td sha256 /fd sha256 /a AmplioDfuSetup.exe>signdfu.bat
${dbg}./signdfu.bat
popd

# Put a copy in downloads.amplio.org/software, for convenience
aws s3 sync --exclude '*' --include AmplioDfuSetup.exe ./OutputDfu/ s3://downloads.amplio.org/software/

