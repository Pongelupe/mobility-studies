import scrapy

class CarnivalBlockItem(scrapy.Item):
    name = scrapy.Field()
    description = scrapy.Field()
    day = scrapy.Field()
    time = scrapy.Field()
    profiles = scrapy.Field()
    start_address = scrapy.Field()
    final_address = scrapy.Field()
    route = scrapy.Field()
