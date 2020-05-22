import scrapy
from search_item import SearchItem
from scrapy.exceptions import CloseSpider
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
                    return span && span.textContent.replace('.', '').startsWith(tot);
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
        assert(splash:wait(2.0))

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
                        var time = split[0].startsWith('um') ? 1 : parseInt(split[0]);
                        return ([3, 4].includes(time) || time < 3) && (['meses', 'mês', 'semana', 'semanas', 'dias', 'dia', 'horas', 'hora', 'minutos', 'minuto'].includes(split[1]));
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
        self.start_objs = start_objs()
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
       #print(response.body)
        obj = response.meta['original_obj']
        comments = response.xpath('//span[contains(., "comentários no Google")][1]/text()').get()
        if comments and int(comments.split()[0].replace('.', '')) < 4000:
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
        elif response.xpath('//*[@id="captcha-form"]').get():
            raise CloseSpider("Captcha out!")
        else:
            if comments:
                yield { 'place_id': obj['id'], 'comments': int(comments.split()[0].replace('.', '')) }
            else:
                print(f'0 for {obj["name"]} -> {obj["id"]}\n')
                yield {'place_id': obj['id']}

    def parse_result(self, response):
       #print(response.body)
        review_divs = response.css('.WMbnJf')

        i = 1
        for r in review_divs:
            complete_review = SearchItem()
            complete_review['place_id'] = response.meta['original_obj']['id']

            reviewer = r.xpath('.//div[@class="TSUbDb"]/a').attrib['href']
            
            stars_and_relative_time = r.xpath('.//div[@class="PuaHbe"]')
            stars = int(stars_and_relative_time.xpath('.//g-review-stars/span/span').attrib['style'].split(':')[1][:-2]) / 14
            relative_time = stars_and_relative_time.xpath('.//span[@class="dehysf"]/text()').get()

            reviews = r.xpath('.//div[@class="Jtu6Td"]/span')
            hidden_review = reviews.xpath('.//span[@style="display:none"]/text()').get() 
            review = hidden_review if hidden_review is not None else reviews.xpath('.//text()').get()

            i = i + 1
            
            complete_review['reviewer'] = reviewer
            complete_review['stars'] = stars
            complete_review['relative_time'] = relative_time
            complete_review['review'] = review

            yield complete_review  
