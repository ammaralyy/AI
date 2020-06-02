import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Main {

    public static void main(String[] args) {

        jade.core.Runtime r = jade.core.Runtime.instance();
        Profile profile = new ProfileImpl("localhost", 5000,"Ghada");
        ContainerController container = r.createMainContainer(profile);

        AgentController Agent1;
        AgentController Agent2;

        try {
            Agent2 = container.createNewAgent("SellerAgent", "SellerAgent", null);
            Agent2.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

        try {
            Agent1 = container.createNewAgent("BuyerAgent", "BuyerAgent", null);
            Agent1.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }


    }
}
