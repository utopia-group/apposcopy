#!/bin/bash

tmpdir=astroid-output

sig=$1
sample=$2
limit=$3
function=$4

declare -a family
declare -a score
declare -a seen

index=0
max=0
fmax='ADRD'

getmax()
{
    counter=0
    max=0
    imax=0

    while [ $counter -lt $index ]; do
	if [ ${seen[$counter]} -eq 0 ]; then
	    if [ ${score[$counter]} -gt $max ]; then 
		max=${score[$counter]}
		fmax=${family[$counter]}
		imax=$counter
	    fi
	fi
	(( counter++ ))
    done

    seen[$imax]=1
}

zero() 
{

    rm -f *.json

    rm -rf "$tmpdir"
    mkdir "$tmpdir"
    cp $sig "$tmpdir"
    cp $sample "$tmpdir"

    ant sigGen -Dtarget.loc=$tmpdir -Dencoding=basic -Dsize=2 -Dfreq=frequency.txt > /dev/null

    count=`ls -1 *.json 2>/dev/null | wc -l`
    if [ $count != 0 ] ; then 

	rm $tmpdir/*
	mv *.json $tmpdir/zero.json
	cp $sig $tmpdir

	ant score -Dsig=$sig -Dzero=$tmpdir/zero.json -Dlimit=$limit -Dfunction=$function -Dfreq=frequency.txt > "$tmpdir"/output.txt

	value=`grep SCORE:  "$tmpdir"/output.txt | cut -d ':' -f 2 | cut -d ' ' -f 2`
	score[$index]=$value
	family[$index]=$(basename $sig .json)

	(( index++ ))
    fi

    rm -rf $tmpdir
}

if [[ $# -eq 0 ]] ; then
    echo 'Usage: ./approximate.sh <signature> <sample> <cutoff> <function>'
    echo 'Cutoff: [0--10000]; Function: {0=lexico, 1=frequency}'
    echo 'WARNING: No .json files can exist in the current directory!'
    exit 0
fi 

if [[ $# -lt 4 ]] ; then
    echo 'Usage: ./approximate.sh <signature> <sample> <cutoff> <function>'
    echo 'Cutoff: [0--10000]; Function: {0=lexico, 1=frequency}'
    echo 'WARNING: No .json files can exist in the current directory!'
    exit 0
fi

count=`ls -1 *.json 2>/dev/null | wc -l`
if [ $count != 0 ]
then 
    echo 'No .json file should exist in the current directory!'
    exit 0
fi 

if [ -d $sig ]; then
    l=`ls -1 $sig/* 2>/dev/null | wc -l`

    counter=0
    while [ $counter -lt $l ]; do
	seen[$counter]=0
	(( counter++ ))
    done

    dir=$sig
    
    for f in $dir/*; do
	sig=$f
	zero
    done

    c=0
    while [ $c -lt $l ]; do
	getmax
	if [ $c -eq 0 ]; then
	    if [ $max -ge $limit ]; then
		echo 'Malware'
	    else
		echo 'Benign'
	    fi
	fi
	if [ $max -gt 0 ]; then 
	    echo "$max $fmax"
	fi
	(( c++ ))
    done
else
    zero

    counter=0
    while [ $counter -lt $index ]; do
	seen[$counter]=0
	(( counter++ ))
    done

    c=0
    while [ $c -lt 1 ]; do
	getmax
	if [ $c -eq 0 ]; then
	    if [ $max -ge $limit ]; then
		echo 'Malware'
	    else
		echo 'Benign'
	    fi
	fi
	if [ $max -gt 0 ]; then 
	    echo "$max $fmax"
	fi
	(( c++ ))
    done
fi

exit 0
