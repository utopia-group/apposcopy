grep -r "Total time" * | grep -Eo  "[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9][0-9]" | awk -F: '{ print ($1 * 3600) + ($2 * 60) + $3 }' | awk -F : '{sum+=$1} END {print "AVG=",sum/NR}'
