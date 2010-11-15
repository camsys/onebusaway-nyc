import sys
from sqlobject import *
from time import time, sleep
from httplib2 import Http

sqlhub.processConnection = connectionForURI('postgresql://onebusaway:onebusaway@localhost/onebusaway')

targetServer = "http://novalis.org/cgi/env.cgi"

class LocationRecord(SQLObject):
    class sqlmeta:
        table = "oba_nyc_raw_location"
        style = MixedCaseStyle()
    timeReceived = IntCol()
    rawData = StringCol()
   

lastCheck = time() * 1000

while 1:
    sleep(1)
    for row in LocationRecord.select(LocationRecord.q.timeReceived > lastCheck).orderBy("timeReceived"):
        print "sending row"
        sys.stdout.flush()
        h = Http()
        resp, content = h.request(targetServer, 
                                  "POST", body=row.rawData)
        if not resp['status'] == '200':
            print resp, content
            print rawdata
            print

        if lastCheck < row.timeReceived:
            lastCheck = row.timeReceived
