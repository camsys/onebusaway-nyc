#!/usr/bin/python

import os
import sys

directory_to_code_mapping = {
'mlk' : ' H',
'presidents_day' : ' I',
'good_friday' : ' J',
'memorial_day' : ' K',
'july_fourth' : ' M',
'labor_day' : ' N',
'columbus_day' : ' O',
'thanksgiving' : ' R',
'day_after_thanksgiving' : ' S',
'christmas_eve' : ' T',
'christmas_day' : ' U',
'christmas_day_observed' : ' V',
'christmas_week' : ' W',
'new_years_eve' : ' X',
'new_years_day' : ' Y',
'new_years_day_observed' : ' Z',
'non_holiday' : None
}


basedir = os.getcwd()
if len(sys.argv) > 1:
    basedir = sys.argv[1]


found_non_holiday = False
for file in os.listdir(basedir):
    if file == 'non_holiday':
        found_non_holiday = True
if not found_non_holiday:
    print >>sys.stderr, "Expected to find a directory called non_holiday in " + basedir
    sys.exit(1)

def process(base, directory):
    replacement_code = directory_to_code_mapping.get(directory)
    if replacement_code is None:
        return #no replacement

    dir = os.path.join(base, directory)
    for file in os.listdir(dir):
        path = os.path.join(dir, file)
        f = open(path, "r+b")
        #the code is at position 14,15 (zero-indexed)
        start = f.read(16)
        start = start[:14] + replacement_code
        f.seek(0)
        f.write(start)
        f.close()

for file in os.listdir(basedir):
    if os.path.isdir(file):
        process(basedir, file)
