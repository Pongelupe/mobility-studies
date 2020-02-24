#!/usr/bin/env python3
import psycopg2
import schedule
import time
import tweepy
import sys
import carnaval_data_scraping

consumer_key = sys.argv[1] # Twitter consumer key
consumer_secret = sys.argv[2] # Twitter consumer secret
access_token = sys.argv[3] # Twitter access token
access_token_secret = sys.argv[4] # Twitter access token secret

auth = tweepy.OAuthHandler(consumer_key, consumer_secret)
auth.set_access_token(access_token, access_token_secret)

con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
cursor = con.cursor()

SELECT_ID_BLOCK_FIRST_TIME = """
SELECT id_block FROM carnival_block cb
	WHERE cb.id_block NOT IN (SELECT id_block FROM tweet_search_control)
	ORDER BY cb.id_block LIMIT 1;
"""
SELECT_ID_BLOCK_LATER = """
select
	id_block
from
	(
	select
		tsc.id_block,
		count(txs.*) as c
	from
		tweet_search_control tsc
	join carnival_block cb on
		cb.id_block = tsc.id_block
	join tweetxsearch txs on
		txs.id_search = tsc.id_search
	where
		cb."date" < current_timestamp
		and tsc.final_search_time is not null
	group by
		1
	order by
		2) as ids_and_tweet_count
where ids_and_tweet_count.id_block in (select tsc2.id_block from tweet_search_control tsc2
where tsc2.final_search_time < current_timestamp - interval '30 minutes'
and tsc2.final_search_time = (select max(tsc3.final_search_time) from tweet_search_control tsc3 where tsc3.id_block = ids_and_tweet_count.id_block limit 1))
order by ids_and_tweet_count.c
limit 1;
"""
SELECT_NEWEST_TWEET_BY_BLOCK_ID = """
select max(txs.id_tweet) from tweetxsearch txs
join tweet_search_control tsc on tsc.id_search = txs.id_search
where tsc.id_block = %s
"""
SELECT_BLOCK_NAME_BY_BLOCK_ID = 'select "name" from carnival_block where id_block = %s'

def get_block_id():
    cursor.execute(SELECT_ID_BLOCK_FIRST_TIME)
    block = cursor.fetchone()
    if block == None: # all blocks have been scraped at least once!
        cursor.execute(SELECT_ID_BLOCK_LATER)
        return cursor.fetchone()[0]
    else:
        return block[0]

def get_newest_tweet_by_block_id(block_id):
    cursor.execute(SELECT_NEWEST_TWEET_BY_BLOCK_ID, (block_id, ))
    newest = cursor.fetchone()
    return -1 if newest[0] == None else newest[0]

def get_search_query_block_id(block_id):
    cursor.execute(SELECT_BLOCK_NAME_BY_BLOCK_ID, (block_id, ))
    name = cursor.fetchone()[0]
    if name.startswith('BLOCO') and len(name.split()) == 2:
        return f'"{name}" OR {name.split()[1]}'
    else: return name

def job(block, newest_tweet, search_query, auth):
    block_id = block()
    newest_tweet_id = newest_tweet(block_id)
    search_query_text = search_query(block_id)
    print(f"{block_id},{newest_tweet_id},{search_query_text}")
    carnaval_data_scraping.job(block_id, newest_tweet_id, search_query_text, auth)

schedule.every().seconds.do(job, lambda : get_block_id(), lambda block_id :get_newest_tweet_by_block_id(block_id), lambda block_id : get_search_query_block_id(block_id), auth)

#execute at first
job(lambda : get_block_id(), lambda block_id :get_newest_tweet_by_block_id(block_id), lambda block_id : get_search_query_block_id(block_id), auth)


while True:
    schedule.run_pending()
    time.sleep(1)
