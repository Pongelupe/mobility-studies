#!/usr/bin/env python3
import psycopg2
import pymongo
import scrapy
import sys
from scrapy.crawler import CrawlerProcess
from review_spider import ReviewSpider 
from apscheduler.schedulers.twisted import TwistedScheduler


#SQL_SELECT_PLACES = "select p2.id_place from place p2 where p2.id_place not in (select id_place from placexreview_search ps) limit 1"
SQL_SELECT_PLACES = """
select
	p.id_place
from
	place p
join typexplace txp on
	txp.id_place = p.id_place
join place_type pt on
	pt.id_place_type = txp.id_place_type
where
	pt.desc_type in ('bar',
	'cafe',
	'convenience_store',
	'pharmacy',
	'liquor_store',
	'restaurant',
	'tourist_attraction')
	and p.id_place not in (
	select
		pxrs.id_place
	from
		placexreview_search pxrs)
limit 1
"""
SQL_INSERT_PLACEXREVIEW_SEARCH = "INSERT INTO public.placexreview_search (id_place) VALUES(%s)"


def start_objs():
    mongo_client = pymongo.MongoClient('mongodb://localhost:27017')
    places_collection = mongo_client['carnaval_db']['places']

    con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
    cursor = con.cursor()

    cursor.execute(SQL_SELECT_PLACES, )
    places_id = cursor.fetchall()
    places = places_collection.find({'id': { '$in': list(map(lambda p: p[0], places_id)) }})
    start_objs = []

    for place in places:
        q = ''
        if 'vicinity' in place:
            q = f"{place['name']} - {place['vicinity']}"
        else:
            q = place['name']
        url = f'https://www.google.com/search?q="{q}"&ie=UTF-8'
        start_objs.append({'url': url, 'name': place['name'], 'id': place['id']})
    return start_objs

process = CrawlerProcess(settings={
    'BOT_NAME': 'google_review_scrapper',
    'FEED_FORMAT': 'json',
    'USER_AGENT': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36',
    'DOWNLOAD_DELAY': 0.75,
'ITEM_PIPELINES': {
    'pipelines.FilterPipeline': 300,
    'pipelines.SaveReviewPipeline': 400
    },
    'DOWNLOADER_MIDDLEWARES': {
        'scrapy_splash.SplashCookiesMiddleware': 723,
        'scrapy_splash.SplashMiddleware': 725,
        'scrapy.downloadermiddlewares.httpcompression.HttpCompressionMiddleware': 810,
        },
    'POSTGRES_HOST': 'localhost',
    'POSTGRES_PORT': '25432',
    'POSTGRES_DB': 'mob',
    'POSTGRES_USER': 'mob',
    'POSTGRES_PASSWORD': 'mob'
    })

if len(sys.argv) == 1:
    scheduler = TwistedScheduler()
    scheduler.add_job(process.crawl, 'interval', args=[ReviewSpider, lambda: start_objs()], seconds=45)
    scheduler.start()
    process.start(False)
else:
    process.crawl(ReviewSpider, lambda: start_objs())
    process.start()
