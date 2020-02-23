#!/usr/bin/env python3
import psycopg2
import sys
import requests
import pymongo

key = sys.argv[1] # google maps api key
place_type = sys.argv[2] # The type to be searched. It references https://developers.google.com/places/web-service/supported_types
radius = sys.argv[3] # the radius em meters from the coord to be searched

BASE_URL_PLACES_SERVICE = 'https://maps.googleapis.com/maps/api/place/nearbysearch/json'

SELECT_PLACES_TYPE = "select id_place_type from palce_type where desc_type in %s"
EXISTS_PLACE = "select count(1) > 0 from place where id_place = %s"
INSERT_PLACE = "INSERT INTO public.place (id_place, coords) VALUES(%s, ST_MakePoint(%s, %s)) RETURNING id_palce"
SELECT_COORDS = "SELECT ST_X(coords), ST_Y(coords) FROM carnival_address limit 1"

con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
cursor = con.cursor()

mongo_client = pymongo.MongoClient('mongodb://localhost:27017')
places_collection = mongo_client['carnaval_db']['places']

def search_coords(params):
    response = requests.post(BASE_URL_PLACES_SERVICE, params = params)
    json = response.json()
    print(params)
    if 'next_page_token' not in json:
        return json['results']
    else:
        return json['results'] + search_coords({'pagetoken': json['next_page_token'], 'key': params['key']})

cursor.execute(SELECT_COORDS)
addresses_coords = cursor.fetchall()
for address_coord in addresses_coords:
    print(address_coord[0])
    params = {
            'location': f"{address_coord[1]},{address_coord[0]}",
            'radius': radius,
            'key': key
            }
    print(search_coords(params))

cursor.close()
