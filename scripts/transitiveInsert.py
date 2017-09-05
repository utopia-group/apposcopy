#For each app in the database, do the query;
#Then check the corresponding folder to get the result.

import sqlite3 as lite
from subprocess import PIPE, Popen
import sys
import os
import datetime

def queryApp(dbDir):
    print dbDir  
    
    #load sql files to query set.
    con = None

    try:
        con = lite.connect(dbDir)
        
        cur = con.cursor()    
        cur.execute('SELECT * from iccg')
        
        rows_iccg = cur.fetchall()

        for row_ig in rows_iccg:
            apkId = str(row_ig[0])
            apkName = row_ig[1] 
            #print apkName + ":" + apkId
            cur.execute('SELECT id from node where iccg_id=' + apkId)
            rows_node = cur.fetchall()
            for row_n in rows_node:
                nodeId = str(row_n[0])
                #print  "****" + nodeId
                for row_t in rows_node:
                    ##same node?
                    if row_t[0] == row_n[0]:
                        continue
                    ##exist?
                    cur.execute('SELECT id  FROM edge where iccg_id='+apkId+' and src_node_id='+nodeId+' and tgt_node_id=' + str(row_t[0]))
                    rows_exist = cur.fetchall()
                    if len(rows_exist) > 0:
                        #print "exist..........****"
                        continue
                    else:
                        ##insert.
                        insertStmt = "insert into edge values(?,"+nodeId+","+str(row_t[0])+","+apkId+")"
                        print "neet to insert....." + insertStmt

                        cur.executescript(insertStmt)

        
    except lite.Error, e:
        
        print "Error %s:" % e.args[0]
        sys.exit(1)
        
    finally:
        
        if con:
            con.close()


def main():
    if len(sys.argv) < 2:
        print "Invalid arguments, you must provide a database."
        return
      
    dbDir = sys.argv[1]
    
    starttime = datetime.datetime.now()

    queryApp(dbDir)

    endtime = datetime.datetime.now()
    print "Total execute time:"
    print (endtime - starttime)

if __name__ == "__main__":
        main()
