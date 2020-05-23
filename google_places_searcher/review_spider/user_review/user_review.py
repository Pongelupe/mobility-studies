#!/usr/bin/env python3
import psycopg2
import scrapy
from scrapy.crawler import CrawlerProcess
from user_review_spider import UserReviewSpider 

SQL_SELECT_PLACES = "select p2.id_place from place p2 where p2.id_place not in (select id_place from placexreview_search ps) limit 10"
SQL_INSERT_PLACEXREVIEW_SEARCH = "INSERT INTO public.placexreview_search (id_place) VALUES(%s)"

#con = psycopg2.connect(host='localhost', port=25432, database='mob',
#            user='mob', password='mob')
#cursor = con.cursor()

user2 = 'https://www.google.com/maps/contrib/107162384527452903903?hl=pt-BR&sa=X&ved=2ahUKEwiEn9Cgj8bpAhVGeawKHQtHAdYQvvQBegQIARAt'
user = 'https://www.google.com/maps/contrib/110236640560488811231/photos/@-19.8615537,-43.9496531,13z/data=!3m1!4b1!4m3!8m2!3m1!1e1?hl=pt-BR'

start_objs = [{'url': user}, {'url': user2}]

process = CrawlerProcess(settings={
    'BOT_NAME': 'google_review_scrapper',
    'FEED_FORMAT': 'json',
    'USER_AGENT': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36',
   'ITEM_PIPELINES': {
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
process.crawl(UserReviewSpider, start_objs)
process.start()
