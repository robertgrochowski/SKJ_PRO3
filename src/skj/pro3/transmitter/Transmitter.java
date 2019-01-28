package skj.pro3.transmitter;

import skj.pro3.utils.ForwardData;
import skj.pro3.utils.Utils;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Transmitter {

    public class AgentCommunicator implements Runnable {

        BlockingQueue<ForwardData> dataToForward = new LinkedBlockingDeque<>();

        public AgentCommunicator() {
            new Thread(this).start();
        }
        public void forwardToAgent(ForwardData data) {
            dataToForward.add(data);
        }

        @Override
        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(agentSocket.getInputStream()));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(agentSocket.getOutputStream()));
                boolean waitingForData = false;
                while (!Thread.currentThread().isInterrupted()) {
                    if (agentSocket.getInputStream().available() > 0) {
                        String message = br.readLine();

                        //Forwarding data to Recipient
                        if (waitingForData) {
                            int port = Integer.parseInt(message);
                            Utils.w(bw, "OK");
                            String msg = br.readLine();
                            recipientCommunicator.forwardToRecipient(new ForwardData(msg, port));
                            waitingForData = false;
                            Utils.w(bw, "OK");
                        } else if (message.equals("SHUTDOWN")) {
                            Utils.w(bw, "OK");
                            shutdown();
                            return;
                        } else if (message.equals("FORWARD")) {
                            Utils.w(bw, "OK");
                            waitingForData = true;
                        }
                    }

                    //Forwarding to Agent
                    if (dataToForward.size() > 0) {
                        ForwardData data = dataToForward.poll();
                        if (data == null) continue; //IDE problem

                        Utils.w(bw, "FORWARD");
                        String response = br.readLine();

                        if (response.equals("OK")) {
                            Utils.w(bw, data.getPort() + "");
                            response = br.readLine();
                            if (response.equals("OK")) {
                                Utils.w(bw, data.getData());
                            } else Utils.log("Agent responded with error while forwarding message");
                        }
                    }
                }
            }
            catch (Exception e) {
                if (!e.getMessage().toLowerCase().equals("socket is closed")) {
                    e.printStackTrace();
                    Utils.log("An error occurred in AgentCommunicator, exiting");
                    System.exit(-1);
                }
            }
        }

        void selfShutdown() {
            try {
                agentSocket.close();
                Utils.log("Shutting down agentCommunicator...OK");
            } catch (IOException e) {
                Utils.log("Shutting down agentCommunicator...ERROR", true);
                e.printStackTrace();
            }
        }
    }

    public class RecipientCommunicator implements Runnable{

        DatagramSocket socket;

        public RecipientCommunicator() {
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
                Utils.log("Couldn't open port, exiting", true);
                System.exit(-1);
            }

            new Thread(this).start();
        }

        public void forwardToRecipient(ForwardData data) throws IOException {
            byte[] packet = data.getData().getBytes();
            DatagramPacket dataToSend = new DatagramPacket(packet, packet.length, destClient, data.getPort());
            socket.send(dataToSend);
            Utils.log("Forwarding data to Recipient, data:["+data.getData()+"]");
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(new byte[1460], 1460);
                    socket.receive(packet);

                    if (packet.getAddress().equals(destClient)) //Only specified client
                    {
                        Utils.log("Received BACK-message from Recipient - forwarding to Agent");
                        agentCommunicator.forwardToAgent(new ForwardData(new String(packet.getData()), packet.getPort()));
                    }
                    else Utils.log("Ignoring message from unpermitted client: " + packet.getAddress().toString() + ":"+packet.getPort());
                }
            } catch (IOException e) {
                if (!e.getMessage().toLowerCase().equals("socket closed"))
                    e.printStackTrace();
            }
        }

        void selfShutdown() {
            socket.close();
            Utils.log("Shutting down recipientCommunicator...OK");
        }
    }

    private static int TCP_PORT;
    private Socket agentSocket;
    private InetAddress destClient;

    private AgentCommunicator agentCommunicator;
    private RecipientCommunicator recipientCommunicator;

    private boolean forceShutdown = false;

    public static void main(String[] args) {
        if(args.length != 1) throw new IllegalArgumentException("First param must be TCP port");

        try {
            TCP_PORT = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.err.println("Invalid TCP port value");
            System.exit(-1);
        }

        Utils.log("Transmitter initialization");
        Utils.log("TCP listening on port: " + TCP_PORT);
        Utils.log("-----------");

        new Transmitter();
    }

    public Transmitter(){
        try {
            configureTransmitter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureTransmitter() throws IOException {
        ServerSocket serverSocket = new ServerSocket(TCP_PORT);
        Utils.log("Waiting for TCP connection");
        agentSocket = serverSocket.accept();

        BufferedReader br = new BufferedReader(new InputStreamReader(agentSocket.getInputStream()));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(agentSocket.getOutputStream()));

        Utils.log("Connection established, waiting for configuration");
        boolean configured = false;
        boolean waitingForPort = false;
        while (!configured) {

            String msg = br.readLine();

            if(msg.equals("RECEIVER") && !waitingForPort){
                Utils.w(bw, "OK");
                waitingForPort = true;
            }
            else if(waitingForPort)
            {
                try {
                    destClient = InetAddress.getByName(msg);
                    configured = true;
                    Utils.w(bw, "OK");
                } catch (Exception e) {
                    Utils.w(bw, "ERROR");
                    Utils.log("Invalid configuration message, excepted IP got ["+msg+"]", true);
                    Utils.log("waiting for correct configuration");
                }
                finally {
                    waitingForPort = false;
                }
            }
        }

        Utils.log("Configuration successful!");
        Utils.log("=========================");
        agentCommunicator = new AgentCommunicator();
        recipientCommunicator = new RecipientCommunicator();
    }

    private void shutdown(){
        Utils.log("Shutting down agentCommunicator...");
        agentCommunicator.selfShutdown();
        Utils.log("Shutting down recipientCommunicator...");
        recipientCommunicator.selfShutdown();
    }
}
