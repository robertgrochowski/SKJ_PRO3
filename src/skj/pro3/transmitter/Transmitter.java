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
            try
            {
                BufferedReader br = new BufferedReader(new InputStreamReader(agentSocket.getInputStream()));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(agentSocket.getOutputStream()));
                boolean waitingForData = false;
                while (true) {

                    if (agentSocket.getInputStream().available() > 0) {
                        String message = br.readLine();

                        //Forward to recepient
                        if (waitingForData) {
                            int port = Integer.parseInt(message);
                            Utils.w(bw, "OK"); //TODO
                            String msg = br.readLine();
                            recipientCommunicator.forwardToRecipient(new ForwardData(msg, port));
                            waitingForData = false;
                            Utils.w(bw, "OK"); //TODO
                        }
                        else if (message.equals("SHUTDOWN")) {
                            //todo: do shutdown
                        } else if (message.equals("FORWARD"))
                        {
                            Utils.w(bw,"OK");
                            waitingForData = true;
                        }
                    }

                    //Forward to agent
                    if (dataToForward.size() > 0) {
                        System.out.println("size > 0");
                        ForwardData data = dataToForward.poll();

                        Utils.w(bw,"FORWARD");
                        String response = br.readLine();

                        if (response.equals("OK")) {
                            Utils.w(bw, data.getPort()+"");
                            response = br.readLine();

                            Utils.w(bw, data.getData());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
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
            }

            new Thread(this).start();
        }

        public void forwardToRecipient(ForwardData data) throws IOException {
            byte[] packet = data.getData().getBytes();
            DatagramPacket dataToSend = new DatagramPacket(packet, packet.length, destClient, data.getPort()+1000);
            socket.send(dataToSend);
            System.out.println("Data sent on port: " + (data.getPort() + 1000));
        }

        @Override
        public void run() {
            try {
                while (true) {
                    DatagramPacket packet = new DatagramPacket(new byte[1460], 1460);
                    socket.receive(packet);

                    if (packet.getAddress().equals(destClient)) //Only specified client
                    {
                        System.out.println("I got message from Recipient: "+(packet.getPort()-1000));
                        agentCommunicator.forwardToAgent(new ForwardData(new String(packet.getData()), packet.getPort()-1000));

                    }

                    else
                        Utils.log("Ignoring message from unpermitted client " + packet.getAddress().toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static int TCP_PORT;
    private Socket agentSocket;
    private InetAddress destClient;

    private AgentCommunicator agentCommunicator;
    private RecipientCommunicator recipientCommunicator;

    public static void main(String[] args) {
        if(args.length != 1) throw new IllegalArgumentException("First param must be TCP port");

        try {
            TCP_PORT = Integer.parseInt(args[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid TCP port value");
        }

        Utils.log("Transmitter initialization");
        Utils.log("TCP listen port: " + TCP_PORT);

        new Transmitter();
    }

    public Transmitter(){
        try {
            beginTCP();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void beginTCP() throws IOException {
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
                }
                finally {
                    waitingForPort = false;
                }
            }
        }

        Utils.log("Configuration DONE");
        agentCommunicator = new AgentCommunicator();
        recipientCommunicator = new RecipientCommunicator();
    }




    private void shutdown(){

    }

    //Nasluch na TCP
    //TCP konfiguracja:
    /*
    -odbiorca - port
    -otwieranie losowego udp do kontaktu z odbiorca:
        - po otrzymaiu przesyla je po tcp do agenta

    potem tcp:
    -komenda rozlaczenia - tryb oczekiwania standby
    -rozkaz wyslania pakietu udp do odbiorcy
     */
}
