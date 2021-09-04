from os import path 
d = path.abspath('.')
import os
import re

def utf8open(filename):
    return open(filename, encoding='utf-8', errors='ignore')

def utf8open_w(filename):
    return open(filename, 'w+', encoding='utf-8', errors='ignore')

def parse(file_lines):
    Lang = {}
    Math = {}
    Chart = {}
    Time = {}
    for line in file_lines:
        if line.startswith('Lang'):
            tmp = Lang
        elif line.startswith('Math'):
            tmp = Math
        elif line.startswith('Chart'):
            tmp = Chart
        elif line.startswith('Time'):
            tmp = Time
        elif len(line.split(':')) == 2:
            id = int(line.split(':')[0])
            time = float(line.split(':')[1])
            tmp[id] = time
    Langtotal = 0
    for i in range(1,66):
        if i in Lang:
            Langtotal += Lang[i]
        else:
            print(f'Lang{i}not in')
    Mathtotal = 0
    for i in range(1,107):
        if i in Math:
            Mathtotal += Math[i]
        else:
            print(f'Math{i}not in')
    Charttotal = 0
    for i in range(1,27):
        if i in Chart:
            Charttotal += Chart[i]
        else:
            print(f'Chart{i}not in')
    Timetotal = 0
    for i in range(1,28):
        if i in Time:
            Timetotal += Time[i]
        else:
            print(f'Time{i}not in')
    print (f'Lang total = {Langtotal}\nMath total = {Mathtotal}\nChart total = {Charttotal}\nTime toal = {Timetotal}')
    print (f'Lang average = {Langtotal/64}')
    print (f'Math average = {Mathtotal/106}')
    print (f'Chart average = {Charttotal/26}')
    print (f'Time average = {Timetotal/26}')
    return Langtotal+Mathtotal+Charttotal+Timetotal

if __name__ == '__main__':
    print('parse:')
    parsetime = utf8open('parsetime').readlines()
    parsetotaltime = parse(parsetime)
    print(parsetotaltime/(64+105+26+26))
    print('trace:')
    tracetime = utf8open('tracetime').readlines()
    tracetotaltime = parse(tracetime)
    print(tracetotaltime/(64+105+26+26))
