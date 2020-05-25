#!/usr/bin/env python3
import psycopg2
import sys
import matplotlib.pyplot as plt

threshold = 0.5 if len(sys.argv) == 1 else float(sys.argv[1]) # The significance value to determinate a tourist
print_tourists_x_block = len(sys.argv) == 3 

SQL_SELECT_USERS = 'SELECT distinct user_id from review where user_id is not null'
SQL_SELECT_LOCATIONS = 'SELECT "location" from userxlocation where user_id = %s and "location" is not null'
SQL_SELECT_USER_BLOCK = """
                    select cb.id_block, cb."name", cb."date" from carnival_block cb 
                    join carnival_block_route cbr on cbr.id_block = cb.id_block
                    join placexcarnival_address pxca on pxca.id_address = cbr.id_address
                    join review r on r.id_place = pxca.id_place 
                    where r.user_id in %s
"""


con = psycopg2.connect(host='localhost', port=25432, database='mob',
        user='mob', password='mob')
cursor = con.cursor()

cursor.execute(SQL_SELECT_USERS)
users = cursor.fetchall()

tourists = []
unknown = 0

for u in users:
    user_id = u[0]
    cursor.execute(SQL_SELECT_LOCATIONS, (user_id,))
    locations = cursor.fetchall()

    total = len(locations)
    bh = 0 # number of reviews done in Belo Horizonte - MG 
    for l in locations:
        location = l[0]
        if 'Belo Horizonte - MG' in location:
            bh = bh + 1

    if total > 1:  
        tourist_level = (total - bh) / total
        if tourist_level >= threshold:
            tourists.append(user_id)
    else:
        unknown = unknown + 1

total = len(users)
tourists_count = len(tourists)
residents_count = total - unknown - tourists_count

print("Threshold: " + ">={0:.0%}".format(threshold))
print(f"Tourists: {tourists_count} - " + "({0:.2%})".format(tourists_count / total))
print(f"Residents: {residents_count} - " + "({0:.2%})".format(residents_count / total))
print(f"Unknown: {unknown} - " + "({0:.2%})".format(unknown / total))
print(f"Total: {total} - (100%)")

if print_tourists_x_block:
    tourists_x_block = {}
    cursor.execute(SQL_SELECT_USER_BLOCK, (tuple(tourists),) )
    res = cursor.fetchall()

    print("----------- Tourists per block -----------")
    print("Block's id | Tourists | Name")
    
    for r in res:
        id_block = r[0]
        if str(id_block) in tourists_x_block:
            tourists_x_block[str(id_block)]['count'] = tourists_x_block[str(id_block)]['count'] + 1 
        else:
            tourists_x_block[str(id_block)] = { 'count': 1, 'name': r[1], "date": r[2] } 

    names = []
    sizes = []
    for i,txb in tourists_x_block.items():
        print(f'{i} | {txb["count"]} | {txb["name"]}')
        names.append(f'{txb["name"]} ({txb["date"].strftime("%d/%m")})')
        sizes.append(txb["count"])

    fig1, ax1 = plt.subplots()
    ax1.pie(sizes, labels=names, autopct='%1.1f%%',
            shadow=True, startangle=90)
    ax1.axis('equal')  # Equal aspect ratio ensures that pie is drawn as a circle.

    plt.show()

