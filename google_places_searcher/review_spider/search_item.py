import scrapy

class SearchItem(scrapy.Item):
    id_place = scrapy.Field()
    reviewer = scrapy.Field()
    stars = scrapy.Field()
    relative_time = scrapy.Field()
    review = scrapy.Field()
