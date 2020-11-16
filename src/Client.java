import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

//Work needed
public class Client {
    public static void main(String[] args) throws InterruptedException {
        NetworkUtility networkUtility = new NetworkUtility("127.0.0.1", 4444);
        System.out.println("Connected to server");
        /**
         * Tasks
         */
        EndDevice endDevice = null;
        Random random = new Random();
        Scanner input = new Scanner(System.in);


        while (endDevice == null)
        {
            endDevice = (EndDevice) networkUtility.read();
            System.out.println("Device ID: " + endDevice.getDeviceID());
            //System.out.println(endDevice.getGateway());
            System.out.println("Device IP: " + endDevice.getIpAddress());
        }

        while (true)
        {
            System.out.println("Choose Option: ");
            System.out.println("1. Send Packets ");
            System.out.println("2. Exit");

            int option = Integer.parseInt(input.nextLine());

            if(option == 1)
            {
                    networkUtility.write(1);
                    int totalHopCount = 0;
                    int totalDroppedPacket = 0;

                    //Receive Active Client List (without Self)
                    String activeClients = null;
                    while (activeClients == null) {
                        activeClients = (String) networkUtility.read();
                    }

                    System.out.println("Active Clients: " + activeClients);
                    String[] clientList = activeClients.split(",");
//                System.out.println(clientList.length);
                    //Active Client List Received

                    for (int i = 0; i < Constants.totalPacketsToSent; i++) {
                        //Generate Random Msg
                        int length = random.nextInt(10) + 1;
                        String randomMsg = "";
                        while (length-- != 0) randomMsg += (char) (random.nextInt(26) + 'a');

                        //Random Client Selection
                        String dest = clientList[random.nextInt(clientList.length)];
                        if (dest.length() < 1) continue;
                        IPAddress destIp = new IPAddress(dest);
                        System.out.println("Destination: " + destIp.getString());
                        //Create and Send Packet
                        if (i == Constants.specialPacketNumber) {
                            Packet packet = new Packet(randomMsg, "SHOW_ROUTE", endDevice.getIpAddress(), destIp);
                            networkUtility.write(packet);
                            String response = null;
                            while (response == null) {
                                response = (String) networkUtility.read();
                            }

                            String[] responseArray = response.split(",");
                            int routingPathLength = Integer.parseInt(responseArray[0]);
                            System.out.print("Routing Path: ");
                            for (int index = 1; index <= routingPathLength; index++) {
                                System.out.print(responseArray[index] + "\t");
                            }
                            System.out.println();
                            for (int index = routingPathLength + 1; index < responseArray.length; index++) {
                                System.out.println(responseArray[index]);
                            }
                        } else {
                            Packet packet = new Packet(randomMsg, "", endDevice.getIpAddress(), destIp);
                            networkUtility.write(packet);
                        }

                        String acknowledement = null;
                        while (acknowledement == null) {
                            acknowledement = (String) networkUtility.read();
                        }

                        String[] msgArray = acknowledement.split(",");

                        System.out.println("------------------------------------");
                        System.out.println("Packet Number: " + (i + 1));
                        System.out.println(msgArray[0]);
                        if (msgArray[0].equalsIgnoreCase("dropped packet")) {
                            totalDroppedPacket++;
                        } else {
                            //System.out.println("Hop Count: " + msgArray[1]);
                            int hopCount = Integer.parseInt(msgArray[1]);
                            totalHopCount += hopCount;
                        }

                    }

                    double avgHopCount = (totalHopCount * 1.0) / Constants.totalPacketsToSent;
                    double avgDropRate = (totalDroppedPacket * 1.0) / Constants.totalPacketsToSent;

                    System.out.println("Avg Hop Count: " + avgHopCount);
                    System.out.println("Avg Drop Rate: " + avgDropRate);

            }

            else
            {
                networkUtility.write(2);
                System.out.println("Client Exited From network");
                networkUtility.closeConnection();
                break;
            }
        }


        /*
        1. Receive EndDevice configuration from server
        2. Receive active client list from server
        3. for(int i=0;i<100;i++)
        4. {
        5.      Generate a random message
        6.      Assign a random receiver from active client list
        7.      if(i==20)
        8.      {
        9.            Send the message and recipient IP address to server and a special request "SHOW_ROUTE"
        10.           Display routing path, hop count and routing table of each router [You need to receive
                            all the required info from the server in response to "SHOW_ROUTE" request]
        11.     }
        12.     else
        13.     {
        14.           Simply send the message and recipient IP address to server.
        15.     }
        16.     If server can successfully send the message, client will get an acknowledgement along with hop count
                    Otherwise, client will get a failure message [dropped packet]
        17. }
        18. Report average number of hops and drop rate
        */
    }
}
