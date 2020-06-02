import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class BuyerGui extends JFrame {



    private BuyerAgent buyerAgent;

    private JTextField titleField;

    String title;

    public BuyerGui(BuyerAgent b){

        super(b.getLocalName());

        buyerAgent = b;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(4, 4));
        p.add(new JLabel("item:"));
        titleField = new JTextField(25);
        p.add(titleField);

        getContentPane().add(p, BorderLayout.CENTER);

        JButton addButton = new JButton("search");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    title = titleField.getText().trim();
                    buyerAgent.action(title);
                } catch (Exception e) {
                }
            }
        });
        p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                buyerAgent.doDelete();
            }
        });

        setResizable(false);
    }

    public void showGui() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.setVisible(true);
    }

}

class RequestPerformer extends Behaviour {

   private String item;

   private AID[] sellerAgents;

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
               cfp.setConversationId("item-trade");
               cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
               myAgent.send(cfp);
               // Prepare the template to get proposals
               mt = MessageTemplate.and(MessageTemplate.MatchConversationId("item-trade"),
                       MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
               step = 1;
               break;
           case 1:
               // Receive all proposals/refusals from seller agents
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
                       // We received all replies
                       step = 2;
                   }
               }
               else {
                   block();
               }
               break;
           case 2:
               // Send the purchase order to the seller that provided the best offer
               ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
               order.addReceiver(bestSeller);
               order.setContent(item);
               order.setConversationId("item-trade");
               order.setReplyWith("order"+System.currentTimeMillis());
               myAgent.send(order);
               // Prepare the template to get the purchase order reply
               mt = MessageTemplate.and(MessageTemplate.MatchConversationId("item-trade"),
                       MessageTemplate.MatchInReplyTo(order.getReplyWith()));
               step = 3;
               break;
           case 3:
               // Receive the purchase order reply
               reply = myAgent.receive(mt);
               if (reply != null) {
                   // Purchase order reply received
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
           System.out.println("failed: "+item+" not available");
       }
       return ((step == 2 && bestSeller == null) || step == 4);
   }
}
