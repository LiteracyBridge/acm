#!/usr/bin/env bash

cp='acm.jar:lib/*'
if [ "${OSTYPE}" == "msys" ]; then
    cp='acm.jar;lib/*'
fi

splash=""
if [ -e splash-tbl.png ]; then
    splash="-splash:splash-tbl.png"
fi

java ${splash} -cp ${cp} TB loader $@
