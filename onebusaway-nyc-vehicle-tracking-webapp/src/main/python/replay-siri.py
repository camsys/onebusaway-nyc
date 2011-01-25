#
# Copyright (c) 2011 Metropolitan Transportation Authority
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#

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
        h.force_exception_to_status_code = True
        resp, content = h.request(targetServer, 
                                  "POST", body=row.rawData)
        if not resp['status'] == '200':
            print resp, content
            print row.rawData
            print

        if lastCheck < row.timeReceived:
            lastCheck = row.timeReceived
