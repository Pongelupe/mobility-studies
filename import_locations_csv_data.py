#!/usr/bin/env python:
import csv23 
import psycopg2

with csv23.open_reader('data/PessoasFrequentamConenxaoEoutrosLocais.csv') as csv_file:
    con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
    cursor = con.cursor()

    query = """
    SELECT COUNT(*) FROM public.conexao
    WHERE mac IN %s;
    """

    line = 0
    macs = set()
    for p in csv_file:
        if p[0].startswith( 'Conexão' ) and line !=0:
            macs.add(p[1])
        line += 1
    print(tuple(macs))
    print(cursor.execute(query, (tuple(macs), )))

    con.commit()
    cursor.close()
