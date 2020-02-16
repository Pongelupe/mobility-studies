#!/usr/bin/env python3
import scrapy
from scrapy.crawler import CrawlerProcess
from belo_horizonte_spider import BeloHorizonteSpider

process = CrawlerProcess(settings={
        'BOT_NAME': 'carnaval_2020_scrapper',
        'FEED_FORMAT': 'json',
        'USER_AGENT': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36',
    })
process.crawl(BeloHorizonteSpider)
process.start()
