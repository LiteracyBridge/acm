#!/usr/bin/env bash

# first argument needs to be the acm name
acmname=$(echo $1|tr /a-z/ /A-Z/)
shift
# if doesn't start with ACM-, make it
prefix=${acmname:0:4}
if [ "${prefix}" != "ACM-" ]; then
    acmname="ACM-${acmname}"
fi

splash=""
# TODO: if we ever have a TB-Loader splash screen, put that here.
# if [ -e splash-acm.jpg ]; then
#     splash="-splash:splash-acm.jpg"
# fi

java ${splash} -cp acm.jar:lib/*:resources/ TB loader $acmname $@
