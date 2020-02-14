#!/usr/bin/env python:
import csv
import psycopg2

def get_value_or_default(arr, index, default = -1):
    val = ''
    try:
        val = arr[index]
    except:
        val = default
    return val

with open('sample_data/sample_bus_data.csv') as csv_file:
    reader = csv.reader(csv_file, delimiter=';')
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
    con.commit()
    cursor.close()
