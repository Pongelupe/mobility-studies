import scrapy
from scrapy.exceptions import CloseSpider
from scrapy_splash import SplashRequest

class UserReviewSpider(scrapy.Spider):
    
    script_scroll_resource = """
    function main(splash, args)
        assert(splash:go(args.url))
        local click = splash:jsfunc([[
            function() {
                var a = document.querySelector("#pane > div > div.widget-pane-content.scrollable-y > div > div > div.section-layout.section-scrollbox.scrollable-y.scrollable-show")
a.querySelector('.section-tab-info-stats-label').textContent // quantidade total de fotos ou comentários
a.querySelectorAll('.section-photo-bucket-photo') // quantidade fotos

                document.querySelector("#pane > div > div.widget-pane-content.scrollable-y > div > div > div.section-tab-bar > button.section-tab-bar-tab.ripple-container.section-tab-bar-tab-unselected").click()
            }
        ]])

        local has_to_scroll = splash:jsfunc([[
            function(resource) {
                var panel = document.querySelector("#pane > div > div.widget-pane-content.scrollable-y > div > div > div.section-layout.section-scrollbox.scrollable-y.scrollable-show")
                var quantity = panel.querySelector('.section-tab-info-stats-label').textContent // quantidade total de fotos ou comentários
                
                if (quantity) {}

                if (resource === 1) {
                }

            }
        ]])

        local af = splash:jsfunc([[
            function() {
                return APP_INITIALIZATION_STATE[0][0][1]
            }
        ]])

        assert(splash:wait(1.0))

        return { a = af()}
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
        yield SplashRequest(url, self.parse_result,
                endpoint='execute',
                dont_filter = True,
                args={'lua_source': self.script_scroll_resource,
                    'timeout': 90,
                    'url': url, 'resource': 0 },
                meta={'original_obj': obj}
                )

    def parse_result(self, response):
       print(f'response -------- >{response.data}')
