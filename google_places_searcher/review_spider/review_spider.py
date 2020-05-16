import scrapy
import base64
from scrapy_splash import SplashRequest

class ReviewSpider(scrapy.Spider):
    
    script_click_a = """
    function main(splash, args)
        assert(splash:go(args.url))
        splash:runjs("document.querySelector('#rhs > div > div.kp-blk.knowledge-panel.Wnoohf.OJXvsb > div > div.ifM9O > div > div.kp-header > div:nth-child(2) > div.fYOrjf.iB08Xb.kp-hc > div:nth-child(2) > div > div > span.hqzQac > span > a').click()")
        assert(splash:wait(0.75))

        local l = splash:evaljs("document.querySelectorAll('.WMbnJf').length")
        local tot = splash:evaljs("Number.parseInt('".. args.reviews_count .. "')")
        
        local s = splash:jsfunc([[
            function(tot) {
                var scroll = document.querySelector('#gsr > span > g-lightbox > div.ynlwjd.oLLmo.u98ib > div.AU64fe > span > div > div > div > div.review-dialog-list');
                scroll.scrollTop = scroll.scrollHeight;
                return document.querySelectorAll('.WMbnJf').length;
            }
        ]])

        while( l < tot )
        do
            l = s(tot)
            assert(splash:wait(2.75))
        end

        return splash:html()
    end
    """

    def __init__(self, start_objs):
        self.start_objs = start_objs
        self.name = 'google review spider'
        scrapy.Spider.__init__(self)

    def start_requests(self):
        for obj in self.start_objs:
            yield SplashRequest(obj['url'], self.parse,
                endpoint='render.html',
                args={'wait': 0.5},
                meta={'original_obj': obj}
            )
    def parse(self, response):
        obj = response.meta['original_obj']
        comments = response.xpath('//*[@id="rhs"]/div/div[1]/div/div[1]/div/div[1]/div[2]/div[2]/div[2]/div/div/span[2]/span/a/span/text()').get()
        if comments:
            name = response.xpath('//*[@id="rhs"]/div/div[1]/div/div[1]/div/div[1]/div[2]/div[2]/div[1]/div/div[2]/div[1]/span/text()').get()
            print(f'{comments} for {obj["name"]}\n')
            comments_count = int(comments.split()[0].replace('.', ''))
            url = obj['url']
            yield SplashRequest(url, self.parse_result,
                    endpoint='execute',
                    dont_filter = True,
                    args={'lua_source': self.script_click_a,
                        'url': url, 'reviews_count': comments_count},
                    meta={'reviews_count': comments_count}
                    )
        else:
            yield {}

    def parse_result(self, response):
       # print(response.body)
        review_divs = response.css('.WMbnJf').extract()

        reviewrs = response.xpath('//div[@class="TSUbDb"]/a')
        comment_count = response.xpath('//a[@class="Msppse"]/span/text()').getall()
        stars_and_relative_time = response.xpath('//div[@class="PuaHbe"]')
        reviews = response.xpath('//div[@class="Jtu6Td"]/span')

        for i,r in enumerate(review_divs):
            hidden_review = reviews[i].xpath('.//span[@style="display:none"]/text()').get() 
            review = hidden_review if hidden_review is not None else reviews[i].xpath('.//text()').get()
            print(f"""{i} --> {reviewrs[i].attrib['href']}, {stars_and_relative_time[i].xpath('.//span[@class="dehysf"]/text()').get()}, {comment_count[i]}, {stars_and_relative_time[i].xpath('.//g-review-stars/span/span').attrib['style'].split(':')[1][:-2]}, {review}""")

