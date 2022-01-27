#!/usr/bin/env bash

cp='acm.jar:lib/*'
if [ "${OSTYPE}" == "msys" ]; then
    cp='acm.jar;lib/*'
fi

java -cp ${cp} TB $@
