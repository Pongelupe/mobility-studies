import scrapy
from scrapy.exceptions import CloseSpider
from scrapy_splash import SplashRequest

class UserReviewSpider(scrapy.Spider):
    script_scroll_resource = """
    function main(splash, args)
        assert(splash:go(args.url))
        local resource = args.resource

        local open_comments = splash:jsfunc([[
            function() {
                document.querySelector('#pane > div > div.widget-pane-content.scrollable-y > div > div > div.section-tab-bar > button.section-tab-bar-tab.ripple-container.section-tab-bar-tab-unselected').click()
            }
        ]])

        local get_max = splash:jsfunc([[
            function(resource) {
                var panel = document.querySelector('#pane div.section-layout.section-scrollbox.scrollable-y.scrollable-show');
                var quantity = panel.querySelector(resource == 0 ? '.section-tab-info-stats-label' : '.section-tab-info-stats-label');
                if (quantity) {
                    var value = quantity.textContent;
                    if (resource == 0) {
                        var realMax = parseInt(value.replace('.','')) // quantidade total de fotos ou comentários
                        return realMax < 50 ? realMax : 50;
                    } else {
                       var split = value.split(' ');
                       var realMax = split.length === 5 ? parseInt(split[0]) + parseInt(split[3]) : parseInt(split[0])
                       return realMax < 50 ? realMax : 50;
                    }
                } else {
                    return 0;
                }
            }
        ]])

        local get_current = splash:jsfunc([[
            function(resource) {
                var panel = document.querySelector('#pane div.section-layout.section-scrollbox.scrollable-y.scrollable-show');
                return resource == 0 ? panel.querySelectorAll('.section-photo-bucket-photo').length : panel.querySelectorAll('.section-review-subtitle').length;
            }
        ]])

        local scroll = splash:jsfunc([[
            function() {
                var scroll = document.querySelector('#pane > div > div.widget-pane-content.scrollable-y > div > div > div.section-layout.section-scrollbox.scrollable-y.scrollable-show');
                scroll.scrollTop = scroll.scrollHeight;
            }
        ]])

        assert(splash:wait(1.0))

        if ( args.initial and resource == 1 ) then
            open_comments()
            assert(splash:wait(0.75))
        end

        local max = get_max(resource)
        local current = get_current(resource)

        while (current < max) do
            scroll()
            assert(splash:wait(2.0))
            current = get_current(resource)
        end

        --current = get_current(resource)

        return { html = splash:html(), resource = resource, max = max, current = current }
    end
    """

    def __init__(self, start_objs):
        self.start_objs = start_objs
        self.name = 'google user review spider'
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
        url = obj['url']
        yield SplashRequest(url, self.parse_result_resource,
                endpoint='execute',
                dont_filter = True,
                args={'lua_source': self.script_scroll_resource,
                    'timeout': 90,
                    'url': url, 'resource': 0, 'initial': True },
                meta={'original_obj': obj, 'url': url, 'resource': 0, 'initial': True }
                )

    def parse_result_resource(self, response):
        user_locations = [] if not 'user_locations' in response.meta else response.meta['user_locations']
        resource = response.data['resource']
        obj = response.meta['original_obj']
        max = response.data['max']
        current = response.data['current']
        if resource == 0: #photo
            locations = response.css('.section-photo-bucket-subtitle > span')
            for l in locations:
                location = l.xpath('.//text()').get()
                user_locations.append(location)

            yield SplashRequest(obj['url'], self.parse_result_resource,
                    endpoint='execute',
                    dont_filter = True,
                    args={'lua_source': self.script_scroll_resource,
                        'timeout': 90,
                        'url': obj['url'], 'resource': 1, 'initial': True },
                    meta={'original_obj': obj, 'url': obj['url'], 'resource': 1, 'initial': True, 'user_locations': user_locations })
        else: #comment
            locations = response.css('.section-review-subtitle')
            for l in locations:
                location = l.xpath('.//span[1]/text()').get()
                user_locations.append(location)
            
            yield {'user_id': obj['user_id'], 'user_locations': user_locations }
