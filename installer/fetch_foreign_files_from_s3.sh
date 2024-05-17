#!/bin/bash

DIRS=""

function dirf() {
  if [ "${DIRS}" == "" ] ; then
    DIRS=$(aws s3 ls s3://amplio-software-update/build_installer/|awk '/PRE/{print $2}')
  fi
  for d in ${DIRS} ; do
    echo ${d%/}
  done
}
# pre-populate the list of "directories"
echo "Fetching list of buckets to be downloaded."
dirf>/dev/null 

echo "Creating empty directories, because s3 doesn't support them."
for f in $(dirf) ; do
  mkdir -p ${f}
done

echo "Fetching new & updated files from s3."
aws s3   sync s3://amplio-software-update/build_installer/ .
