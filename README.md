# apposcopy
Semantics-Based Detection of Android Malware through Static Analysis

Please refer to our paper for more details:
http://www.cs.utexas.edu/~yufeng/papers/fse14.pdf

To analyze a malware(Say, adrd.apk) with apposcopy, here are the steps you need to follow:


1. Build apposcopy.jar from source: 

   ant clean

   ant 

2. Run apposcopy on a specific malware:

    ./stamp analyze adrd.apk

3. The result will be written into the corresponding log.txt:

   _home_yufeng_research_apposcopy_adrd.apk/chord_output/log.txt

   Please focus on the output at the end of the log file:

    ENTER: malware-java at Mon Jul 13 10:50:44 PDT 2015

    -----------Malware family=: [ADRD]

    LEAVE: malware-java

4. To reproduce our result for Android Genome Malware benchmark, simply execute the following script:

   ./apposcopy.sh

5. To analyze samples of a specific malware family:

ant analyze -Dtarget=./adrd_output/

Here, adrd_output is the folder that contains the samples (In the form of json files) of the ADRD family.

------------------------------------------------------------------
Apposcopy II(Automatic Synthesis Malware Signatures)


Basically there are two phases:
1. Signature generation.

 ant sigGen -Dtarget.loc=$family_folder 

 where $family_folder contains all samples of a specific family. e.g.,
 
 ant sigGen -Dtarget.loc=./samples/GoldDream

  means to generate GoldDream signature from the samples in "./samples/GoldDream".

 By default, it will randomly pick up two samples from this folder and generate the signature, if any.

 If you want to generate signature for samples of the entire folder, just turn "RANDOM" to false in SignatureGen.java .  I will make it configurable later.

 The signature will be generated in your current folder named by its family name + timestamp. e.g., GoldDream_636.json

 2. Signature matching.

 ant sigMatch -Dsig=$sig -Dtest=$test 

 where $sig denotes the signature in json file and $test denotes the directory of the testing sample(s). 

 ant sigMatch -Dsig=GoldDream_636.json -Dtest=./samples/GoldDream

 could test all samples under ./samples/GoldDream using signature GoldDream_636.json.

 you can also test a specific sample like this:

 ant sigMatch -Dsig=GoldDream_636.json -Dtest=./samples/GoldDream/c25b15339b07530e5906539b0e116ddbc3f1589f.json


Enjoy:-)


Yu

