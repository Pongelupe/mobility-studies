# -*- coding: utf-8 -*-
#!/usr/bin/env python:

import csv 
import psycopg2

with open('data/PessoasFrequentamConenxaoEoutrosLocais2.csv') as csv_file:
    reader = csv.reader(csv_file, delimiter=',')
    con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
    cursor = con.cursor()

    query = """
    SELECT COUNT(*) FROM conexao
    WHERE mac IN %s;
    """

    line = 0
    macs = set()
    for p in reader:
        if p[0].startswith( 'Conexão' ) and line !=0:
            macs.add(p[1])
        line += 1
    print(tuple(macs))
    cursor.execute(query, (tuple(macs), ))
    print(f"{cursor.fetchone()[0]} conections at Conexao and other places")

    con.commit()
    cursor.close()
