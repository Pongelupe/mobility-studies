import psycopg2
from scrapy.exceptions import DropItem

class SaveUserReviewPipeline(object):

    SQL_UPDATE_REVIEW = "update review set user_id = %s where url_user like '%%/%s%%'"
    SQL_INSERT_USERXLOCATION = 'INSERT INTO public.userxlocation (user_id, "location") VALUES(%s, %s)'
    SQL_EXISTS_USERXLOCATION = 'SELECT COUNT(1) > 0 FROM public.userxlocation WHERE user_id = %s'

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
        user_id = item['user_id']
        self.cursor.execute(self.SQL_UPDATE_REVIEW, (user_id, int(user_id), ))
        self.connection.commit()

        self.cursor.execute(self.SQL_EXISTS_USERXLOCATION, (user_id, ))
        exists = self.cursor.fetchone()[0]
        
        if not exists:
            for r in item['user_locations']:
                self.cursor.execute(self.SQL_INSERT_USERXLOCATION, (user_id, r, ))
            self.connection.commit()
        else:
            raise DropItem("Duplicated user!")
