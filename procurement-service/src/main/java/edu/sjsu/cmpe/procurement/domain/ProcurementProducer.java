package edu.sjsu.cmpe.procurement.domain;
import java.util.ArrayList;
import java.util.Arrays;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
import edu.sjsu.cmpe.procurement.ProcurementService;



public class ProcurementProducer {
	
	private String user;
	private String password;
	private String host;
	private int port;
	
	public ProcurementProducer(){
		
		this.user = "admin";
		this.password = "password";;
		this.host = "54.215.133.131";
		this.port = Integer.parseInt("61613");
				
	}
	
	public ArrayList<String> httpGet() throws JSONException{
		
			ArrayList<String> library = new ArrayList<String>();
			Client client = Client.create();
			WebResource webResource=client.resource("http://54.215.133.131:9000/orders/32304");
			ClientResponse response = webResource.accept("application/json")
					.get(ClientResponse.class);
			if (response.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ response.getStatus());
			}
			String output = response.getEntity(String.class);
			System.out.println("Output from Server .... \n");
			System.out.println(output);
			//System.out.println("\n\n\n");
			JSONObject obj=new JSONObject(output);
			JSONArray bookList=obj.getJSONArray("shipped_books");
			int n=bookList.length();
			
			for(int i=0;i<n;i++)
			{
				
				JSONObject getbooks=bookList.getJSONObject(i);
			/*	System.out.println("isbn is "+getbooks.getLong("isbn"));
				System.out.println("title is "+getbooks.getString("title"));
				System.out.println("category is "+getbooks.getString("category"));
				System.out.println("coverimage is "+getbooks.getString("coverimage"));*/
				
				String book = "" + getbooks.getLong("isbn") + ":\"" + getbooks.getString("title") + "\":\"" + getbooks.getString("category") + "\":\"" + getbooks.getString("coverimage") + "\"" ;
				library.add(book);
				
			 }  
			return library;
	}
	
	public void sendBook(String book) throws JMSException{
		
		String destination_a = "/topic/32304.book.all";
		String destination_b="/topic/32304.book.computer";

		StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
		factory.setBrokerURI("tcp://" + host + ":" + port);
		
		Connection connection = factory.createConnection(user, password);
    	connection.start();
    	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    	Destination dest_a = new StompJmsDestination(destination_a);
    	Destination dest_b = new StompJmsDestination(destination_b);
    	MessageProducer producer_a = session.createProducer(dest_a);
    	MessageProducer producer_b = session.createProducer(dest_b);
    	producer_a.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    	producer_b.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    	String data = book;
    	TextMessage msg_a = session.createTextMessage(data);
    	msg_a.setLongProperty("id", System.currentTimeMillis());
    	producer_a.send(msg_a);
    	System.out.println("Message sent to a");
    	System.out.println(msg_a);
    	
    	if(book.split(":")[2].equals("\"computer\"")){
    		TextMessage msg_b = session.createTextMessage(data);
        	msg_b.setLongProperty("id", System.currentTimeMillis());
        	producer_b.send(msg_b);
        	System.out.println("Message sent to b");
        	System.out.println(msg_b);
    	}

    	/**
    	 * Notify all Listeners to shut down. if you don't signal them, they
    	 * will be running forever.
    	 */
    	//producer.send(session.createTextMessage("SHUTDOWN"));
    	connection.close();
		
	    }
		
	

}
