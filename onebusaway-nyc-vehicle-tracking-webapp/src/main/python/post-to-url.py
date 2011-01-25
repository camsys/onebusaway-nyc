#!/usr/bin/env python
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


import fileinput
import getopt
import re
import sys
import urllib
import urllib2

def usage():
    print "usage: url"

def main():

    if len(sys.argv) < 2:
      usage()
      sys.exit(3)

    url = sys.argv.pop(1)
    data = ''

    for line in fileinput.input():
        data += line

    request = urllib2.Request(url,data)
    request.add_header('Content-type','application/json')
    response = urllib2.urlopen(request)
    response_data = response.read()
    print response_data

if __name__ == '__main__':
    main()
