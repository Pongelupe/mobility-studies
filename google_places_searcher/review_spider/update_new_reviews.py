#!/usr/bin/env python3
import psycopg2
import sys

update = len(sys.argv) > 1

SQL_SELECT_REVIEWS = "select id_review, r2.url_user from review r2 where r2.user_id is null"
SQL_EXISTS_LOCATION = "select count(1) > 0 from userxlocation u2 where user_id = %s"
SQL_UPDATE_REVIEW = "update review set user_id = %s where id_review = %s"

con = psycopg2.connect(host='localhost', port=25432, database='mob',
        user='mob', password='mob')
cursor = con.cursor()

cursor.execute(SQL_SELECT_REVIEWS)
users = cursor.fetchall()
start_objs = []

for u in users:
    user_id = u[1].split('?')[0].split('/')[-1]
    cursor.execute(SQL_EXISTS_LOCATION, (str(user_id),))
    exists = cursor.fetchone()[0]
    if exists:
        print(f"found user - {user_id}!")
        if update:
            print(f"updating review {u[0]} with user_id -> {user_id}")
            cursor.execute(SQL_UPDATE_REVIEW, (user_id, u[0], ))
con.commit()
cursor.close()
