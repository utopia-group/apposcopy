import sqlite3
import sys

"""
Step 1: Clean DB;
Step 2: Build index after running all analysis.
Another nice way should build a fresh new db each time.
But I will switch to datalog anyway.
"""

def cleanDB(dbName):
    print 'sqlite_file: ' + dbName
    conn = sqlite3.connect(dbName)
    c = conn.cursor()
    c.execute('delete from callerComp')
    c.execute('delete from edge')
    c.execute('delete from flow')
    c.execute('delete from iccg')
    c.execute('delete from intentFilter')
    c.execute('delete from node')
    c.execute('delete from permission')

    conn.commit()
    conn.close()

if __name__ == "__main__":
    dbName = sys.argv[1]
    cleanDB(dbName)

