#!/usr/bin/env python3
import psycopg2
import pymongo
import scrapy
from scrapy.crawler import CrawlerProcess
from review_spider import ReviewSpider 

SQL_SELECT_PLACES = "select p2.id_place from place p2 where p2.id_place not in (select id_place from placexreview_search ps) limit 5"
SQL_INSERT_PLACEXREVIEW_SEARCH = "INSERT INTO public.placexreview_search (id_place) VALUES(%s)"

def job():
    mongo_client = pymongo.MongoClient('mongodb://localhost:27017')
    places_collection = mongo_client['carnaval_db']['places']

    con = psycopg2.connect(host='localhost', port=25432, database='mob',
                user='mob', password='mob')
    cursor = con.cursor()

    cursor.execute(SQL_SELECT_PLACES, )
    places_id = cursor.fetchall()
    places = places_collection.find({'id': { '$in': list(map(lambda p: p[0], places_id)) }})#.skip(4)
    start_objs = []

    for place in places:
        q = ''
        if 'vicinity' in place:
            q = f"{place['name']} - {place['vicinity']}"
        else:
            q = place['name']
        url = f'https://www.google.com/search?q="{q}"&ie=UTF-8'
        start_objs.append({'url': url, 'name': place['name'], 'id': place['id']})

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
    process.crawl(ReviewSpider, start_objs)
    process.start()
    print('end');
    for obj in start_objs:
        cursor.execute(SQL_INSERT_PLACEXREVIEW_SEARCH, (obj['id'], ))    
        con.commit()
    cursor.close()
job()
