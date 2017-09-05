# extract app related to my 18 families in virusshare.
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
import re

malwareSet = ['adrd', 'anserverbot','geinimi', 'beanbot', 'basebridge', 'bgserv', 'coinpirate', 'droidcoupon', 'droiddream', 'droiddreamlight', 'kungfu', 'gingermaster', 'pjapps', 'jsmshider', 'golddream']

appdir = '/home/yufeng/research/exp/VirusShare'

for family in malwareSet:
    #create folder
    if os.path.exists(family):
        shutil.rmtree(family)
 
    os.makedirs(family)

    for line in open("../virusReport-v4.txt"):
        if ('Full report' in line) and (re.search(family, line, re.IGNORECASE)):
            #copy file to folder
            apkcode = line[0:32]
	    for path, subdir, files in os.walk(appdir):
	        for file in files:
                    apkfile = os.path.join(path, file)
		    if apkcode in file:
			print file
			shutil.copy(apkfile, family)
     


