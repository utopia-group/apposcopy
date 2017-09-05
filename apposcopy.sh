#echo Step1: Clean database.
#python ./scripts/cleanDB.py iccg_scheme.sqlite 

#echo Step2: Run analysis.
#python ./scripts/plotICCG.py /home/yufeng/research/shord/ /home/yufeng/research/exp/malware_genome/fse/ADRD

#echo Step3: Build index to speed up query.


for file in $(find /home/yufeng/research/exp/malware_genome/fse/ -iname "*.apk")
do
    echo 'Analyzing....' $file
    ./stamp analyze $file
done
