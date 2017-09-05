#Script for running batch apps on stamp
#1.Grep all possible apk files
#2.Run it on Stamp
#3.Parse SrcSinkFlow.xml of each app

import xml.etree.cElementTree as ET
import os
import sys
from subprocess import PIPE, Popen
import fileinput
import datetime
import shutil
import signal
import time
from signal import alarm, signal, SIGALRM, SIGKILL, SIGTERM
from random import randint

print "Automatically execute stamp app:"

stampdir = "" 
configFile = stampdir + "local.config"
appdir = ""
outputdir = "_result/"

baseXmlLoc = stampdir + "/stamp_output/"
xmlName = stampdir + "/results/SrcSinkFlow.xml" 
chord_log = stampdir + "/chord_output/log.txt"
iccgDir = stampdir + "/chord_output/iccg.dot"

def analyzeSrc2Sink(stampdir, appPath):
    usefulFlow = 0
    srcflag = True
    sinkflag = True
    src2Sink = ""
    appPath = appPath.replace("/", "_")

    baseXmlLoc = stampdir + "stamp_output/"

    print "PATH: " + baseXmlLoc + appPath + xmlName

    reftxt = baseXmlLoc + appPath + "/chord_output/out_taintedRefVar.txt"
    primtxt = baseXmlLoc + appPath + "/chord_output/out_taintedPrimVar.txt"

    if os.path.exists(reftxt):
	with open(reftxt) as f:
	    totalTaintedRef = sum(1 for line in f)
    if os.path.exists(primtxt):
        with open(primtxt) as f:
       	    totalTaintedPrim=sum(1 for line in f)

    if os.path.exists(primtxt):
        src2Sink += "\n Tainted Ref = " + str(totalTaintedRef) + " | Tainted Prim = " + str(totalTaintedPrim) + "\n"


    if os.path.isfile(baseXmlLoc + appPath + xmlName):
        tree = ET.ElementTree(file = baseXmlLoc + appPath + xmlName)
        treeResult = tree.iterfind('tuple/value/label')
        ix = 1
        for elem in treeResult:

            src2Sink += "\n"
            res = ''.join(elem.text) 
            if ix%2 == 0:
                src2Sink += " SINK:" + res 
                if "getExtras" in res or "INTENT" in res:
                    sinkflag = False
                if sinkflag and srcflag:
                    usefulFlow = usefulFlow + 1
                srcflag = True
                sinkflag = True
            else:
                src2Sink += "\n SRC:" + res + "---> "
                if "getExtras" in res or "INTENT" in res:
                    srcflag = False

            ix = ix + 1
    else:
        src2Sink +=  "BUILD FAIL************"
        
    src2Sink += "\n Number of meaningful flows(Exclude intent/bundle):" + str(usefulFlow)
        
    return src2Sink

def timeout(signum, frame):
    raise Exception

timeout = 500



def runAppWithStamp(stampdir, appdir):
    for path, subdir, files in os.walk(appdir):
        for file in files:
            if file.endswith(".apk"):
                appName = file
                #fullPath = os.path.join(path, file)

                statResult = ""
                permitResult = ""
                apkfile = os.path.join(path, file)

                #os.system(stampdir + "stamp analyze " + apkfile)
                cmd = stampdir + "stamp analyze " + apkfile

                process = Popen(cmd.split(" "), preexec_fn=os.setsid, stdout=PIPE, stderr=PIPE)


                start = datetime.datetime.now()
                exestart = time.time()
                flag = False

                while process.poll() is None:
                    time.sleep(0.1)
                    now = datetime.datetime.now()
                    if (now - start).seconds > timeout:
                        os.killpg(process.pid, SIGTERM)
                        print "timeout....." + apkfile
                        flag = True
                        break

                if flag: 
                    continue
                
                apkfile = apkfile.replace("/", "_")
                baseXmlLoc = stampdir + "stamp_output/"
                iccg =  baseXmlLoc + apkfile + iccgDir
                str = "dot -Tpng " + iccg + " > " + outputdir+appName+".png"

                if os.path.isfile(iccg):
                    exeend = time.time()
                    elapsed = exeend-exestart
                    timecost = secondsToStr(elapsed)

                    pf = open( outputdir + appName + ".txt", "w")
                    pf.write(timecost)
                    pf.close()

                    os.system(str)
                    print str + ':' + timecost


def main():
    if len(sys.argv) < 3:
        print "Invalid arguments, you must provide both stamp and app dirs."
        return
    
    #delete the previous.
    if os.path.exists(outputdir):
        shutil.rmtree(outputdir)
    
    #create a new one.    
    os.makedirs("_result")
      
    stampdir = sys.argv[1]
    appdir = sys.argv[2]
    
    starttime = datetime.datetime.now()

    runAppWithStamp(stampdir, appdir)

    endtime = datetime.datetime.now()
    print "Total execute time:"
    print (endtime - starttime)

if __name__ == "__main__":
        main()

