import datetime
import http.client
import json
import psycopg2

def get_value_or_default(arr, key, default = 0):
    val = ''
    try:
        val = arr[key]
    except:
        val = default
    return val

conn = http.client.HTTPSConnection("temporeal.pbh.gov.br")
payload = ''
conn.request("GET", "/?param=D", payload)
res = conn.getresponse()
data = res.read()
decoded = json.loads(data.decode("utf-8"))

print(f"{len(decoded)} novos registros em {datetime.datetime.today()}")
con = psycopg2.connect(host='localhost', port=15432, database='bh',
            user='bh', password='bh')
cursor = con.cursor()
sql = """
    INSERT INTO bh.onibus_tempo_real
(codigo_evento, data_hora, coord, numero_ordem_veiculo, velocidade_instantanea, id_linha, direcao, sentido, distancia_percorrida)
VALUES(%s, TO_TIMESTAMP(%s, 'YYYYMMDDHH24MISS'), ST_MakePoint(%s, %s), %s, %s, %s, %s, %s, %s);
"""

for p in decoded:
    if 'NL' in p:
        try:
            cursor.execute(sql, (p['EV'], p['HR'], p['LG'], p['LT'], p['NV'], p['VL'], p['NL'], get_value_or_default(p, 'DG'), get_value_or_default(p, 'SV'),get_value_or_default(p, 'DT'), ))
        except:
            print(f"registro sem linha correspondente {p}")
    else:
        print(f"registro sem linha de onibus {p}")

con.commit()
cursor.close()
