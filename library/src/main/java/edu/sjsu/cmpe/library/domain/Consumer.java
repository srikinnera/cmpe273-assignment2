package edu.sjsu.cmpe.library.domain;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;

import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;

public class Consumer {
	
    private  final BookRepositoryInterface bookRepository;
	private String queueName;
    private String libraryName;
    private String apolloHost;
    private String apolloPort;
    private String apolloUser;
    private String apolloPassword;
    private String topicName;
       
    public Consumer(BookRepositoryInterface bookRepository, String queueName, String topicName, String libraryName, String apolloHost, String apolloPort, String apolloUser, String apolloPassword){
    	
    	this.bookRepository = bookRepository;
    	this.queueName = queueName;
    	this.libraryName = libraryName;
    	this.apolloHost = apolloHost;
    	this.apolloPort = apolloPort;
    	this.apolloUser = apolloUser;
    	this.apolloPassword = apolloPassword;
    	this.topicName = topicName;
    }
    
    //This method is used to recieve ascynchronously delivered messages
    public void onMessage(Message msg){
    	TextMessage message = (TextMessage) msg;
		try {
			System.out.println("Message recieved " + message.getText());
		} catch (JMSException ex) {
			ex.printStackTrace();
		}
    }
    
    public void listenQueue() throws JMSException, MalformedURLException{
    	
    //	ArrayList<String> receivedBooks = new ArrayList<String>();
    	int port = Integer.parseInt(apolloPort);
    	
    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
		factory.setBrokerURI("tcp://" + apolloHost + ":" + port);

		while (true) {

			Connection connection = factory.createConnection(apolloUser, apolloPassword);

			connection.start();
			Session session = connection.createSession(false,
					Session.AUTO_ACKNOWLEDGE);
			Destination dest = new StompJmsDestination(topicName);

			MessageConsumer consumer = session.createConsumer(dest);
			System.currentTimeMillis();
			 //System.out.println("Waiting for messages.....");
			while (true) {
				Message msg = consumer.receive(500);
				if (msg == null)
					break;
				if (msg instanceof TextMessage) {
					String body = ((TextMessage) msg).getText();
					System.out.println("Received message =  " + body);
					String[] bookDetails = body.split(":",4);
					Long isbn = Long.parseLong(bookDetails[0]);
					System.out.println("isbn " + isbn);
					String title = bookDetails[1].substring(1, bookDetails[1].length()-1);
					System.out.println("title " + title);
					String category = bookDetails[2].substring(1, bookDetails[2].length()-1);
					System.out.println("category " + category);
					String url = bookDetails[3].substring(1,bookDetails[3].length() - 1);
					URL coverimage = new URL(url);
					System.out.println("coverimage " + coverimage);
					Book book = bookRepository.getBookByISBN(isbn);
					if(book != null){
						System.out.println("Book  is in repo");
						if(book.getStatus().equals(Status.lost)){
							book.setStatus(Status.available);
						}
					}else{
						System.out.println("Creating a new book");
						book = new Book();
						book.setIsbn(isbn);
						book.setStatus(Status.available);
						book.setCategory(category);
						book.setTitle(title);
						book.setCoverimage(coverimage);
						bookRepository.saveBookWithIsbn(book, isbn);
						
					}
					
				} else {
					System.out.println("unexpected msg " + msg.getClass());
				}		
					
			}
			connection.close();		
		}
    	
    }

}
