import tweepy
import psycopg2
import pymongo

SELECT_1000_TRUNCATED_TWEETS = "SELECT id_tweet FROM tweet WHERE truncated = true LIMIT 1000"
UPDATE_TRUNCATED_TWEETS = "UPDATE public.tweet SET truncated = false WHERE id_tweet = %s"
DELETE_TWEETXSEARCH = "DELETE FROM tweetxsearch WHERE id_tweet = %s"
DELETE_TWEET = "DELETE FROM tweet WHERE id_tweet = %s"

def job(auth):
#------------- DATABASES
    con = psycopg2.connect(host='localhost', port=25432, database='mob',
            user='mob', password='mob')
    cursor = con.cursor()

    mongo_client = pymongo.MongoClient('mongodb://localhost:27017')
    tweets_collection = mongo_client['carnaval_db']['tweets']
#------------- DATABASES
    cursor.execute(SELECT_1000_TRUNCATED_TWEETS)
    truncated_tweets = cursor.fetchall()

    api = tweepy.API(auth, wait_on_rate_limit=True)

    for truncated_tweet in truncated_tweets:
        tweet_id = truncated_tweet[0]
        try:
            status = api.get_status(tweet_id, tweet_mode ="extended")
            tweets_collection.insert_one(status._json)
            cursor.execute(UPDATE_TRUNCATED_TWEETS, (tweet_id, ))
            con.commit()
            print(f"tweet with id {tweet_id} is not truncated anymore!")
        except:
            print(f"deleting bad data: id {tweet_id}")
            cursor.execute(DELETE_TWEETXSEARCH, (tweet_id, ))
            cursor.execute(DELETE_TWEET, (tweet_id, ))

    cursor.close()
    mongo_client.close()
    print("job done")
