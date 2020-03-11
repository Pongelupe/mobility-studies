from scrapy.exceptions import DropItem

class FilterPipeline(object):

    def process_item(self, item, spider):
        if not item:
            raise DropItem("No comments!")
        else:
            return item
