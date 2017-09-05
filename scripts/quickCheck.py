"""
Step 1: For each app, run featureList.sql to get F1 to F10;
Step 2: Calculate the type of component that has more than 3 flows;
Step 3: Output the source-sink in step2.
"""
import sqlite3 as lite
from subprocess import PIPE, Popen
import sys
import os
import datetime
import math

def checkFeature(dbDir, sqlDir, appDir):
    #open the feature sql.
    queryFeature = ''
    with open (sqlDir, "r") as myfile:
        queryFeature = myfile.read().replace('\n', '')

    myTable = {

        'DroidDream' : [True, True, True, True, True, True, True, True, True, True]
        ,
        'DroidDreamLight' : [True, True, True, True, True, True, True, True, True, True]

        ,
        'GoldDream' : [True, True, True, True, True, True, True, True, True, True]

        ,
        'Geinimi' : [True, True, True, True, True, True, True, True, True, True]

        ,
        'Pjapps' : [True, True, True, True, True, True, True, True, True, True]

        ,
        'DroidKungFu1' : [True, True, True, True, True, True, True, True, True, True]

        ,
        'DroidKungFu2' : [True, True, True, True, True, True, True, True, True, True]

        ,
        'DroidKungFu3' : [True, True, True, True, True, True, True, True, True, True]

        ,
        'DroidKungFu4' : [True, True, True, True, True, True, True, True, True, True]

        ,
        'BaseBridge' : [True, True, True, True, True, True, True, True, True, True]

        ,
        'ADRD' : [True, True, True, True, True, True, True, True, True, True]
        ,
        'BeanBot' : [True, True, True, True, True, True, True, True, True, True]
        ,
        'Bgserv' : [True, True, True, True, True, True, True, True, True, True]
        ,
        'AnserverBot' : [True, True, True, True, True, True, True, True, True, True]
        ,
        'CoinPirate' : [True, True, True, True, True, True, True, True, True, True]
        ,
        'DroidCoupon' : [True, True, True, True, True, True, True, True, True, True]
        ,
        'jSMSHider' : [True, True, True, True, True, True, True, True, True, True]
        ,
        'GingerMaster' : [True, True, True, True, True, True, True, True, True, True]
    }

    pair = {

        'DroidDream' : 0
        ,
        'DroidDreamLight' : 0

        ,
        'GoldDream' : 0

        ,
        'Geinimi' : 0

        ,
        'Pjapps' : 0

        ,
        'DroidKungFu1' : 0

        ,
        'DroidKungFu2' : 0

        ,
        'DroidKungFu3' : 0

        ,
        'DroidKungFu4' : 0

        ,
        'BaseBridge' : 0

        ,
        'ADRD' : 0
        ,
        'BeanBot' : 0
        ,
        'Bgserv' : 0
        ,
        'AnserverBot' : 0
        ,
        'CoinPirate' : 0
        ,
        'DroidCoupon' : 0
        ,
        'jSMSHider' : 0
        ,
        'GingerMaster' : 0
    }

    appMap = {}

    try:

        con = lite.connect(dbDir)
        
        cur = con.cursor()    
        cur.execute('SELECT * from iccg')
        
        rows = cur.fetchall()

        for row in rows:
        #step1
            apkId = str(row[0])
            apkName = row[1] 

            #replace the actual iccg_id.
            currentQuery = queryFeature.replace('?', apkId)
            #print '***************' + key
            #print currentQuery

            cur.execute(currentQuery)
            features = cur.fetchone()

            grep = "find " + appDir + " -iname " + apkName
            output, error = Popen(
                grep.split(" "), stdout=PIPE, stderr=PIPE).communicate()

            output =  output.replace('\n', '') 
            familyName = output.split('/')[3]

            if not pair.has_key(familyName):
                continue

            nodeId = str(features[11])
            queryFlow = "SELECT source, sink FROM flow where src_node_id="+nodeId+"""
               and flow.src_node_id=flow.sink_node_id and 
               ( flow.sink='!INTERNET' or flow.sink='!File' or flow.sink='!FILE' or 
                 flow.sink='!EXEC' or flow.sink='!WebView' or flow.sink='!ENC/DEC' or flow.sink='!SOCKET' ) and 
               (source='$getDeviceId' or source='$getLine1Number' or source='$getSubscriberId' or
                source='$getSimSerialNumber' or source='$SDK' or source='$MODEL' or source='$BRAND' or
                source='$File' or source='$ENC/DEC' or source='$InstalledPackages' or
                source='$content://sms' or source='$RELEASE' or source='$PRODUCT' or
                source='MANUFACTURER')"""

        
            mystr = []

            if nodeId <> 'None':
                cur.execute(queryFlow)
                flows = cur.fetchall()
                for f in flows:
                    #print f[0] + '->' + f[1] + ','
                    #sys.stdout.write(f[0]+'->'+f[1]+',')
                    mystr.append(f[0]+'->'+f[1])

            mystr.sort()

            list = []
            list.append(familyName)
            for i in range(1,11):
                list.append(str(features[i]))
            list.append(str(mystr))
            appMap[apkName] = list


    except lite.Error, e:
        
        print "Error %s:" % e.args[0]
        sys.exit(1)
        
    finally:

        equalist = []
        count = 0

        """
        for key in pair:
            count = count + nCr(pair[key],2)
        """

        
        for key in appMap:
            for key2 in appMap:
                if key == key2:
                    continue
                if checklist(appMap[key2], appMap[key]):
                    list1 = []
                    list1.append(key)
                    list1.append(key2)
                    if addlist(equalist, list1):
                        pair[appMap[key][0]] = pair[appMap[key][0]] + 1 
                #    count = count + 1

        print 'count = ' + str(len(equalist))
        print pair
        
        if con:
            con.close()


def nCr(n,r):
    f = math.factorial
    return f(n) / f(r) / f(n-r)

def checklist(list1, list2):
    rte = False
    if (list1[0] <> list2[0]):
        return False
    for i in range(1,12):
        if (list1[i] <> list2[i]):
            rte = True
            break

    return rte

def addlist(mylist, entity):
    added = True
    for i in mylist:
        if (i[0] in entity) and (i[1] in entity):
            added = False
            break

    if added:
        mylist.append(entity)

    return added

def main():
    if len(sys.argv) < 4:
        print "Invalid arguments, you must provide app, db and sql."
        return
      
    dbDir = sys.argv[1]
    sqlDir = sys.argv[2]
    appDir = sys.argv[3]
    
    starttime = datetime.datetime.now()

    checkFeature(dbDir, sqlDir,appDir)

    endtime = datetime.datetime.now()
    print "Total execute time:"
    print (endtime - starttime)

if __name__ == "__main__":
        main()

