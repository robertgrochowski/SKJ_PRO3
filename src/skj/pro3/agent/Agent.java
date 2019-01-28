package skj.pro3.agent;

import skj.pro3.utils.ForwardData;
import skj.pro3.utils.Utils;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Agent {

    public class ClientCommunicator implements Runnable {
        DatagramSocket socket;

        private ClientCommunicator(DatagramSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted())
            {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[1460], 1460);
                    socket.receive(packet);

                    InetSocketAddress client = new InetSocketAddress(packet.getAddress(), packet.getPort());

                    if(activeClients.get(socket) == null)
                    {
                        Utils.log("Associating client to socket "+client.getAddress()+":"+client.getPort());
                        activeClients.put(socket, client);
                    }
                    else if(!activeClients.get(socket).equals(client))
                    {
                        Utils.log("Client tried to connect on busy socket..", true);
                        continue;
                    }

                    System.out.println("[RECEIVED] on port ["+(socket.getLocalPort()-1000)+"]: " + new String(packet.getData()));
                    forwardToTransmitter(new ForwardData(new String(packet.getData()), (socket.getLocalPort()-1000))); //forward on correct port

                } catch (IOException e) {
                    if (!e.getMessage().toLowerCase().equals("socket closed"))
                        e.printStackTrace();
                }
            }
        }
    }

    public class TransmitterListener implements Runnable {

        @Override
        public void run() {

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(transmitterSocket.getInputStream()));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(transmitterSocket.getOutputStream()));

                while (!Thread.currentThread().isInterrupted()) {
                    if(transmitterSocket.getInputStream().available() > 0 && !lockTransmitterListener) {
                        String msg = br.readLine();
                        if (msg.equals("FORWARD")) {
                            Utils.w(bw, "OK");

                            int port = Integer.parseInt(br.readLine());
                            Utils.w(bw, "OK");
                            String message = br.readLine();
                            Utils.w(bw, "OK");

                            forwardToClient(new ForwardData(message, port));
                        }
                    }
                }
            } catch (IOException e) {
                if (!e.getMessage().toLowerCase().equals("socket is closed")) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* Parameters:
        - Transmitter IP
        - Transmitter PORT
        - Recipient IP
        - At least one UDP port to listen (associated with receiver udp listen ports)
     */

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            throw new IllegalArgumentException("Too little program arguments");
        }

        InetSocketAddress transmitter = null;
        InetAddress recipient = null;
        try {
            transmitter = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
            recipient = InetAddress.getByName(args[2]);
        } catch (Exception e) {
            Utils.log(e.toString(), true);
            System.exit(-1);
        }

        List<Integer> udpPorts = new ArrayList<>();
        for (int i = 3; i < args.length; i++) {
            udpPorts.add(Integer.parseInt(args[i]));
        }

        new Agent(transmitter, recipient, udpPorts);
    }

    private boolean lockTransmitterListener = false;
    private Socket transmitterSocket;
    private InetAddress recipient;

    private Map<DatagramSocket, InetSocketAddress> activeClients = new HashMap<>();


    private Agent(InetSocketAddress transmitter, InetAddress recipient, List<Integer> udpPorts) throws IOException {

        Utils.log("Agent started, type 'exit' to shutdown");

        this.recipient = recipient;
        ExecutorService executor = Executors.newFixedThreadPool(udpPorts.size() + 1);
        DatagramSocket[] udpSockets = new DatagramSocket[udpPorts.size()];
        transmitterSocket = new Socket(transmitter.getAddress(), transmitter.getPort());

        //Configure transmitter
        configureTransmitter();

        //Open UDP ports and start listen
        for (int i = 0; i < udpPorts.size(); i++) {
            udpSockets[i] = new DatagramSocket((udpPorts.get(i)+1000));
            executor.execute(new ClientCommunicator(udpSockets[i]));
        }

        executor.execute(new TransmitterListener());

        boolean running = true;
        while(running)
        {
            Scanner scanner = new Scanner(System.in);
            if (scanner.nextLine().equals("exit")) {
                Utils.log("Shutting down transmitter...");
                shutdownTransmitter();
                Utils.log("Shutting down Agent...");
                for (DatagramSocket socket : udpSockets)
                    socket.close();

                transmitterSocket.close();

                executor.shutdownNow();
                Utils.log("Shutting down Agent...OK");
                running = false;
            } else Utils.log("Invalid command, type 'exit' to shutdown");
        }
    }

    private void shutdownTransmitter() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(transmitterSocket.getInputStream()));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(transmitterSocket.getOutputStream()));

            Utils.w(bw, "SHUTDOWN");

            String response = br.readLine();

            if (response.equals("OK")) {
                Utils.log("Shutting down transmitter...OK");
            } else Utils.log("Shutting down transmitter...ERROR");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureTransmitter() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(transmitterSocket.getInputStream()));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(transmitterSocket.getOutputStream()));

        Utils.w(bw, "RECEIVER");

        String response = br.readLine();

        if (response.equals("OK")) {
            Utils.w(bw, recipient.getHostAddress());

            if(response.equals("OK")) {
                Utils.log("Transmitter successfully configured");
                return;
            }
        }
        Utils.log("An error occured while configuring transmitter..");
        System.exit(-1);
    }

    private void forwardToTransmitter(ForwardData data) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(transmitterSocket.getInputStream()));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(transmitterSocket.getOutputStream()));
        lockTransmitterListener = true;

        Utils.w(bw, "FORWARD");
        String response = br.readLine();
        if (response.equals("OK")) {
            Utils.w(bw, data.getPort() + "");
            br.readLine();
            Utils.w(bw, data.getData());
            br.readLine();
        }
        lockTransmitterListener = false;
        Utils.log("[FORWARD]: ["+data.getData().trim()+"] forwarded to Transmitter");
    }

    private void forwardToClient(ForwardData data) throws IOException {

        int clientPort = data.getPort()+1000;

        for (Map.Entry<DatagramSocket, InetSocketAddress> e : activeClients.entrySet()) {
            if(e.getValue().getPort() == clientPort)
            {
                DatagramSocket socket = e.getKey();
                byte[] packet = data.getData().getBytes();
                DatagramPacket dataToSend = new DatagramPacket(packet, packet.length, e.getValue().getAddress(), e.getValue().getPort());
                socket.send(dataToSend);
                Utils.log("[BACK-MESSAGE] from Transmitter forwarded to Client on port ["+data.getPort()+"]");
                return;
            }
        }

        Utils.log("Could not find client in activeClients MAP, client port:"+data.getPort(), true);
    }
}
