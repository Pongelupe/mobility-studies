#!/usr/bin/env python3
import psycopg2

SQL_SELECT_USERS = 'SELECT distinct url_user from review where user_id is null'

con = psycopg2.connect(host='localhost', port=25432, database='mob',
        user='mob', password='mob')
cursor = con.cursor()

cursor.execute(SQL_SELECT_USERS)
users = cursor.fetchall()

users_to_go = set()

for u in users:
    user_id = u[0].split('?')[0].split('/')[-1]
    users_to_go.add(user_id)

print(f"{len(users_to_go)} users from {len(users)} reviews to go")
