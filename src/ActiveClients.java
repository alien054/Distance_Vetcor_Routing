import java.util.ArrayList;

public class ActiveClients {
    ArrayList<EndDevice> activeClientList;

    public ActiveClients()
    {
        activeClientList = new ArrayList<>();
    }

    public void add(EndDevice e)
    {
        activeClientList.add(e);
    }

    public void remove(EndDevice e)
    {
        activeClientList.remove(e);
    }

    public ArrayList<EndDevice> getActiveClientList()
    {
        return this.activeClientList;
    }

}
