#!/usr/bin/env python3
import pymongo
import scrapy
from scrapy.crawler import CrawlerProcess
from review_spider import ReviewSpider 

mongo_client = pymongo.MongoClient('mongodb://localhost:27017')
places_collection = mongo_client['carnaval_db']['places']

#places = places_collection.find({'name': 'Espaço Maggiore'})
places = places_collection.find().limit(5).skip(13)
#places = places_collection.find({})
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
   'ITEM_PIPELINES': {
       'pipelines.FilterPipeline': 300
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
