#!/usr/bin/env python3
import gpxpy
import gpxpy.gpx
import psycopg2
import sys

SQL_SELECT_ID_BLOCK = "select id_block from carnival_block"
SQL_SELECT_BLOCK_COORDS = "select ST_X(ca.coords), ST_Y(ca.coords) from carnival_address ca join carnival_block_route cbr on cbr.id_address = ca.id_address and cbr.id_block = %s"

con = psycopg2.connect(host='localhost', port=25432, database='mob',
        user='mob', password='mob')
cursor = con.cursor()

cursor.execute(SQL_SELECT_ID_BLOCK)
block_ids = cursor.fetchall()

for b in block_ids:
    block_id = b[0]
    f = open(f'gpx_block/bloco_{block_id}.gpx', 'w')
    
    gpx = gpxpy.gpx.GPX()
    gpx.creator = "Ride with gpxpy"

    cursor.execute(SQL_SELECT_BLOCK_COORDS, (block_id,))
    coords = cursor.fetchall()
    for c in coords:
        lat = c[1]
        lon = c[0]
        gpx.waypoints.append(gpxpy.gpx.GPXWaypoint(lat, lon))

    f.write(gpx.to_xml())
    f.close()

con.commit()
cursor.close()
