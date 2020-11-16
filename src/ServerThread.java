

import java.util.ArrayList;
import java.util.Random;

public class ServerThread implements Runnable {

    NetworkUtility networkUtility;
    EndDevice endDevice;
    ArrayList<EndDevice> acl;
    Thread t;
    Boolean ack;

    ServerThread(NetworkUtility networkUtility, EndDevice endDevice) {
        this.networkUtility = networkUtility;
        this.endDevice = endDevice;
        this.acl = new ArrayList<>();
        this.ack = false;
        System.out.println("Server Ready for client " + NetworkLayerServer.clientCount);
        System.out.println("--------------------------------");
//        NetworkLayerServer.clientCount++;
        t = new Thread(this);
        t.start();

    }

    @Override
    public void run() {
        /**
         * Synchronize actions with client.
         */
        networkUtility.write(this.endDevice);

        while (true)
        {
            int option = (Integer) networkUtility.read();
            if(option == 1)
            {
                //Sending Active Client list (without Self)
                StringBuilder acl = new StringBuilder();
                int i=0;
                int size = NetworkLayerServer.activeClientList.size();
                for (IPAddress ip:  NetworkLayerServer.activeClientList)
                {
                    if(!(ip==this.endDevice.getIpAddress()))
                    {
                        acl.append(ip.getString());
                        if(i!= size-1)  acl.append(",");
                    }
                    i++;
                }

                networkUtility.write((String) acl.toString());
                //Active Client list Sent

                //Receive Packet
                for (i=0;i<Constants.totalPacketsToSent;i++)
                {
                    Packet p = null;
                    String response = "";
                    while (p == null)
                    {
                        p = (Packet) networkUtility.read();
                    }
                    System.out.println(p.getMessage());
                    ack = deliverPacket(p);
                    if(!ack)
                    {
                        response += "dropped packet,0";
                        networkUtility.write(response);
                        System.out.println("Packet Dropped");
                    }
                    else
                    {
                        response += "Packet delivered Successfully," + p.hopcount;
                        networkUtility.write(response);
                        System.out.println("Packet Sent Successfully");
                    }
                }
            }
            else
            {
                NetworkLayerServer.activeClientList.remove(endDevice.getIpAddress());
                networkUtility.closeConnection();
                break;
            }

        }
        /*
        Tasks:
        1. Upon receiving a packet and recipient, call deliverPacket(packet)
        2. If the packet contains "SHOW_ROUTE" request, then fetch the required information
                and send back to client
        3. Either send acknowledgement with number of hops or send failure message back to client
        */
    }


    public Boolean deliverPacket(Packet p) {

        /*
        1. Find the router s which has an interface
                such that the interface and source end device have same network address.
        2. Find the router d which has an interface
                such that the interface and destination end device have same network address.
        3. Implement forwarding, i.e., s forwards to its gateway router x considering d as the destination.
                similarly, x forwards to the next gateway router y considering d as the destination,
                and eventually the packet reaches to destination router d.

            3(a) If, while forwarding, any gateway x, found from routingTable of router r is in down state[x.state==FALSE]
                    (i) Drop packet
                    (ii) Update the entry with distance Constants.INFTY
                    (iii) Block NetworkLayerServer.stateChanger.t
                    (iv) Apply DVR starting from router r.
                    (v) Resume NetworkLayerServer.stateChanger.t

            3(b) If, while forwarding, a router x receives the packet from router y,
                    but routingTableEntry shows Constants.INFTY distance from x to y,
                    (i) Update the entry with distance 1
                    (ii) Block NetworkLayerServer.stateChanger.t
                    (iii) Apply DVR starting from router x.
                    (iv) Resume NetworkLayerServer.stateChanger.t

        4. If 3(a) occurs at any stage, packet will be dropped,
            otherwise successfully sent to the destination router
        */
        IPAddress sourceIP = p.getSourceIP();
        IPAddress destIP   = p.getDestinationIP();
        System.out.println("Source IP: "+sourceIP+"\tDest IP: "+destIP);

        //Need to change
        Router sourceRouter = NetworkLayerServer.getRouterFromID(NetworkLayerServer.deviceIDtoRouterID.get(endDevice.getDeviceID()));
        Router destRouter = null;

        String response = "";
        String routingPath = "";
        boolean isDelivered = false;

        Short[] destNetworkAdd = destIP.getBytes();
        boolean flag = false;
        for(Router router: NetworkLayerServer.routers)
        {
            for(IPAddress interfaces: router.getInterfaceAddresses())
            {
                Short[] routerNetworkAdd = interfaces.getBytes();

                if(destNetworkAdd[0].equals(routerNetworkAdd[0]) && destNetworkAdd[1].equals(routerNetworkAdd[1]) && destNetworkAdd[2].equals(routerNetworkAdd[2]))
                {
                    destRouter = router;
                    flag = true;
                    break;
                }
            }

            if(flag) break;
        }

        assert destRouter != null;
        System.out.println("Src Rt: " + sourceRouter.getRouterId() +"\tState: "+sourceRouter.getState()+"\tDest Rt: " + destRouter.getRouterId()+"\tState: "+destRouter.getState());

        Router senderHop = sourceRouter;
        while(true)
        {
            //Sender Router is Down at this instance ---> Packet Dropped
            if(!senderHop.getState())
            {
                System.out.println(senderHop.getRouterId() + " is Down");
                isDelivered = false;
                break;
//                return false;
            }

            //Generating response. This is a corner case where the router is the initial source router
            if(senderHop.getRouterId() == sourceRouter.getRouterId() && p.getSpecialMessage().equalsIgnoreCase("SHOW_ROUTE"))
                routingPath += senderHop.getRouterId();

            //Try to access the nextHop
            RoutingTableEntry entry = senderHop.getIdtoRTE(senderHop.getRoutingTable(),destRouter.getRouterId());

            //Current Sender doesn't acknowledge the destination router ---> Packet Dropped
            if(entry.getGatewayRouterId() == -1)
            {
                System.out.println("-1");
                System.out.println("Sender doesn't acknowledge Destination");
                isDelivered = false;
                break;
//                return false;
            }

            //nextHop Found!!!
            Router nextHop = NetworkLayerServer.getRouterFromID(entry.getGatewayRouterId());
            System.out.println("next" + nextHop.getRouterId()+"\tState: "+nextHop.getState());

            //nextHop is Down ---> Drop Packet & Update DVR
            if(!nextHop.getState())
            {
                //Lock stateChanger
                RouterStateChanger.islocked = true;
                sleep(500);

                //Sanity Check!! while selecting the nextHop stateChanger might corrupt the senderHop
                if(senderHop.getState())
                {
                    RoutingTableEntry routingTableEntry = senderHop.getIdtoRTE(senderHop.getRoutingTable(),nextHop.getRouterId());
                    routingTableEntry.setDistance(Constants.INFINITY);
                    routingTableEntry.setGatewayRouterId(-1);

                    System.out.println("DVR is running");
                    NetworkLayerServer.applyDVR(senderHop.getRouterId());
                }

                //Unlock stateChanger
                RouterStateChanger.islocked = false;
                try {
                    synchronized (RouterStateChanger.msg) {
                        RouterStateChanger.msg.notify();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println(nextHop.getRouterId() + " is Down");

                isDelivered = false;
                break;
//                return false;
            }
            //NextHop is alive
            else
            {
                if(p.getSpecialMessage().equalsIgnoreCase("SHOW_ROUTE"))
                {
                    routingPath += ",";
                    routingPath += nextHop.getRouterId();
                }

                RoutingTableEntry routingTableEntry = nextHop.getIdtoRTE(nextHop.getRoutingTable(),senderHop.getRouterId());

                //check if nextHop knew about the senderHop till now
                if(routingTableEntry.getDistance() == Constants.INFINITY)
                {
                    System.out.println(senderHop.getRouterId() + " was previously Down");
                    routingTableEntry.setDistance(1);
                    routingTableEntry.setGatewayRouterId(senderHop.getRouterId());

                    //Lock stateChanger
                    RouterStateChanger.islocked = true;
                    sleep(500);

                    System.out.println("DVR is running");
                    NetworkLayerServer.applyDVR(nextHop.getRouterId());

                    RouterStateChanger.islocked = false;
                    try {
                        synchronized (RouterStateChanger.msg) {
                            RouterStateChanger.msg.notify();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                p.hopcount++;           //Increase hopCount
                senderHop = nextHop;    //Packet reached to nextHop

                //Check if the Packet has been Reached to its Destination
                if(nextHop.getRouterId() == destRouter.getRouterId())
                {
                    isDelivered = true;
                    break;
                }
            }

            sleep(500);
        }

        if(p.getSpecialMessage().equalsIgnoreCase("SHOW_ROUTE"))
        {
            response += routingPath.length() + ",";
            response += routingPath;
            for(Router r: NetworkLayerServer.routers)
            {
                response += ",";
                response += r.strRoutingTable();
            }
            response += ",Hop Count: " + p.hopcount;

            networkUtility.write(response);
        }
        return isDelivered;
    }

    public void sleep(int millis)
    {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
    }
}
