import scrapy
from scrapy_splash import SplashRequest

class ReviewSpider(scrapy.Spider):
    
    script_click_a = """
    function main(splash, args)
        splash:runjs("document.querySelector('#rhs > div > div.kp-blk.knowledge-panel.Wnoohf.OJXvsb > div > div.ifM9O > div > div.kp-header > div:nth-child(2) > div.fYOrjf.iB08Xb.kp-hc > div:nth-child(2) > div > div > span.hqzQac > span > a').click()")
        return splash:html()
    end
    """

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
        comments = response.xpath('//*[@id="rhs"]/div/div[1]/div/div[1]/div/div[1]/div[2]/div[2]/div[2]/div/div/span[2]/span/a/span/text()').get()
        if comments:
            name = response.xpath('//*[@id="rhs"]/div/div[1]/div/div[1]/div/div[1]/div[2]/div[2]/div[1]/div/div[2]/div[1]/span/text()').get()
            print(f'{comments} for {name}')
            url = response.request.url
            yield SplashRequest(url, self.parse_result,
                    endpoint='execute',
                    args={'lua_source': self.script_click_a,
                        'url': url},
                    )
        else:
            yield {}

    def parse_result(self, response):
        print(response.body)
