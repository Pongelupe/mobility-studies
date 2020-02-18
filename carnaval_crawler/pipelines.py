import psycopg2

class PostgresPipeline(object):

    SQL_SELECT_ADDRESS = ' SELECT id_address from carnival_address where desc_address = %s '
    SQL_SELECT_PROFILE = ' SELECT id_profile from profile where desc_profile = %s '
    SQL_SELECT_MUSIC_TYPE = ' SELECT id_music_type from music_type where desc_music_type = %s '

    SQL_INSERT_ADDRESS = ' INSERT INTO carnival_address (desc_address) VALUES(%s) returning id_address '
    SQL_INSERT_PROFILE = ' INSERT INTO profile (desc_profile) VALUES(%s) RETURNING id_profile '
    SQL_INSERT_MUSIC_TYPE = ' INSERT INTO music_type (desc_music_type) VALUES(%s) RETURNING id_music_type '
    SQL_INSERT_CARNIVAL_BLOCK = "INSERT INTO carnival_block (name, description, date, start_address_id, final_address_id) VALUES(%s, %s, to_timestamp(%s,'dd/mm/yyy hh24:mi' ), %s, %s) RETURNING id_block "

    SQL_INSERT_MUSIC_TYPEXCARNIVAL_BLOCK = 'INSERT INTO music_typexcarnival_block (id_block, id_music_type) VALUES(%s, %s)'
    SQL_INSERT_PROFILEXCARNIVAL_BLOCK = 'INSERT INTO profilexcarnival_block (id_block, id_profile) VALUES(%s, %s)'
    SQL_INSERT_CARNIVAL_BLOCK_ROUTE = 'INSERT INTO carnival_block_route (id_block, id_address, "order") VALUES(%s, %s, %s)'

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
        start_address = self.retreive_or_insert(self.SQL_SELECT_ADDRESS, self.SQL_INSERT_ADDRESS, item['start_address'])
        final_address = self.retreive_or_insert(self.SQL_SELECT_ADDRESS, self.SQL_INSERT_ADDRESS, item['final_address'])
        self.cursor.execute(self.SQL_INSERT_CARNIVAL_BLOCK, (item['name'], item['description'], f'{item["day"]} {item["time"]}', start_address, final_address, ))
        id_block = self.cursor.fetchone()[0]

        # insert music_typexcarnival_block
        self.insert_list_items_related_to_block(self.SQL_SELECT_MUSIC_TYPE, self.SQL_INSERT_MUSIC_TYPE, self.SQL_INSERT_MUSIC_TYPEXCARNIVAL_BLOCK, item['music_types'], id_block)

        # insert profilexcarnival_block
        self.insert_list_items_related_to_block(self.SQL_SELECT_PROFILE, self.SQL_INSERT_PROFILE, self.SQL_INSERT_PROFILEXCARNIVAL_BLOCK, item['profiles'], id_block)

        # insert profilexcarnival_block
        self.insert_list_items_related_to_block(self.SQL_SELECT_ADDRESS, self.SQL_INSERT_ADDRESS, self.SQL_INSERT_CARNIVAL_BLOCK_ROUTE, item['route'], id_block, '->', True)

        return item
    
    def retreive_or_insert(self, sql_select, sql_insert, value):
        self.cursor.execute(sql_select, (value.strip(), ))
        db_id = self.cursor.fetchone()
        if db_id == None:
            self.cursor.execute(sql_insert, (value.strip(), ))
            db_id = self.cursor.fetchone()[0]
        else:
            db_id = db_id[0]
        return db_id

    def insert_list_items_related_to_block(self, sql_select, sql_insert_single, sql_insert_block, values, id_block, delimiter = ',', enumerated = False):
        for i, value in enumerate(values.split(delimiter)):
            id_target = self.retreive_or_insert(sql_select, sql_insert_single, value)
            if enumerated:
                self.cursor.execute(sql_insert_block, (id_block, id_target, i, ))
            else:
                self.cursor.execute(sql_insert_block, (id_block, id_target, ))
