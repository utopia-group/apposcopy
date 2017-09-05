#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo 'Usage: ./accuracy.sh [directory with signatures] [directory with samples]'
    echo 'Example: ./accuracy.sh experiments/output/ samples/popl17/drebin/SMSreg/'
    exit 0
fi

input=$1
samples=$2

rm -rf /var/tmp/astroid
mkdir /var/tmp/astroid

for f in $input/* ; do 

    y=$(basename $f .json)
    echo $y.json
    ant sigMatch -Dsig=$f -Dtest=$2 > /var/tmp/astroid/$y.out
    grep "Detection rate:" /var/tmp/astroid/$y.out
    
done


rm -rf /var/tmp/astroid


    
