import scrapy
from scrapy_splash import SplashRequest

class ReviewSpider(scrapy.Spider):
    
    def __init__(self, start_urls):
        self.start_urls = start_urls
        self.name = 'google review spider'
        scrapy.Spider.__init__(self)

    def start_requests(self):
        for url in self.start_urls:
            yield SplashRequest(url, self.parse,
                endpoint='render.html',
                args={'wait': 0.5},
            )
    def parse(self, response):
        print(response.body)
        no_comments = response.xpath('//*[@id="wrl"]/span/span').get()
        if no_comments:
            print(no_comments)
        else:
           #print(response.xpath('//*[@id="wrl"]/span/span').get())
            #print(response.xpath('//*[@id="rhs"]/div/div[1]/div/div[1]/div/div[7]/div[2]/div[2]/span/span/a/span').get())
            print(response.xpath('//*[@id="rhs"]/div/div[1]/div/div[1]/div/div[7]/div[2]/div[2]/span').get())
