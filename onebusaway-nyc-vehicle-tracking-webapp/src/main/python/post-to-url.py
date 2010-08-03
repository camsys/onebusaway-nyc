#!/usr/bin/env python

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
