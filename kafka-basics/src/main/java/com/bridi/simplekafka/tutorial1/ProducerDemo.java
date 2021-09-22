package com.bridi.simplekafka.tutorial1;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class ProducerDemo {

	public static void main(String[] args) {

		// Create producer properties
		
		Properties properties = new Properties();
		
		String bootstrapServers = "127.0.0.1:9092";
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		
		// Create the producer
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
		
		
		//Kafka needs to run! (zookeeeper too!!)
		ProducerRecord<String, String> record = new ProducerRecord<String, String>("first_topic", "hello world!");
		
		// SendData
		producer.send(record);
		
		producer.flush();
		producer.close();
		
	}
	
}
