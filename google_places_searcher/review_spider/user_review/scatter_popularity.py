#!/usr/bin/env python3
import psycopg2
import sys
import matplotlib.pyplot as plt


SQL_SELECT_POPULARITY = """
with blocks_places as (select distinct pxca.id_place, cb.id_block from carnival_block cb 
join carnival_block_route cbr on cbr.id_block = cb.id_block
join placexcarnival_address pxca on pxca.id_address = cbr.id_address
),
tt_block as (select cb.id_block, count(txs.id_tweet) as ctt from carnival_block cb
join tweet_search_control tsc on tsc.id_block = cb.id_block
join tweetxsearch txs on txs.id_search = tsc.id_search
group by 1 
order by 2 desc)

select bp.id_block, c."name", c."date", coalesce(tt.ctt, 0), count (r.stars) as c, avg(r.stars), 
((count(r.stars)::decimal/2537) * (avg(r.stars)/5) + 
(((coalesce(tt.ctt::decimal, 0) ) / 5805)))/2 as popularidade
from blocks_places bp
join review r on r.id_place = bp.id_place
join carnival_block c on c.id_block = bp.id_block
left join tt_block tt on tt.id_block = c.id_block
group by bp.id_block, c."name", c."date", tt.ctt
order by popularidade desc
"""

con = psycopg2.connect(host='localhost', port=25432, database='mob',
        user='mob', password='mob')
cursor = con.cursor()

cursor.execute(SQL_SELECT_POPULARITY)
popularity = cursor.fetchall()

x = []
x_google = []
x_twitter = []

y = []
y_google = []
y_twitter = []

count = []

for p in popularity:
   # if p[-1] < 0.2:
        x.append(p[3] + p[4])
        x_google.append(p[4])
        x_twitter.append(p[3])

        y.append(p[-1])
        y_google.append(((p[4]/2537) * (float(p[5])/5)))
        y_twitter.append((p[3]/5805))
    #else:
    #    count.append(p[-1])

#plt.hist(count, bins=3)
fig, ax = plt.subplots()
ax.scatter(x, y, label='Twitter e Google Review')
ax.scatter(x_google, y_google, label="Google Review")
ax.scatter(x_twitter, y_twitter, label="Twitter")

ax.set_xlabel(r'interações em redes sociais')
ax.set_ylabel(r'popularidade')
ax.set_title('Popularidade e interações Twitter/Google Review')

plt.legend();

plt.show()
