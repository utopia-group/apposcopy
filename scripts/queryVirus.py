import postfile

print 'query virus file.'

"""
host = "www.virustotal.com"
selector = "https://www.virustotal.com/vtapi/v2/file/scan"
fields = [("apikey", "bc3b30f092b84837c77108030bb10f06b0573e6a23518355004f3d2c87dd4176")]

print '1'
file_to_send = open("test.apk", "rb").read()
print '2'
files = [("file", "test.apk", file_to_send)]
print '3'
json = postfile.post_multipart(host, selector, fields, files)
print json
"""


import simplejson 
import urllib
import urllib2
import json
import os
import time

md5 = "9a36736efa92324b22be2ce966d472be"

"""
url = "https://www.virustotal.com/vtapi/v2/file/rescan"
parameters = {"resource": md5,
              "apikey": "bc3b30f092b84837c77108030bb10f06b0573e6a23518355004f3d2c87dd4176"}
data = urllib.urlencode(parameters)
req = urllib2.Request(url, data)
response = urllib2.urlopen(req)
json = response.read()
print json
"""


malwareDir = "/home/yufeng/research/exp/VirusShare/"

url = "https://www.virustotal.com/vtapi/v2/file/report"

cnt = 1

for malware in os.listdir(malwareDir):
    #get the md5 code of the app.
    time.sleep(5)
    end = malware.find('.apk')
    print malware +"---->"+ malware[10:end]
    md5 = malware[10:end]

    parameters = {"resource": md5,
                  "apikey": "bc3b30f092b84837c77108030bb10f06b0573e6a23518355004f3d2c87dd4176"}

    report = 'unknown'

    try: 

	data = urllib.urlencode(parameters)
	req = urllib2.Request(url, data)
	response = urllib2.urlopen(req)


        res = response.read()
        list = json.loads(res)
        dict = {}

        dict =  list['scans']
        report = dict['ESET-NOD32']

        print md5 + '  NOD32 report: ' + str(report)
        print md5 + '  Full report: ' + str(dict)

        print 'FINISH: ' + str(cnt) + ' ************************************************************\n'

        if cnt%500 == 0:
       	    time.sleep(20)


    #except (RuntimeError,KeyError,ValueError): 
    except: 
        print 'OOPS....We get an error..'
        pass
    finally:
        cnt = cnt + 1

    
