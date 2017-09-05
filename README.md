# Apposcopy & Astroid
Semantics-Based Detection of Android Malware through Static Analysis

Please refer to our papers for more details:
http://www.cs.utexas.edu/~yufeng/papers/fse14.pdf
http://www.cs.utexas.edu/~yufeng/papers/ndss17-astroid.pdf

--------------------------------------------------------------------------
Prerequisite:

Please make sure that you have installed JDK1.8, ANT and Android SDK and set the following env:
```
export ANT_HOME=/home/yu/libs/apache-ant-1.9.2
export ANDROID_HOME=/home/yu/libs/android-sdk-linux
export JAVA_HOME=/home/yu/libs/jdk1.8.0_91
export PATH=~/bin:$JAVA_HOME/bin:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANT_HOME/bin:$PATH
```

Please note that currently our tool only support Android SDK up to level 19. In order to reproduce the result in FSE'14, you might want to 
manually install old SDKs from level 3 to level 19 using the interface of Android SDK tool. 

All experiments were done under Ubuntu.

-----------------------------------------------------------------------
[x] How to compute ICCG of an apk?

To analyze a malware (Say, an app from ADRD family):

1. Run apposcopy on the malware:

    ./stamp analyze adrd/09b143b430e836c513279c0209b7229a4d29a18c.apk

2. The result will be written into the corresponding log.txt:

    stamp_output/_home_yu_research_ndss17-release_apposcopy_adrd_09b143b430e836c513279c0209b7229a4d29a18c.apk/chord_output/log.txt

    The ICCG (.json) will be stored in the following file:
        stamp_output/_home_yu_research_ndss17-release_apposcopy_adrd_09b143b430e836c513279c0209b7229a4d29a18c.apk/09b143b430e836c513279c0209b7229a4d29a18c.json

[x] How to compute a signature of multiple ICCGs?

1. Generate a new signature based on a random number of ICCGs of a given directory:

   ./generate.sh samples/fse14/ADRD/ 2

   This will select 2 samples randomly from that directory and will generate a signature that unifies them.
   If this directory only contains k samples, you can ask for a signature that unifies the k samples.

   Usage: ./generate.sh <samples> <size>

   Note that different runs may generate different signatures.

[x] How to test if an app (Given its ICCG) is benign or malware?

1. To use the zero-day malware detection you just need to use the script ./approximate.sh

```
   Usage: ./approximate.sh <signature> <sample> <cutoff>
   Cutoff: [0--10000]
   WARNING: No .json files can exist in the current directory!
```

   Example1:
   ./approximate.sh signatures/ADRD.json samples/fse14/ADRD/09b143b430e836c513279c0209b7229a4d29a18c.json 4927 0

```
   Output:
   Malware
   10000 ADRD
```

   Example2a using lexicographical order:
   ./approximate.sh signatures samples/fse14/ADRD/09b143b430e836c513279c0209b7229a4d29a18c.json 4927 0

```
   Output:
   Malware
   10000 ADRD
   5844 DroidDreamLight
   5200 DroidDream
   4504 GingerMaster
   4303 Geinimi
   4166 Bgserv
   3076 DroidKungFu
   1807 GoldDream
   1635 AnserverBot
   1538 Pjapps
   1014 BeanBot
   392 BaseBridge
   49 jSMSHider
```

   Example2b using lexicographical order with frequency analysis:
   ./approximate.sh signatures samples/fse14/ADRD/09b143b430e836c513279c0209b7229a4d29a18c.json 4927 1		

```
   Malware
   10000 ADRD
   6363 DroidDream
   4709 DroidDreamLight
   240 Pjapps
   223 Bgserv
   172 GingerMaster
   129 BeanBot
   102 GoldDream
   65 Geinimi
   28 DroidKungFu
   5 BaseBridge
   4 AnserverBot
```

Enjoy:-)


Yu and Ruben

