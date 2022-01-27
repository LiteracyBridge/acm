#!/usr/bin/env bash

cp='acm.jar:lib/*'
if [ "${OSTYPE}" == "msys" ]; then
    cp='acm.jar;lib/*'
fi

splash=""
if [ -e splash-acm.jpg ]; then
    splash="-splash:splash-acm.jpg"
fi

java ${splash} -cp ${cp} org.literacybridge.acm.gui.Application $@
