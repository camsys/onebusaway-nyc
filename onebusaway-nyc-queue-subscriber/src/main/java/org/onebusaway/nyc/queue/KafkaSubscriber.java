/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.queue;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.zeromq.ZMQ;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Executors;

public class KafkaSubscriber implements ISubscriber{
	public static final String HOST_KEY = "mq.host";
	public static final String PORT_KEY = "mq.port";
	public static final String TOPIC_KEY = "mq.topic";
	public static final String PBDIR_KEY = "pb.dir";
	public static final String PB_LIMIT_KEY = "pb.limit";
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 9092;
	private static final String DEFAULT_TOPIC = "bhs_queue";
	private static final int DEFAULT_PB_LIMIT = -1;

	protected static KafkaConsumer<String, String> consumer;

	protected static Properties properties = new Properties();

	public void main(String[] args) {

    String host = DEFAULT_HOST;
    if (System.getProperty(HOST_KEY) != null) {
					host = System.getProperty(HOST_KEY);
    }
    int port = DEFAULT_PORT;
    if (System.getProperty(PORT_KEY) != null) {
			try {
				port = Integer.parseInt(System.getProperty(PORT_KEY));
			} catch (NumberFormatException nfe) {
				port = DEFAULT_PORT;
			}
		}
		String topic = DEFAULT_TOPIC;
		//if (System.getProperty(TOPIC_KEY) != null) {
		//	topic = System.getProperty(TOPIC_KEY);
		//}

		String pbdir = null;
		if (System.getProperty(PBDIR_KEY) != null) {
			pbdir = System.getProperty(PBDIR_KEY);
		}

		int pblimit = DEFAULT_PB_LIMIT;
		if (System.getProperty(PB_LIMIT_KEY) != null) {
			try {
				pblimit = Integer.parseInt(System.getProperty(PB_LIMIT_KEY));
			} catch (NumberFormatException nfe) {
				pblimit = DEFAULT_PB_LIMIT;
			}
		}

		String bind = "tcp://" + host + ":" + port;

		if (properties.isEmpty() ||
				properties.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG).isBlank() ||
				properties.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG).isEmpty()) {
			setProperties(bind, topic);
		}


		consumer = new KafkaConsumer<>(properties);
		consumer.subscribe(Arrays.asList(topic));
		System.out.println("listening on " + bind);
		int nprocess = 0;
		while (true) {
			// Read envelope with address
			ConsumerRecords<String, String> records =
					consumer.poll(Duration.ofMillis(100));
			for (ConsumerRecord<String, String> record : records) {
				System.out.println("Key: " + record.key() + ", Value: " + record.value());
				System.out.println("Partition: " + record.partition() + ", Offset:" + record.offset());
				String address = record.key();
				// Read message contents
				byte[] contents = record.value().getBytes();
				if (pbdir == null)
					process(address, new String(contents));
				else
					processToDir(pbdir, address, contents);

				nprocess++;
				if (pblimit > 0 && nprocess >= pblimit)
					break;
			}
		}
	}
	private static void process(String address, String contents) {
		System.out.println(address + " : " + toDate(new Date()) +  " : " + contents);
	}

	private static void processToDir(String dir, String address, byte[] contents) {
		writeToFile(dir + "/" + toDate(new Date()) + ".pb", contents);
	}

	protected static void writeToFile(String filename, byte[] contents) {
		File file = new File(filename);
		if (file.getParentFile() != null)
			file.getParentFile().mkdirs();
		try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
			dos.write(contents);
			dos.close();
		} catch(IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private static String toDate(Date date) {
		if (date != null) {
			return dateFormatter.format(date);
		}
		return null;
	}

	private static void setProperties(String bind, String queueName){

		try {

			properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bind);
			properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bind);
			properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
			properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
			properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
			properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
			properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, queueName);
			properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		}catch(Exception e){
			System.out.println("exception = " + e + "");
		}

	}

}
