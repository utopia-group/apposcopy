import sqlite3

def connectDFATable(path):
    conn = sqlite3.connect(path + "app-reports.db")
    return conn


def createDFATable(path):
    conn = connectDFATable(path)
    cursor = conn.cursor()

    cursor.execute("""CREATE TABLE if not exists flows (flowKey INTEGER PRIMARY KEY AUTOINCREMENT, appName TEXT, sourceLabel TEXT, sourceClass TEXT, sinkLabel TEXT, sinkClass TEXT, flowClass TEXT, modifier TEXT, analysisCounter INTEGER, approvedStatus TEXT, timeStamp DATETIME DEFAULT CURRENT_TIMESTAMP)""")
    return conn


def insertDFATable(conn, dfa_data):
    cursor = conn.cursor()

    if not dfa_data:
        return False 
    appName = str(dfa_data[0][0])

    # Test for old analysis results and get analysis count
    cursor.execute("SELECT max(analysisCounter) FROM flows")
    dataTest = cursor.fetchone()

    analysisCounter = 0
    if not dataTest[0] is None:
        analysisCounter = dataTest[0] + 1
        
    insertData = [(x[0],x[1],x[2],x[3],x[4],x[5],x[6],analysisCounter) for x in dfa_data]
    cursor.executemany("INSERT INTO flows(appName,sourceLabel,sourceClass,sinkLabel,sinkClass,flowClass,modifier,analysisCounter) VALUES(?,?,?,?,?,?,?,?)", insertData)
    conn.commit()

    
def selectDFATable(conn):
    cursor = conn.cursor()
    sql = "SELECT * FROM flows"
    cursor.execute(sql)

    for v in cursor.fetchall():
        print v
