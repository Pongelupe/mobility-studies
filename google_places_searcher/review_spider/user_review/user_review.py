#!/usr/bin/env python3
import psycopg2
import scrapy
import sys
from scrapy.crawler import CrawlerProcess
from user_review_spider import UserReviewSpider 
from apscheduler.schedulers.twisted import TwistedScheduler


def start_objs():
    SQL_SELECT_REVIEWS = "select distinct(r2.url_user) from review r2 where r2.user_id is null order by 1 asc limit 1"

    con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
    cursor = con.cursor()

    cursor.execute(SQL_SELECT_REVIEWS)
    users = cursor.fetchall()
    start_objs = []

    for u in users:
        user_id = u[0].split('?')[0].split('/')[-1]
        user = {'user_id': str(user_id), 'url': f"https://www.google.com/maps/contrib/{user_id}/photos?hl=pt-BR"}
        start_objs.append(user)
    return start_objs

process = CrawlerProcess(settings={
    'BOT_NAME': 'google_review_scrapper',
    'FEED_FORMAT': 'json',
    'USER_AGENT': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36',
   'ITEM_PIPELINES': {
       'pipelines.SaveUserReviewPipeline': 300
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
    scheduler.add_job(process.crawl, 'interval', args=[UserReviewSpider, lambda: start_objs()], seconds=30)
    scheduler.start()
    process.start(False)
else:
    process.crawl(UserReviewSpider, lambda: start_objs())
    process.start()
