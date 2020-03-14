import scrapy
import base64
from scrapy_splash import SplashRequest

class ReviewSpider(scrapy.Spider):
    
    script_click_a = """
    function main(splash, args)
        assert(splash:go(args.url))
        splash:runjs("document.querySelector('#rhs > div > div.kp-blk.knowledge-panel.Wnoohf.OJXvsb > div > div.ifM9O > div > div.kp-header > div:nth-child(2) > div.fYOrjf.iB08Xb.kp-hc > div:nth-child(2) > div > div > span.hqzQac > span > a').click()")
        assert(splash:wait(0.75))
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
                meta={'original_url': url}
            )
    def parse(self, response):
        comments = response.xpath('//*[@id="rhs"]/div/div[1]/div/div[1]/div/div[1]/div[2]/div[2]/div[2]/div/div/span[2]/span/a/span/text()').get()
        if comments:
            name = response.xpath('//*[@id="rhs"]/div/div[1]/div/div[1]/div/div[1]/div[2]/div[2]/div[1]/div/div[2]/div[1]/span/text()').get()
            print(f'{comments} for {name}')
            comments_count = int(comments.split()[0].replace('.', ''))
            url = response.meta['original_url']
            yield SplashRequest(url, self.parse_result,
                    endpoint='execute',
                    dont_filter = True,
                    args={'lua_source': self.script_click_a,
                        'url': url},
                    meta={'reviews_count': comments_count}
                    )
        else:
            yield {}

    def parse_result(self, response):
        review_divs = response.css('.WMbnJf').extract()

        reviewrs = response.xpath('//div[@class="TSUbDb"]/a')
        comment_count = response.xpath('//a[@class="Msppse"]/span/text()').getall()
        stars_and_relative_time = response.xpath('//div[@class="PuaHbe"]')
        reviews = response.xpath('//div[@class="Jtu6Td"]/span')

        for i,r in enumerate(review_divs):
            hidden_review = reviews[i].xpath('.//span[@style="display:none"]/text()').get() 
            review = hidden_review if hidden_review is not None else reviews[i].xpath('.//text()').get()
            print(f"""{i} --> {reviewrs[i].attrib['href']}, {comment_count[i]}, {stars_and_relative_time[i].xpath('.//span[@class="dehysf"]/text()').get()}, {comment_count[i]}, {stars_and_relative_time[i].xpath('.//g-review-stars/span/span').attrib['style'].split(':')[1][:-2]}, {review}""")

