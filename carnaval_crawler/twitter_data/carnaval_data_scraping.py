import tweepy
import psycopg2
import pymongo

#------------- SQL QUERIES
INSERT_SEARCH = "INSERT INTO public.tweet_search_control (id_block, initial_search_time) VALUES(%s, current_timestamp) RETURNING id_search"
INSERT_TWEET = "INSERT INTO public.tweet (id_tweet, truncated) VALUES(%s, %s)" 
INSERT_TWEETxSEARCH = "INSERT INTO public.tweetxsearch (id_tweet, id_search) VALUES(%s, %s)" 
EXISTS_TWEET = "SELECT COUNT(1) > 0 FROM public.tweet WHERE id_tweet = %s"
UPDATE_SEARCH = "UPDATE public.tweet_search_control SET final_search_time= current_timestamp WHERE id_search = %s "
#------------- SQL QUERIES

def job(id_block, newest_tweet_id, search_query, auth):
#------------- DATABASES
    con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
    cursor = con.cursor()

    mongo_client = pymongo.MongoClient('mongodb://localhost:27017')
    tweets_collection = mongo_client['carnaval_db']['tweets']
#------------- DATABASES

    api = tweepy.API(auth, wait_on_rate_limit=True)
    public_tweets = tweepy.Cursor(api.search, q=search_query).items(200) if newest_tweet_id == -1 else tweepy.Cursor(api.search, q=search_query, since_id = newest_tweet_id).items(200)
    cursor.execute(INSERT_SEARCH, (id_block, ))
    search_id = cursor.fetchone()[0]
    print(public_tweets)
    for tweet in public_tweets:
        tweet_id = tweet._json['id']
        cursor.execute(EXISTS_TWEET, (tweet_id, ))
        exists = cursor.fetchone()[0]
        if not exists: #new tweet!
            truncated = tweet._json['truncated']
            cursor.execute(INSERT_TWEET, (tweet_id, truncated, ))
            if not truncated:
                tweets_collection.insert_one(tweet._json)
                print('inserted on mongo')
            else: print('truncated!')
        cursor.execute(INSERT_TWEETxSEARCH, (tweet_id, search_id, ))
        con.commit()

    cursor.execute(UPDATE_SEARCH, (search_id, ))
    con.commit()

    cursor.close()
    mongo_client.close()
    print("job done")
