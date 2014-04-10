package edu.sjsu.cmpe.procurement.domain;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ProcurementListener {
	
	String user;
	String password;
	String host;
	int port;
	String queue;
	
	public ProcurementListener(){
		
		this.user = "admin";
		this.password = "password";;
		this.host = "54.215.133.131";
		this.port = Integer.parseInt("61613");
		this.queue = "/queue/32304.book.orders";
	}
	
	
	public void readQueue() throws JMSException{
		
		String destination = queue;
		String tempIsbnList = "";
		
		StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
		factory.setBrokerURI("tcp://" + host + ":" + port);

		Connection connection = factory.createConnection(user, password);
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination dest = new StompJmsDestination(destination);

		MessageConsumer consumer = session.createConsumer(dest);
		System.out.println("Waiting for messages from " + queue + "...");
		
		long waitUntil = 5000;
		while(true) {
		    Message msg = consumer.receive(waitUntil);
		 //   System.out.println("I am here " + msg);
		    if( msg instanceof  TextMessage ) {
			String body = ((TextMessage) msg).getText();
			String isbn = body.split(":")[1];
			if(tempIsbnList.equals("")){
				tempIsbnList = isbn;	
			}else{
				tempIsbnList = tempIsbnList+","+isbn;
			}
		//	System.out.println("ISBN LIST : " + tempIsbnList);
			
		/*	if( "SHUTDOWN".equals(body)) {
			    break;
			}*/
			System.out.println("Received message = " + body);
			
			

		    } else if (msg instanceof StompJmsMessage) {
			StompJmsMessage smsg = ((StompJmsMessage) msg);
			String body = smsg.getFrame().contentAsString();
			if ("SHUTDOWN".equals(body)) {
			    break;
			}
			System.out.println("Received message = " + body);

		    }else if(msg == null){
		    	System.out.println("No new messages. Exiting due to timeout - " + waitUntil / 1000 + " sec");
		    	break;
		    }else {
			System.out.println("Unexpected message type: "+msg.getClass());
		    }
		}
		connection.close();
		if(!tempIsbnList.equals("")){
			httpPost(tempIsbnList);
		}
	
	}
	
	public void httpPost(String isbns){
		
		String request = "\"id\":\"32304\",\"order_book_isbns\" : [" + isbns + "]";
	//	System.out.println(request);
		try {
			System.out.println("inside post method");
			Client client = Client.create();
			WebResource webResource = client
					.resource("http://54.215.133.131:9000/orders");
			String input="{" +request +"}";
			System.out.println(input);
			ClientResponse response = webResource.type("application/json")
					.post(ClientResponse.class, input);
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
		
	}
	
}
