#!/usr/bin/env python3
import psycopg2
import sys
import requests

key = sys.argv[1]#geocode api key

SELECT_ADDRESS = "select ca.id_address, ca.desc_address from carnival_address ca where coords is null"
UPDATE_ADDRESS = "update carnival_address set coords = ST_MakePoint(%s, %s) where id_address = %s"

con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
cursor = con.cursor()

cursor.execute(SELECT_ADDRESS, )
addresses = cursor.fetchall()
for address in addresses:
    print(address)
    params = {
            'address': f'{address[1]}, Belo Horizonte, MG',
            'key': key
            }
    response = requests.post("https://maps.googleapis.com/maps/api/geocode/json", params=params)
    json = response.json()['results'][0]
    geometry = json['geometry']['location']
    cursor.execute(UPDATE_ADDRESS, (geometry['lng'], geometry['lat'], address[0]))
    
    con.commit()
cursor.close()

