#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo 'Usage: ./generation.sh [directory with samples] [size of sample] [number of repetitions] [timeout] [output directory for signatures]'
    echo 'Example: ./generation.sh samples/popl17/drebin/SMSreg/ 2 10 60 experiments/output'
    echo 'WARNING: assumes there are no json files in the current directory'
    exit 0
fi

family=$1
samples=$2
number=$3
output=$5
timeout=$4

i=0
while [ $i -lt $number ] ; do

    echo $i
    ./experiments/runsolver-linux -w /dev/null -W $timeout ant sigGen -Dtarget.loc=$family -Dencoding=basic -Dsize=$samples > /dev/null
    i=$[$i+1]
    
done
    
mv *.json $output
    
    
