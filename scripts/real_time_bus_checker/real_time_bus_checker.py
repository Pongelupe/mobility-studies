#!/usr/bin/env python:
import csv
import http.client
from io import StringIO
import psycopg2

def get_value_or_default(arr, index, default = -1):
    val = ''
    try:
        val = arr[index]
    except:
        val = default
    return val

connection = http.client.HTTPSConnection('temporeal.pbh.gov.br')
connection.request('GET', '/gtfsrt.php')
print("requesting")
response = connection.getresponse()
print(f"response was {response.status}")

csv_string = response.read().decode()
print(csv_string)

buffed_csv = StringIO(csv_string)
reader = csv.reader(buffed_csv, delimiter=';')
con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
cursor = con.cursor()
sql = """
    INSERT INTO public.bus
(dt_moviment, coord, veic_num_order, instant_speed, line_code, direction, way, current_distance)
VALUES(TO_TIMESTAMP(%s, 'YYYYMMDDHH24MISS'), ST_MakePoint(%s, %s), %s, %s, %s, %s, %s, %s);
"""

line = 0
for p in reader:
    if line != 0 and len(p) > 0:
        print(p)
        cursor.execute(sql, (p[1], p[2], p[3], p[4], p[5], p[6], get_value_or_default(p, 7), get_value_or_default(p, 8), get_value_or_default(p, 9), ))
    line += 1 

print(f"insert {line} new records!")
connection.close()
con.commit()
cursor.close()
