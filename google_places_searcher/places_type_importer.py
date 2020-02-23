#!/usr/bin/env python3
import psycopg2

con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
cursor = con.cursor()

places_type = """
accounting
airport
amusement_park
aquarium
art_gallery
atm
bakery
bank
bar
beauty_salon
bicycle_store
book_store
bowling_alley
bus_station
cafe
campground
car_dealer
car_rental
car_repair
car_wash
casino
cemetery
church
city_hall
clothing_store
convenience_store
courthouse
dentist
department_store
doctor
drugstore
electrician
electronics_store
embassy
fire_station
florist
funeral_home
furniture_store
gas_station
grocery_or_supermarket
gym
hair_care
hardware_store
hindu_temple
home_goods_store
hospital
insurance_agency
jewelry_store
laundry
lawyer
library
light_rail_station
liquor_store
local_government_office
locksmith
lodging
meal_delivery
meal_takeaway
mosque
movie_rental
movie_theater
moving_company
museum
night_club
painter
park
parking
pet_store
pharmacy
physiotherapist
plumber
police
post_office
primary_school
real_estate_agency
restaurant
roofing_contractor
rv_park
school
secondary_school
shoe_store
shopping_mall
spa
stadium
storage
store
subway_stationsupermarket
synagogue
taxi_stand
tourist_attraction
train_station
transit_station
travel_agency
university
veterinary_care
zoo
"""
INSERT_PLACES_TYPE = "INSERT INTO public.place_type (desc_type) VALUES(%s)"
places = places_type.split()
for place in places:
    cursor.execute(INSERT_PLACES_TYPE, (place, ))

con.commit()
cursor.close()
