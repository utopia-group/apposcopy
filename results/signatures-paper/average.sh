#!/bin/bash

rm -f foo ; for f in *ACC* ; do sec=`echo $f | cut -d '.' -f 5 | cut -d 'm' -f 2` ; ms=`echo $f | cut -d '.' -f 6 | cut -d 's' -f 1`; echo "$sec.$ms" >> foo ; done
awk '{sum+=$1}END{print sum/NR}' foo
rm -f foo

