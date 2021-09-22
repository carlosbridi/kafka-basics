package com.bridi.simplekafka.tutorial1;

import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerDemoWithCallback {

	public static void main(String[] args) {

		final Logger logger = LoggerFactory.getLogger(ProducerDemoWithCallback.class);

		// Create producer properties

		Properties properties = new Properties();

		String bootstrapServers = "127.0.0.1:9092";
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		// Create the producer
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

		for (int i = 0; i <= 10; i++) {

			// Kafka needs to run! (zookeeeper too!!)
			ProducerRecord<String, String> record = new ProducerRecord<String, String>("first_topic", "hello world! " + i);

			// SendData
			producer.send(record, new Callback() {

				public void onCompletion(RecordMetadata recordMeta, Exception e) {
					// executes every tiime a record is successefully sent or an exception is thrown

					if (e == null) {
						logger.info("Received new metada. \n" + "Topic: " + recordMeta.topic() + "\n" + "Partition: "
								+ recordMeta.partition() + "\n" + "Offset: " + recordMeta.offset() + "\n"
								+ "Timestamp: " + recordMeta.timestamp());
					} else {
						logger.error("Error while producing message: ", e);
					}
				}
			});

			producer.flush();
		}

		producer.close();

	}

}
