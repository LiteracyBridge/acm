#!/usr/bin/env bash

if [ "$#" -lt 3 ]; then
    echo Usage: $0 OPERATION ACMNAME DEPLOYMENT ...
    exit 1
fi

# first argument is operation
op=$1&&shift
# second argument needs to be the acm name. Upper case it.
acmname=$(echo $1|tr /a-z/ /A-Z/)
shift
# if doesn't start with ACM-, make it
prefix=${acmname:0:4}
if [ "${prefix}" != "ACM-" ]; then
    acmname="ACM-${acmname}"
fi

java -cp acm.jar:lib/*:resources/ org.literacybridge.acm.tbbuilder/TBBuilder ${op} ${acmname} $@
