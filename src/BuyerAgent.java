import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BuyerAgent extends Agent {

	private String item;

	private AID[] sellerAgents;

	private BuyerGui myGui;


	// Put agent initializations here
	protected void setup() {

		myGui = new BuyerGui(this);
		myGui.showGui();

	}

	protected void action(String item){


		this.item = item;
		if (this.item != null ) {


			System.out.println("item is "+ this.item);

			addBehaviour(new TickerBehaviour(this, 60000) {
				protected void onTick() {
					System.out.println("Trying to buy "+ BuyerAgent.this.item);

					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("book-selling");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						System.out.println("Found the following seller agents:");
						sellerAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							sellerAgents[i] = result[i].getName();
							System.out.println(sellerAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}
					myAgent.addBehaviour(new RequestPerformer());
				}
			} );
		}
		else {
			System.out.println("No target item title specified");
			doDelete();
		}
	}

	protected void takeDown() {

		System.out.println("Buyer-agent "+getAID().getName()+" terminating.");
	}


	private class RequestPerformer extends Behaviour {

		private AID bestSeller;
		private int bestPrice;

		private int repliesCnt = 0;
		private MessageTemplate mt;

		private int step = 0;

		public void action() {
			switch (step) {
			case 0:


				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.length; ++i) {
					cfp.addReceiver(sellerAgents[i]);
				} 
				cfp.setContent(item);
				cfp.setConversationId("book-trade");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer 
						int price = Integer.parseInt(reply.getContent());
						if (bestSeller == null || price < bestPrice) {
							// This is the best offer at present
							bestPrice = price;
							bestSeller = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= sellerAgents.length) {
						step = 2;
					}
				}
				else {
					block();
				}
				break;
			case 2:
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeller);
				order.setContent(item);
				order.setConversationId("book-trade");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:      
				reply = myAgent.receive(mt);
				if (reply != null) {
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can terminate
						System.out.println(item+" successfully purchased from agent "+reply.getSender().getName());
						System.out.println("Price = "+bestPrice);
						myAgent.doDelete();
					}
					else {
						System.out.println("Attempt failed: requested book already sold.");
					}

					step = 4;
				}
				else {
					block();
				}
				break;
			}        
		}

		public boolean done() {
			if (step == 2 && bestSeller == null) {
				System.out.println("Attempt failed: "+item+" not available for sale");
			}
			return ((step == 2 && bestSeller == null) || step == 4);
		}
	}
}
