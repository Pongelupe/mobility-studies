import scrapy
from search_item import CarnivalBlockItem

class BeloHorizonteSpider(scrapy.Spider):
    
    BASE_URL = 'http://portalbelohorizonte.com.br'

    def __init__(self):
        self.start_urls = []
        self.name = 'belo horizonte carnaval spider'
        for i in range(8, 31):
            if i == 30:
                date_to_search = '01/03/2020' # stands for march 1st
            else:
                date_to_search = f'0{i}/02/2020' if i < 10 else f'{i}/02/2020'# stands for dates in february
            url = f'{self.BASE_URL}/carnaval/buscabloc/?diadatabloc={date_to_search}'
            print(f"Crawling url {url}")
            self.start_urls.append(url)
        scrapy.Spider.__init__(self)

    def parse(self, response):
        hrefs = response.xpath("//a[starts-with(@href, '/carnaval')]//@href").getall()
        for href in hrefs:
            if href.startswith("/carnaval/bloco?reg="):
                request = scrapy.Request(f"{self.BASE_URL}{href},callback=self.parse_model")
                yield request

    def parse_model(self, response):
        print(response)
