#!/usr/bin/env python:
import csv23 
import psycopg2

with csv23.open_reader('data/ConexãoAeroporto.csv') as csv_file:
    con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
    cursor = con.cursor()

    sql = """
    INSERT INTO public.conexao
    (mac, init_conn, end_conn)
    VALUES(%s, %s, %s);
"""

    line = 0
    for p in csv_file:
        print(p)
        if line != 0:
            cursor.execute(sql, (p[2], p[3], p[4], ))
        line += 1

    con.commit()
    cursor.close()
