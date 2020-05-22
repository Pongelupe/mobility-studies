import scrapy

class SearchItem(scrapy.Item):
    place_id = scrapy.Field()
    reviewer = scrapy.Field()
    stars = scrapy.Field()
    relative_time = scrapy.Field()
    review = scrapy.Field()
