#For each app in the database, do the query;
#Then check the corresponding folder to get the result.

import sqlite3 as lite
from subprocess import PIPE, Popen
import sys
import os
import datetime

def queryApp(dbDir, appDir, sqlDir):
    print dbDir + '  ' + appDir + ' ' + sqlDir
    
    querySet = {
        'DroidDream' : ''
        ,
        'DroidDreamLight' : ''
        ,
        'GoldDream' : '' 
        ,
        'Geinimi' : ''
        ,
        'Pjapps' : ''
        ,
        'DroidKungFu1' : ''
        ,
        'DroidKungFu2' : ''
        ,
        'DroidKungFu3' : ''
        ,
        'DroidKungFu4' : ''
        ,
        'BaseBridge' : ''
        ,
        'ADRD' : '' 
        ,
        'AnserverBot' : ''
    }

    for path, subdir, files in os.walk(sqlDir):
        for sqlfile in files:
            #print sqlfile + " ||"  + path
            #print sqlfile[:-4]
            with open (os.path.join(path, sqlfile), "r") as myfile:
                #data = myfile.read()
                data = myfile.read().replace('\n', '')
                querySet[sqlfile[:-4]] = data

    #print querySet

    #return

    #load sql files to query set.
    con = None

    try:
        con = lite.connect(dbDir)
        
        cur = con.cursor()    
        cur.execute('SELECT * from iccg')
        
        rows = cur.fetchall()

        for row in rows:
        #for each apk, check whether it matches our query.
            apkId = str(row[0])
            apkName = row[1] 
            

            #for each app, do all the query.
            matchFamily = ''
            for key in querySet:
                #currentQuery = querySet[key]
                #replace the actual iccg_id.
                currentQuery = querySet[key].replace('?', apkId)
                #print '***************' + key
                #print currentQuery

                cur.execute(currentQuery)
                matchs = cur.fetchall()
                if len(matchs) > 0 :
                    matchFamily = matchFamily + " | " + key

            grep = "find " + appDir + " -iname " + apkName
            output, error = Popen(
                grep.split(" "), stdout=PIPE, stderr=PIPE).communicate()

            if matchFamily=='':
                matchFamily = 'unknown'

            print apkName + ' belongs to those families: ' + matchFamily + "  Original: " + output
            
        
    except lite.Error, e:
        
        print "Error %s:" % e.args[0]
        sys.exit(1)
        
    finally:
        
        if con:
            con.close()


def main():
    if len(sys.argv) < 4:
        print "Invalid arguments, you must provide database, app dir and sql dir."
        return
      
    dbDir = sys.argv[1]
    appdir = sys.argv[2]
    sqlDir = sys.argv[3]
    
    starttime = datetime.datetime.now()

    queryApp(dbDir, appdir, sqlDir)

    endtime = datetime.datetime.now()
    print "Total execute time:"
    print (endtime - starttime)

if __name__ == "__main__":
        main()

