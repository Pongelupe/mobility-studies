import psycopg2
from scrapy.exceptions import DropItem

class FilterPipeline(object):

    SQL_INSERT_BIG_REVIEW = 'INSERT INTO public.big_reviews (id_place, commments) VALUES(%s, %s)'

    def __init__(self, postgres_host, postgres_port, postgres_db, postgres_user, postgres_password):
        self.postgres_host = postgres_host
        self.postgres_port = postgres_port
        self.postgres_db = postgres_db
        self.postgres_user = postgres_user
        self.postgres_password = postgres_password

    @classmethod
    def from_crawler(cls, crawler):
        return cls(
                postgres_host = crawler.settings.get('POSTGRES_HOST'),
                postgres_port = crawler.settings.get('POSTGRES_PORT'),
                postgres_db = crawler.settings.get('POSTGRES_DB'),
                postgres_user = crawler.settings.get('POSTGRES_USER'),
                postgres_password = crawler.settings.get('POSTGRES_PASSWORD'),
                )

    def open_spider(self, spider):
        self.connection = psycopg2.connect(host=self.postgres_host, port=self.postgres_port, database=self.postgres_db, user=self.postgres_user, password=self.postgres_password)
        self.cursor = self.connection.cursor()

    def close_spider(self, spider):
        self.connection.commit()
        self.cursor.close()

    def process_item(self, item, spider):
        if not item:
            raise DropItem("No comments!")
        elif 'relative_time' in item and not self.date_in_range(item['relative_time']):
            raise DropItem("Review date out of range!")
        else:
            if not 'comments' in item:
                return item
            else:
                self.cursor.execute(self.SQL_INSERT_BIG_REVIEW, (item['place_id'], item['comments'],))
                raise DropItem("Big review")



    def date_in_range(self, comment_relative_date):
        split = comment_relative_date.split()
        if len(split) == 3:
            time = 1 if split[0].startswith('um') else int(split[0])
            return time in [3,4] and split[1] == 'meses'
        else:
            return False

class SaveReviewPipeline(object):

    SQL_INSERT_REVIEW = "INSERT INTO public.review(url_user, stars, relative_time, review_text, id_place) VALUES(%s, %s, %s, %s, %s)"
    SQL_EXISTS_PLACEXREVIEW_SEARCH = "select count(1) > 0 from placexreview_search ps where id_place = %s"
    SQL_INSERT_PLACEXREVIEW_SEARCH = "INSERT INTO public.placexreview_search (id_place) VALUES(%s)"

    def __init__(self, postgres_host, postgres_port, postgres_db, postgres_user, postgres_password):
        self.postgres_host = postgres_host
        self.postgres_port = postgres_port
        self.postgres_db = postgres_db
        self.postgres_user = postgres_user
        self.postgres_password = postgres_password

    @classmethod
    def from_crawler(cls, crawler):
        return cls(
                postgres_host = crawler.settings.get('POSTGRES_HOST'),
                postgres_port = crawler.settings.get('POSTGRES_PORT'),
                postgres_db = crawler.settings.get('POSTGRES_DB'),
                postgres_user = crawler.settings.get('POSTGRES_USER'),
                postgres_password = crawler.settings.get('POSTGRES_PASSWORD'),
                )

    def open_spider(self, spider):
        self.connection = psycopg2.connect(host=self.postgres_host, port=self.postgres_port, database=self.postgres_db, user=self.postgres_user, password=self.postgres_password)
        self.cursor = self.connection.cursor()

    def close_spider(self, spider):
        self.connection.commit()
        self.cursor.close()

    def process_item(self, item, spider):
        print(item)
        self.cursor.execute(self.SQL_INSERT_REVIEW, (item['reviewer'], item['stars'], item['relative_time'], item['review'], item['id_place'], ))
