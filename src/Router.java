//Work needed
import java.util.*;

@SuppressWarnings("Duplicates")
public class Router {
    private int routerId;
    private int numberOfInterfaces;
    private ArrayList<IPAddress> interfaceAddresses;//list of IP address of all interfaces of the router
    private ArrayList<RoutingTableEntry> routingTable;//used to implement DVR
    private ArrayList<Integer> neighborRouterIDs;//Contains both "UP" and "DOWN" state routers
    private Boolean state;//true represents "UP" state and false is for "DOWN" state
    private Map<Integer, IPAddress> gatewayIDtoIP;
    public Router() {
        interfaceAddresses = new ArrayList<>();
        routingTable = new ArrayList<>();
        neighborRouterIDs = new ArrayList<>();

        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p < Constants.ROUTER_ON_PROBABILITY) state = true;
        else state = false;

        numberOfInterfaces = 0;
    }

    public Router(int routerId, ArrayList<Integer> neighborRouters, ArrayList<IPAddress> interfaceAddresses, Map<Integer, IPAddress> gatewayIDtoIP) {
        this.routerId = routerId;
        this.interfaceAddresses = interfaceAddresses;
        this.neighborRouterIDs = neighborRouters;
        this.gatewayIDtoIP = gatewayIDtoIP;
        routingTable = new ArrayList<>();



        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p < Constants.ROUTER_ON_PROBABILITY) state = true;
        else state = false;

        numberOfInterfaces = interfaceAddresses.size();
    }

    @Override
    public String toString() {
        String string = "";
        string += "Router ID: " + routerId + "\n" + "Interfaces: \n";
        for (int i = 0; i < numberOfInterfaces; i++) {
            string += interfaceAddresses.get(i).getString() + "\t";
        }
        string += "\n" + "Neighbors: \n";
        for(int i = 0; i < neighborRouterIDs.size(); i++) {
            string += neighborRouterIDs.get(i) + "\t";
        }
        return string;
    }



    /**
     * Initialize the distance(hop count) for each router.
     * for itself, distance=0; for any connected router with state=true, distance=1; otherwise distance=Constants.INFTY;
     */
    public void initiateRoutingTable() {
        for(Router router: NetworkLayerServer.routers)
        {
            int rID = router.getRouterId();

            if(rID == this.routerId)
            {
                routingTable.add(new RoutingTableEntry(rID,0,this.routerId));
            }

            else if(neighborRouterIDs.contains(rID) && router.getState())
            {
                routingTable.add(new RoutingTableEntry(rID,1,rID));
            }

            else
            {
                routingTable.add(new RoutingTableEntry(rID,Constants.INFINITY,-1));
            }

        }

    }

    /**
     * Delete all the routingTableEntry
     */
    public void clearRoutingTable() {
        this.routingTable.clear();
    }

    /**
     * Update the routing table for this router using the entries of Router neighbor
     * @param neighbor
     */
    public boolean updateRoutingTable(Router neighbor) {
        boolean isChanged = false;

        int neighborID = neighbor.getRouterId();
        double selfToNeighborDistance = getIdtoRTE(this.routingTable,neighborID).getDistance();

        for(RoutingTableEntry RTE : this.routingTable)
        {
            int rID = RTE.getRouterId();

            //Current Routing Table Update
            int curNexthopId = RTE.getGatewayRouterId();
            if(curNexthopId != -1) {
                Router curNexthop = NetworkLayerServer.getRouterFromID(RTE.getGatewayRouterId());
                double selfToNextDistance = getIdtoRTE(this.routingTable, curNexthop.getRouterId()).getDistance();
                double nexthopTodestDistance = curNexthop.getState() ? getIdtoRTE(curNexthop.getRoutingTable(), rID).getDistance() : Constants.INFINITY;

                double newDistance = selfToNextDistance + nexthopTodestDistance;
                newDistance = newDistance >= Constants.INFINITY ? Constants.INFINITY : newDistance;
                double oldDistance = RTE.getDistance();

                if (newDistance != oldDistance) {
                    RTE.setDistance(newDistance);
                    if (newDistance == Constants.INFINITY) RTE.setGatewayRouterId(-1);

                    isChanged = true;
                }
            }


            //Compare with neighbor Routing Table
//            System.out.println("Rid: "+rID+"\tSelf to N: " + selfToNeighborDistance + "\tSelf RT: " + selfRTdistance +  "\tN Rt: " + neighborRTdistance);
            double currentRTdistance = RTE.getDistance();
            double neighborRTdistance = neighbor.getState() ? getIdtoRTE(neighbor.getRoutingTable(),rID).getDistance() : Constants.INFINITY;

            double viaNeighborDistance = selfToNeighborDistance+neighborRTdistance;
            viaNeighborDistance = viaNeighborDistance>= Constants.INFINITY ? Constants.INFINITY : viaNeighborDistance;

            if(viaNeighborDistance < currentRTdistance)
            {
                RTE.setDistance(viaNeighborDistance);
                if(viaNeighborDistance == Constants.INFINITY) RTE.setGatewayRouterId(-1);
                else RTE.setGatewayRouterId(neighborID);

                isChanged = true;
            }
        }
        return isChanged;
    }

    public boolean sfupdateRoutingTable(Router neighbor) {
        boolean isChanged = false;

        int neighborID = neighbor.getRouterId();
        double selfToNeighborDistance = getIdtoRTE(this.routingTable,neighborID).getDistance();

        for(RoutingTableEntry RTE : this.routingTable)
        {
            int rID = RTE.getRouterId();

            //Current Routing Table Update
            int curNexthopId = RTE.getGatewayRouterId();
            if(curNexthopId != -1) {
                Router curNexthop = NetworkLayerServer.getRouterFromID(RTE.getGatewayRouterId());
                double selfToNextDistance = getIdtoRTE(this.routingTable, curNexthop.getRouterId()).getDistance();
                double nexthopTodestDistance = curNexthop.getState() ? getIdtoRTE(curNexthop.getRoutingTable(), rID).getDistance() : Constants.INFINITY;

                double newDistance = selfToNextDistance + nexthopTodestDistance;
                newDistance = newDistance >= Constants.INFINITY ? Constants.INFINITY : newDistance;
                double oldDistance = RTE.getDistance();

                if (newDistance != oldDistance) {
                    RTE.setDistance(newDistance);
                    if (newDistance == Constants.INFINITY) RTE.setGatewayRouterId(-1);

                    isChanged = true;
                }
            }


            //Compare with neighbor Routing Table
//            System.out.println("Rid: "+rID+"\tSelf to N: " + selfToNeighborDistance + "\tSelf RT: " + selfRTdistance +  "\tN Rt: " + neighborRTdistance);
            double currentRTdistance = RTE.getDistance();
            double neighborRTdistance = neighbor.getState() ? getIdtoRTE(neighbor.getRoutingTable(),rID).getDistance() : Constants.INFINITY;

            double viaNeighborDistance = selfToNeighborDistance + neighborRTdistance;
            viaNeighborDistance = viaNeighborDistance>= Constants.INFINITY ? Constants.INFINITY : viaNeighborDistance;

            int selfToDestNextHop = getIdtoRTE(this.getRoutingTable(),rID).getGatewayRouterId(); //current next hop
            int neighborToDestNextHop = neighbor.getState() ? getIdtoRTE(neighbor.getRoutingTable(),rID).getGatewayRouterId() : -1;

            if((selfToDestNextHop == neighborID && currentRTdistance != viaNeighborDistance)
                    || (viaNeighborDistance < currentRTdistance && this.routerId != neighborToDestNextHop))
            {
                RTE.setDistance(viaNeighborDistance);
                if(viaNeighborDistance == Constants.INFINITY) RTE.setGatewayRouterId(-1);
                else RTE.setGatewayRouterId(neighborID);

                isChanged = true;
            }
        }
        return isChanged;
    }

    /**
     * If the state was up, down it; if state was down, up it
     */
    public void revertState() {
        state = !state;
        if(state) { initiateRoutingTable(); }
        else { clearRoutingTable(); }
    }

    public int getRouterId() {
        return routerId;
    }

    public void setRouterId(int routerId) {
        this.routerId = routerId;
    }

    public int getNumberOfInterfaces() {
        return numberOfInterfaces;
    }

    public void setNumberOfInterfaces(int numberOfInterfaces) {
        this.numberOfInterfaces = numberOfInterfaces;
    }

    public ArrayList<IPAddress> getInterfaceAddresses() {
        return interfaceAddresses;
    }

    public void setInterfaceAddresses(ArrayList<IPAddress> interfaceAddresses) {
        this.interfaceAddresses = interfaceAddresses;
        numberOfInterfaces = interfaceAddresses.size();
    }

    public ArrayList<RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }

    public void addRoutingTableEntry(RoutingTableEntry entry) {
        this.routingTable.add(entry);
    }

    public ArrayList<Integer> getNeighborRouterIDs() {
        return neighborRouterIDs;
    }

    public void setNeighborRouterIDs(ArrayList<Integer> neighborRouterIDs) { this.neighborRouterIDs = neighborRouterIDs; }

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }

    public Map<Integer, IPAddress> getGatewayIDtoIP() { return gatewayIDtoIP; }

    public void printRoutingTable() {
        System.out.println("Router " + routerId + " State: " + this.state);
        System.out.println("DestID\tDistance\tNexthop");
        for (RoutingTableEntry routingTableEntry : routingTable) {
            System.out.println(routingTableEntry.getRouterId() + "\t\t" + String.format("%.2f", routingTableEntry.getDistance()) + "\t\t" + routingTableEntry.getGatewayRouterId());
        }
        System.out.println("-----------------------");
    }
    public String strRoutingTable() {
        String string = "Router " + routerId + "\tState: " + this.state + "\n";
        string += "DestID\tDistance\tNexthop\n";
        for (RoutingTableEntry routingTableEntry : routingTable) {
            string += routingTableEntry.getRouterId() + "\t\t" + String.format("%.2f", routingTableEntry.getDistance()) + "\t\t" + routingTableEntry.getGatewayRouterId() + "\n";
        }

        string += "-----------------------\n";
        return string;
    }

    public RoutingTableEntry getIdtoRTE(ArrayList<RoutingTableEntry>table, int id)
    {
        RoutingTableEntry entry = null;
        for(RoutingTableEntry rte: table)
        {
            if(rte.getRouterId() == id)
            {
                entry = rte;
                break;
            }
        }
        return entry;
    }

}
