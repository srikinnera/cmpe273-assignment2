package edu.sjsu.cmpe.library.domain;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;

public class Publisher {

		private String queueName;
	    private String libraryName;
	    private String apolloHost;
	    private String apolloPort;
	    private String apolloUser;
	    private String apolloPassword;
	    private String topicName;
	    
	    public Publisher(String queueName, String topicName, String libraryName, String apolloHost, String apolloPort, String apolloUser, String apolloPassword){
	    	
	    	this.queueName = queueName;
	    	this.libraryName = libraryName;
	    	this.apolloHost = apolloHost;
	    	this.apolloPort = apolloPort;
	    	this.apolloUser = apolloUser;
	    	this.apolloPassword = apolloPassword;
	    	this.topicName = topicName;
	    }
	    
	    public void sendOrder(long isbn)throws JMSException{
	    	String user = this.apolloUser;
	    	String password = this.apolloPassword;
	    	String host = this.apolloHost;
	    	int port = Integer.parseInt(this.apolloPort);
	    	String destination = this.queueName;

	    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	    	factory.setBrokerURI("tcp://" + host + ":" + port);

	    	Connection connection = factory.createConnection(user, password);
	    	connection.start();
	    	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    	Destination dest = new StompJmsDestination(destination);
	    	MessageProducer producer = session.createProducer(dest);
	    	producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

	    	String data = libraryName+":"+isbn;
	    	TextMessage msg = session.createTextMessage(data);
	    	//System.out.println(msg);
	    	msg.setLongProperty("id", System.currentTimeMillis());
	    	producer.send(msg);

	    	/**
	    	 * Notify all Listeners to shut down. if you don't signal them, they
	    	 * will be running forever.
	    	 */
	    	//producer.send(session.createTextMessage("SHUTDOWN"));
	    	connection.close();
	    }
			

	}



