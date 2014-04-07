package edu.sjsu.cmpe.procurement.jobs;

import java.util.Map;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
import edu.sjsu.cmpe.procurement.ProcurementService;
import edu.sjsu.cmpe.procurement.domain.Book;

/**
 * This job will run at every 5 second.
 */

@Every("5mn")
public class ProcurementSchedulerJob extends Job {
	private final Logger log = LoggerFactory.getLogger(getClass());

	public void doJob() {
		String body = "{\"id\":\"32304\",\"order_book_isbns\" : [";
		Message msg = null;
		boolean hasMsg = false;
		try {
			// Client client=Client.create();

			// WebResource webResource = client
			// .resource("http://54.215.133.131:9000/orders");
			// String body = "{\"id\":\"32304\",\"order_book_isbns\":[1]}";

			long waitUntil = 5000;
			while (true) {
				log.info("before consuming");
				msg = ProcurementService.consumer.receive(waitUntil);
				log.info("after consuming = {}", msg);
				if (msg instanceof StompJmsMessage) {
					StompJmsMessage smsg = ((StompJmsMessage) msg);
					log.info("after parsing msg = {}", smsg.getFrame()
							.contentAsString());
					body += smsg.getFrame().contentAsString().substring(10)
							+ ",";
					hasMsg = true;
					
				} else if (msg == null) {
					System.out.println("No Books in the Queue");
					System.out
							.println("No new messages. Exiting due to timeout - "
									+ waitUntil / 1000 + " sec");
					//hasMsg = false;
					break;
				} else {
					System.out.println("Unexpected message type: "
							+ msg.getClass());
				}
			}
			System.out.println(hasMsg);
			System.out.println("body 1 " +body);
			if (hasMsg) {
				//// System.out.println(input);
				body += "]}";
				System.out.println("body 2 "+body);
				int position = body.lastIndexOf(",");
				body = replaceCharAt(body, position, "");
				System.out.println("body 3 "+body);
			/*	ClientResponse strResponse = Client.create()
							.resource("http://54.215.210.214:9000/orders")
							.type("application/json")
							.post(ClientResponse.class, body);*/
				try {
					System.out.println("inside post method");
					Client client = Client.create();
					WebResource webResource = client.resource("http://54.215.133.131:9000/orders");
					ClientResponse response = webResource.type("application/json")
							.post(ClientResponse.class, body);
					if (response.getStatus() != 200) {
						throw new RuntimeException("Failed : HTTP error code : "
								+ response.getStatus());
					}
					System.out.println("Output from Server .... \n");
					String output = response.getEntity(String.class);
					System.out.println(output);
				} catch (Exception e) {
					e.printStackTrace();
				}
				

				/*
				 * ClientResponse
				 * strResponse=ProcurementService.jerseyClient.create
				 * ().resource( "http://54.215.210.214:9000/orders"
				 * ).type("application/json").post(ClientResponse.class,body);
				 */

				// String strResponse =
				// ProcurementService.jerseyClient.resource(
				// "http://ip.jsontest.com/").get(String.class);
			/*	if (strResponse.getStatus() == 200) {
					log.debug("Response from Publisher:{}",
							strResponse.getEntity(String.class));
				} else {
					log.debug("HTTP Error Code returned as:{}",
							strResponse.getStatus());
				}*/

			} // end hasMsg if

			doGetFromPublisher();

		} catch (JMSException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		/*
		 * String strResponse1 = ProcurementService.jerseyClient.resource(
		 * "http://ip.jsontest.com/").get(String.class);
		 * log.debug("Response from jsontest.com: {}", strResponse1);
		 */
	}

	private void doGetFromPublisher() throws JMSException {
		
		System.out.println("I am here");
		Map<String, Book[]> collector;
		ClientResponse strResponse = ProcurementService.jerseyClient.create()
				.resource("http://54.215.133.131:9000/orders/32304")
				.accept("application/json").type("application/json")
				.get(ClientResponse.class);
		System.out.println("Response is "+strResponse);
		// log.debug("Response from Get Publisher:{}",
		// strResponse.getEntity(String.class));
		// TODO Auto-generated method stub
		collector = strResponse
				.getEntity(new GenericType<Map<String, Book[]>>() {
				});
		Book[] myCollection = collector.get("shipped_books");
		int size = myCollection.length;

		// get connection
		StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
		factory.setBrokerURI("tcp://" + "54.215.133.131" + ":" + 61613);
		Connection connection = factory.createConnection("admin", "password");
		connection.start();
		Session session = connection.createSession(false,
				Session.AUTO_ACKNOWLEDGE);

		// StompJmsMessage msg = (StompJmsMessage) session
		// .createTextMessage(strResponse.getEntity(String.class));
		// System.out.println("Publisher Sending the books to Queue"+strResponse.getEntity(String.class));
		// System.out.println("Msg is"+msg);
		for (int i = 0; i < size; i++) {
			ProcurementService.topicName = "/topic/32304.book."
					+ myCollection[i].getCategory();
			Destination topicdestination = new StompJmsDestination(
					ProcurementService.topicName);
			ProcurementService.producer = session
					.createProducer(topicdestination);
			String data = myCollection[i].getIsbn() + ":" + "\""
					+ myCollection[i].getTitle() + "\":\""
					+ myCollection[i].getCategory() + "\":\""
					+ myCollection[i].getCoverimage() + "\"";
			StompJmsMessage msg = (StompJmsMessage) session
					.createTextMessage(data);
			msg.setLongProperty("id", System.currentTimeMillis());

			System.out.println("Books sending to Library:" + data);
			ProcurementService.producer.send(msg);
			ProcurementService.producer
					.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		}

	}

	public static String replaceCharAt(String s, int pos, String c) {
		return s.substring(0, pos) + c + s.substring(pos + 1);
	}

}
