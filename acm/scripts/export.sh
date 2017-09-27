#!/usr/bin/env bash

# first argument needs to be the acm name
acmname=$(echo $1|tr /a-z/ /A-Z/)
shift
# if doesn't start with ACM-, make it
prefix=${acmname:0:4}
if [ "${prefix}" != "ACM-" ]; then
    acmname="ACM-${acmname}"
fi

csvname="$(echo ${acmname}|tr [:upper:] [:lower:]).csv"

java -cp acm.jar:lib/*:resources/ org.literacybridge.acm.tools.CSVDatabaseExporter $acmname $csvname -n $@

