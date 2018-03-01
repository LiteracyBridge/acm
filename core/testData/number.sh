#!/usr/bin/env bash


target="$1"

num=3001

for f in $(find ${target} -type f); do
    echo ${num}>${f}
    num=$(expr ${num} + 1)
done

