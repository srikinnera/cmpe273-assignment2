package edu.sjsu.cmpe.procurement.jobs;

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
import org.fusesource.stomp.jms.message.StompJmsMessage;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
import edu.sjsu.cmpe.procurement.ProcurementService;
import edu.sjsu.cmpe.procurement.config.ProcurementServiceConfiguration;
import edu.sjsu.cmpe.procurement.domain.ProcurementListener;
import edu.sjsu.cmpe.procurement.domain.ProcurementProducer;

/**
 * This job will run at every 5 second.
 */
//@Every("5s")
@Every("300s")
public class ProcurementSchedulerJob extends Job {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void doJob(){
	
    	ProcurementListener proListen = new ProcurementListener();
    	try {
			proListen.readQueue();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	ProcurementProducer proProduce = new ProcurementProducer();
    	ArrayList<String> library = new ArrayList<String>();
    	try {
			library = proProduce.httpGet();
			int n = library.size();
			for(int i = 0 ; i < n ; i++){
				try {
					proProduce.sendBook(library.get(i));
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    	
    	
}
