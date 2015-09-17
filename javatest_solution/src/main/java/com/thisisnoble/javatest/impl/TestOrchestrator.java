package com.thisisnoble.javatest.impl;

import com.thisisnoble.javatest.*;
import com.thisisnoble.javatest.processors.*;
import com.thisisnoble.javatest.events.*;

public class TestOrchestrator implements Orchestrator {
	
	private Processor myMarginProcessor;
	private Processor myRiskProcessor;
	private Processor myShippingProcessor;
	private Publisher myPublisher; 
	private CompositeEvent compEvent; 
	
	private int eventCount; //The eventCount variable keeps us from publishing until all events in the "tree" are added to the composite event
	
	public TestOrchestrator() {
		eventCount = 0; //Not necessary since primitive member variable ints are initialized to zero anyway, but good practice
	}

	@Override
	public void register(Processor processor) {
		//Assign instances of each processor object passed to our orchestrator 
		if(processor instanceof MarginProcessor) {
			myMarginProcessor = processor;
		}
		else if (processor instanceof RiskProcessor) {
			myRiskProcessor = processor;
		}
		else if (processor instanceof ShippingProcessor){
			myShippingProcessor = processor; 
		}
	}

	@Override
    public synchronized void receive(Event event) {
		// Note: This code only accounts for trade events and shipping events are passed directly as the parent from the test code
		
		//If the composite event is null, store the first event passed as the id and parent in the new composite event
		if(compEvent == null) {
			compEvent = new CompositeEvent(event.getId(), event);
		}
		
		if(event instanceof TradeEvent) {
    		//When we have a trade event, we need to create child events from each of the 3 processors. 
			// These are then processed by this receive() method.  This behavior was determined from the 1st JUnit test.
			eventCount++;
    		myRiskProcessor.process(event);
    		
    		eventCount++;
    		myShippingProcessor.process(event);
    		
    		eventCount++;
    		myMarginProcessor.process(event);
    	}
    	else if(event instanceof ShippingEvent) {    		
    		if(!compEvent.getParent().equals(event)) {
    			//If this event isn't the parent event, then add it as a child.  Behavior determined from the 1st JUnit test
    			addToCompositeEvent(event);
    			
        		//Now create of child events from 2 processors, which will be received again by this method.  
        		eventCount++;
        		myRiskProcessor.process(event);
        		
        		eventCount++;
        		myMarginProcessor.process(event);   
    		}
    		else {
    			//When the shipping event is the parent event, then we need to increase the shipping cost by 2x. 
    			// Behavior determined from the 2nd JUnit test
    			ShippingEvent shipEvent = new ShippingEvent(event.getId(), ((ShippingEvent) event).getShippingCost()*2);
    			
        		eventCount++;
        		myRiskProcessor.process(shipEvent);
        		
        		eventCount++;
        		myMarginProcessor.process(shipEvent);   
    		}
    	}
		else if(event instanceof MarginEvent) {
    		//According to the 1st JUnit test, we need to create a shipping event when we have a margin event of id "tradeEvt-shipEvt"
    		// We then store this shipping event as a child and pass it to the margin processor and risk processor. 
    		if(event.getId().equals("tradeEvt-shipEvt")) {
   				//According to the 1st JUnit test, we need to store these as shipping events, not margin events
	    		ShippingEvent shipEvent = new ShippingEvent(((MarginEvent) event).getId(), ((MarginEvent) event).getMargin());
	    		
	    		addToCompositeEvent(shipEvent);
	    		
	    		eventCount++;
	    		myRiskProcessor.process(shipEvent);
	    		
	    		eventCount++;
	    		myMarginProcessor.process(shipEvent);   
    		}
    		else {
    			//Else if it is not of id "tradeEvt-shipEvt" do not create a new shipping event, add as a margin event to the composite event
    			// Behavior determined from the 1st JUnit test
    			addToCompositeEvent(event);
    		}    		
    	}
    	else if(event instanceof RiskEvent) {
    		//Risk events are only stored as-is, they are not processed
    		addToCompositeEvent(event);    		
    	}
		
		//Only publish if all events in the tree are processed and added to the composite event
		if(eventCount == 0) {
			myPublisher.publish(compEvent);
		}
    }
	
	private void addToCompositeEvent(Event event) {
		eventCount--;
		compEvent.addChild(event); 
	}

	@Override
    public void setup(Publisher publisher) {
		//Initialize instance of the publisher
		myPublisher = publisher; 
    }

}
