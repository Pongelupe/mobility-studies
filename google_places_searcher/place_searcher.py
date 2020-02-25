#!/usr/bin/env python3
import psycopg2
import sys
import requests
import pymongo

key = sys.argv[1] # google maps api key
place_type = sys.argv[2] # The type to be searched. It references https://developers.google.com/places/web-service/supported_types
radius = sys.argv[3] # the radius em meters from the coord to be searched

BASE_URL_PLACES_SERVICE = 'https://maps.googleapis.com/maps/api/place/nearbysearch/json'

SELECT_PLACES_TYPE = "select id_place_type from place_type where desc_type in %s"
EXISTS_PLACE = "select count(1) > 0 from place where id_place = %s"
INSERT_PLACE = 'INSERT INTO public.place (id_place, coords, "name") VALUES(%s, ST_MakePoint(%s, %s), %s)'
SELECT_COORDS = "SELECT ST_X(coords), ST_Y(coords), id_address FROM carnival_address where id_address not in (select id_address from placexcarnival_address)"
INSERT_PLACEXADDRESS = "INSERT INTO public.placexcarnival_address (id_place, id_address) VALUES(%s, %s)"
INSERT_TYPEXPLACE = "INSERT INTO public.typexplace (id_place, id_place_type) VALUES(%s, %s)"

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
    params = {
            'location': f"{address_coord[1]},{address_coord[0]}",
            'radius': radius,
            'key': key
            }
    print(f"places for address_id -> {address_coord[2]}")
    places = search_coords(params)
    for place in places:
        id_place = place['id']
        name = place['name']
        geometry = place['geometry']['location']
        cursor.execute(EXISTS_PLACE, (id_place, ))
        if not cursor.fetchone()[0]:
            print(f"{name} discovered!")
            cursor.execute(INSERT_PLACE, (id_place, geometry['lng'], geometry['lat'], name))
            cursor.execute(SELECT_PLACES_TYPE, (tuple(place['types']), ))
            types = cursor.fetchall()
            for place_type in types:
                cursor.execute(INSERT_TYPEXPLACE, (id_place, place_type))
            places_collection.insert_one(place)


        cursor.execute(INSERT_PLACEXADDRESS, (id_place, address_coord[2]))
        con.commit()
cursor.close()
