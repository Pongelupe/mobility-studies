import scrapy
import base64
from scrapy_splash import SplashRequest

class ReviewSpider(scrapy.Spider):
    
    script_click_a = """
    function main(splash, args)
        assert(splash:go(args.url))
        local tot = splash:evaljs("Number.parseInt('".. args.reviews_count .. "')")

        local click = splash:jsfunc([[
            function(tot) {
                var as = document.querySelectorAll('a');
                var href = Array.prototype.slice.call(as, 0).filter(function(el) {
                    var span = el.querySelector('span');
                    return span && span.textContent.startsWith(tot);
                })[0];
                href.click()
            }
        ]])

        local sort = splash:jsfunc([[
            function() {
                var filter = 'Mais recentes';
                document.getElementsByTagName('g-dropdown-button')[0].click();
                var items = document.getElementsByTagName('g-menu-item');
                Array.prototype.slice.call(items, 0).filter(function(el) {
                    return el.querySelector('div').innerHTML === filter;
                })[0].click()
            }
        ]])

        click(tot)
        assert(splash:wait(1.0))

        if (tot > 10) then
            sort()
            assert(splash:wait(0.75))
        end

        local lines = splash:evaljs("document.querySelectorAll('.WMbnJf').length")
        
        local scroll = splash:jsfunc([[
            function() {
                var scroll = document.querySelector('#gsr > span > g-lightbox > div.ynlwjd.oLLmo.u98ib > div.AU64fe > span > div > div > div > div.review-dialog-list');
                scroll.scrollTop = scroll.scrollHeight;
                return document.querySelectorAll('.WMbnJf').length;
            }
        ]])

        local has_to_scroll = splash:jsfunc([[
            function() {
               var last = document.querySelectorAll('.PuaHbe > .dehysf').length - 1; 
               var lastComment = document.querySelectorAll('.PuaHbe > .dehysf')[last].textContent;
               
               var commentInRange = function(c) {
                    var split = c.split(' ');
                    if (split.length === 3) {
                        var time = split[0] === 'um' ? 1 : parseInt(split[0]);
                        return [2, 3, 4].includes(time) && split[1] === 'meses';
                    } else {
                        return false;
                    }
               };

               return commentInRange(lastComment);
            }
        ]])

        while( lines < tot ) do
            if has_to_scroll() then
                lines = scroll()
                assert(splash:wait(0.75))
            else
                lines = tot
            end
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
            print(f'{comments} for {obj["name"]} -> {obj["id"]}')
            comments_count = int(comments.split()[0].replace('.', ''))
            url = obj['url']
            yield SplashRequest(url, self.parse_result,
                    endpoint='execute',
                    dont_filter = True,
                    args={'lua_source': self.script_click_a,
                        'timeout': 90,
                        'url': url, 'reviews_count': comments_count},
                    meta={'original_obj': obj}
                    )
        else:
            print(f'0 for {obj["name"]} -> {obj["id"]}\n')
            yield {}

    def parse_result(self, response):
       #print(response.body)
        review_divs = response.css('.WMbnJf')

        i = 1
        for r in review_divs:
            reviewer = r.xpath('.//div[@class="TSUbDb"]/a').attrib['href']
            
            stars_and_relative_time = r.xpath('.//div[@class="PuaHbe"]')
            stars = int(stars_and_relative_time.xpath('.//g-review-stars/span/span').attrib['style'].split(':')[1][:-2]) / 14
            relative_time = stars_and_relative_time.xpath('.//span[@class="dehysf"]/text()').get()

            reviews = r.xpath('.//div[@class="Jtu6Td"]/span')
            hidden_review = reviews.xpath('.//span[@style="display:none"]/text()').get() 
            review = hidden_review if hidden_review is not None else reviews.xpath('.//text()').get()

            print(f"{i} --> {reviewer}, {stars}, {relative_time}, {review}, {response.meta['original_obj']}")
            i = i + 1

        print("\n--------------------------------------------\n")
