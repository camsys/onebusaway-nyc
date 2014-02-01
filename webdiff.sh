#!/bin/bash
expected=${1}
actual=${2}

wget -q -O - ${1} >/tmp/expected.txt
wget -q -O - ${2} >/tmp/actual.txt
if [ $# -gt 3 ] || [ $# -lt 2 ]
then
  echo "INVALID INPUT:  $0 "
  echo "1: $1"
  echo "2: $2"
  echo "3: $3"
  echo "4: $4"
  echo "5: $5"
  echo "6: $6"
fi
if [ $# -eq 3 ]
then
  echo "VERBOSE:"
  echo "1: ${1}"
  echo "2: ${2}"
  cat /tmp/expected.txt /tmp/actual.txt
fi
exec diff /tmp/expected.txt /tmp/actual.txt
