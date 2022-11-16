import json
import psycopg2
import sys

with open(sys.argv[1], 'r') as json_file:
    relacoes = json.load(json_file)['records']

print(f"inserindo {len(relacoes)} de linhas de onibus...") 

con = psycopg2.connect(host='localhost', port=15432, database='bh',
            user='bh', password='bh')
cursor = con.cursor()
sql = """
    INSERT INTO bh.linha_onibus
(id_linha, codigo_linha, descricao_linha)
VALUES(%s, %s, %s);
"""

for r in relacoes:
    cursor.execute(sql, (r[1], r[2], r[3], ))

con.commit()
cursor.close()
