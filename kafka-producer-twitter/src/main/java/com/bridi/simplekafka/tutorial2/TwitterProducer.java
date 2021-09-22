package com.bridi.simplekafka.tutorial2;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class TwitterProducer {

	public TwitterProducer() {}
	
	Logger logger = LoggerFactory.getLogger(TwitterProducer.class);
	
	String consumerKey = "YdTw1VxnmqwJn3RRKw1kWyCHG";
	String consumerSecret = "pFCQhSONs6RXxWM7c8uQt1D84tCAecyOkUg8EIvzRlIk35LcEa";
	String token = "1438476604380958721-acwVlaWmN3WvvLSky25hcpyYoWvNM7";
	String secret = "p7tD3yUmwdtvlZ65CoeHelXezspl4HKBzGSqfdRe97mpp";
	
	
	List<String> terms = Lists.newArrayList("bitcoin", "politics", "sports", "war");
	
	public static void main(String[] args) {
		new TwitterProducer().run();
	}
	
	public void run() {
		logger.info("Setup");
		//Set up your blocking queues: be sure the size these properly based on expected TPS of your stream		
		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(10000);
		// Create twitter client
		Client client = createTwitterClient(msgQueue);
		client.connect();
		
		// create kafka producer
		KafkaProducer<String, String> producer = createKafkaProducer();
		
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("stopping application...");
			logger.info("shutting down client from twitter...");
			client.stop();
			logger.info("stopping application...");
			producer.close();
		}));
		
		// loop to send tweets to kafka
		// on a different thread, or multiple different threads....
		while (!client.isDone()) {
			String msg = null;
			try {
				msg = msgQueue.poll(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				client.stop();
			}

			if (msg != null) {
				logger.info(msg);
				producer.send(new ProducerRecord<String, String>("twitter_tweets", null, msg), new Callback() {
					
					public void onCompletion(RecordMetadata arg0, Exception exception) {
						if (exception != null)
							logger.error("Something bad happend ", exception );
					}
				});
			}
			
		}
		logger.info("End of application");
	}
	
	private KafkaProducer<String, String> createKafkaProducer() {

		String bootstrapServers = "127.0.0.1:9092";
		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		//create safe producer
		properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
		properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
		properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
		properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
		
		// high throughput producer (at the expense of a bit of latency and cpu usage)
		properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
		properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
		properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024)); //32kb batch size
		
		// Create the producer
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
		
		
		return producer;
	}

	public Client createTwitterClient(BlockingQueue<String> msgQueue) {
		
		/** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		// Optional: set up some followings and track terms
		
		hosebirdEndpoint.trackTerms(terms);

		// These secrets should be read from a config file
		Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);
		
		ClientBuilder builder = new ClientBuilder()
				  .name("Hosebird-Client-01")                              // optional: mainly for the logs
				  .hosts(hosebirdHosts)
				  .authentication(hosebirdAuth)
				  .endpoint(hosebirdEndpoint)
				  .processor(new StringDelimitedProcessor(msgQueue));

		Client hosebirdClient = builder.build();
		return hosebirdClient;

	}
	
}
