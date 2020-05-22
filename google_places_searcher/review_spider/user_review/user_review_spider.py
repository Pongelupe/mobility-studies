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
                document.querySelector("#pane > div > div.widget-pane-content.scrollable-y > div > div > div.section-tab-bar > button.section-tab-bar-tab.ripple-container.section-tab-bar-tab-unselected").click()
            }
        ]])

        local get_max = splash:jsfunc([[
            function(resource) {
                var panel = document.querySelector('#pane div.section-layout.section-scrollbox.scrollable-y.scrollable-show');
                var quantity = panel.querySelector(resource == 0 ? '.section-tab-info-stats-label' : '.section-tab-info-stats-label');
                if (quantity) {
                    var value = quantity.textContent;
                    if (resource == 0) {
                        var realMax = parseInt(value.textContent.replace('.','')) // quantidade total de fotos ou comentários
                        return realMax > 50 ? realMax : 50;
                    } else {
                       var split = value.split(' ');
                       return split.length === 5 ? parseInt(split[0]) + parseInt(split[3]) : parseInt(split[0])
                    }
                } else {
                    return 0;
                }
            }
        ]])

        local has_to_scroll = splash:jsfunc([[
            function(resource, max) {
                var panel = document.querySelector('#pane div.section-layout.section-scrollbox.scrollable-y.scrollable-show');
                if (max > 0) {
                    if (resource == 0) {
                        photos = panel.querySelectorAll('.section-photo-bucket-photo') // quantidade fotos
                        return photos < max;
                    }
                } else {
                    return false;
                }
            }
        ]])


        assert(splash:wait(1.0))

        if ( args.initial and resource == 1 ) then
            open_comments()
            assert(splash:wait(0.75))
        end
        local max = get_max()

        return { html = splash:html(), resource = resource, has_to_scroll = has_to_scroll(resource, max), max = max}
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
        print(response)
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
        resource = response.data['resource']
        obj = response.meta['original_obj']
        if resource == 0: #photo
            if response.data['has_to_scroll']:
                print('scroll photos')
            else:
                print('scrap photos')
                print(f'response -------- >{response.data["max"]}')
                yield SplashRequest(obj['url'], self.parse_result_resource,
                        endpoint='execute',
                        dont_filter = True,
                        args={'lua_source': self.script_scroll_resource,
                            'timeout': 90,
                            'url': obj['url'], 'resource': 1, 'initial': True },
                        meta={'original_obj': obj, 'url': obj['url'], 'resource': 1, 'initial': True })

        else: #comment
            print(response.data)
            if response.data['has_to_scroll']:
                print('scroll comments')
            else:
                print('scrap comments')
                yield SplashRequest(obj['url'], self.parse_result_resource,
                        endpoint='execute',
                        dont_filter = True,
                        args={'lua_source': self.script_scroll_resource,
                            'timeout': 90,
                            'url': obj['url'], 'resource': 1, 'initial': False },
                        meta={'original_obj': obj, 'url': obj['url'], 'resource': 1, 'initial': False })
