#!/usr/bin/python

#demonstrate that onebusaway can read data delivered in SIRI format.

import csv
import httplib
from datetime import datetime, timedelta
from tz import Eastern

#the siri template
f = open("siri.xml")
siri_template = f.read()
f.close()

#the test data (for the b62)
f = open("../../../../onebusaway-nyc-vehicle-tracking/src/test/resources/org/onebusaway/nyc/vehicle_tracking/impl/inference/ivn-dsc.csv")
reader = csv.reader(f)

#how Siri wants dates formatte
siriDateFormat = "%Y-%m-%dT%H:%M:%S%Z"

firstRead = False
fields = ["vid","date","time","lat","lon","dt","dsc","new.dsc"]
for line in reader:
    if not firstRead:
        #the first row is a header
        firstRead = True
        continue
    row = dict((fields[i], line[i]) for i in range(len(line)))
    #munge date formats
    now = datetime.strptime(row['dt'], "%Y-%m-%d %H:%M:%S")
    now = now.replace(tzinfo = Eastern)
    row['dt'] = now.strftime(siriDateFormat)
    validUntil = now + timedelta(0,60,0) #one minute later
    row['validUntil'] = validUntil.strftime(siriDateFormat)
    row['date'] = now.strftime("%Y-%m-%d")
    
    request = siri_template % row

    #generate request
    conn = httplib.HTTPConnection('localhost:6180')

    conn.request("POST", "/onebusaway-nyc-vehicle-tracking-webapp/update-location", request, {})

    response = conn.getresponse()
    data = response.read()
    if data != "ok":
        print "error sending data %s" % row

