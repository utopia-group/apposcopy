import urllib
import urllib2
import json
import json
import requests
import sys
import ssl
import hashlib
import time
import signal

uid = 'yufeng@cs.utexas.edu'

def getreport(md5):
    #print 'uid:' + uid
    #print 'md5:' + md5
    param = {'md5':md5,'uid':uid}
    data = urllib.urlencode(param)
    url = "https://bdsec.soic.indiana.edu:8080/getreport/"
    req = urllib2.Request(url, data)
    #result = urllib2.urlopen(req)

    gcontext = ssl._create_unverified_context()
    result = urllib2.urlopen(req, context=gcontext)

    jdata = json.loads(result.read())


    flag = jdata['checked']
    if flag:
        print jdata['apkname']
        print jdata['apkhash']
        print jdata['methodnum']
        print jdata['result']
        print jdata['exception']
        print jdata['exceptionInfo']

    return flag
   
def scanfile(apk):
    url = "https://bdsec.soic.indiana.edu:8080/scanfile/"
    files = {'file':(apk, open(apk, "rb"))}
    cookie = {'user': uid}
    #result = requests.post(url, files = files, cookies = cookie)
    result = requests.post(url, files = files, cookies = cookie, verify=False)
    jdata = json.loads(result.text)
    
    print jdata['apkname']
    print jdata['apkhash']
    print jdata['methodnum']
    print jdata['result']
    print jdata['exception']
    print jdata['exceptionInfo']

#Get md5 of apk file.
def getmd5(fname):
    hash_md5 = hashlib.md5()
    with open(fname, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_md5.update(chunk)

    return hash_md5.hexdigest()

def signal_handler(signum, frame):
    raise Exception("Timed out!")

def main(argv):
    fname = argv[0]
    md5 = getmd5(fname)
    check = False 

    signal.signal(signal.SIGALRM, signal_handler)
    signal.alarm(60)   # 60 seconds
    try:
        check = getreport(md5)
    except Exception, msg:
        print "Query timed out after 60 secs!"


    # hasn't seen it before.
    if not check:
        #sleep for 45s
        print 'sleep for 45 secs.'
        time.sleep(45)
        signal.signal(signal.SIGALRM, signal_handler)
        signal.alarm(60)   # 60 seconds
        try:
            scanfile(fname)
            print 'Done.'
        except Exception, msg:
            print "Timed out after 60 secs!"


if __name__ == "__main__":
    main(sys.argv[1:])
