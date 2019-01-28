package skj.pro3.agent;

import skj.pro3.utils.ForwardData;
import skj.pro3.utils.Utils;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Agent {

    public class ClientCommunicator implements Runnable {
        DatagramSocket socket;

        public ClientCommunicator(DatagramSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[1460], 1460);
                    socket.receive(packet);

                    System.out.println("Received message: " + new String(packet.getData()));
                    forwardToTransmitter(new ForwardData(new String(packet.getData()), socket.getLocalPort()));

                } catch (IOException e) {
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

                while (true) {

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
                e.printStackTrace();
            }
        }
    }


    /* Parametry:
        - Adres IP przekaznika
        - Port TCP przekaznika
        - Adres IP odbiorcy
        - Conajmniej jeden port UDP na ktore nasluchuje dane od procesu
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
    private Executor executor;
    private DatagramSocket[] udpSockets;


    public Agent(InetSocketAddress transmitter, InetAddress recipient, List<Integer> udpPorts) throws IOException {
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.recipient = recipient;
        executor = Executors.newFixedThreadPool(udpPorts.size()+1);
        udpSockets = new DatagramSocket[udpPorts.size()];
        transmitterSocket = new Socket(transmitter.getAddress(), transmitter.getPort());

        //Configure transmitter
        configureTransmitter();

        //Open UDP ports and start listen
        for (int i = 0; i < udpPorts.size(); i++) {
            udpSockets[i] = new DatagramSocket(udpPorts.get(i));
            executor.execute(new ClientCommunicator(udpSockets[i]));
        }

        executor.execute(new TransmitterListener());

        //TODO: scanner wait for stop
    }

    void configureTransmitter() throws IOException {
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

    void forwardToTransmitter(ForwardData data) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(transmitterSocket.getInputStream()));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(transmitterSocket.getOutputStream()));
        lockTransmitterListener = true;

        Utils.w(bw, "FORWARD");
        System.out.println("waiting for response");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(transmitterSocket.getInputStream().available());
        String response = br.readLine();
        if (response.equals("OK")) {
            Utils.w(bw, data.getPort() + "");
            br.readLine(); //todo
            Utils.w(bw, data.getData());
            br.readLine(); //todo
        }
        lockTransmitterListener = false;
        System.out.println("Forwarded message to Transmitter");
    }

    void forwardToClient(ForwardData data) throws IOException {

        for (DatagramSocket socket : udpSockets)
        {
            if (socket.getLocalPort() == data.getPort())
            {
                byte[] packet = data.getData().getBytes();
                //DatagramPacket dataToSend = new DatagramPacket(packet, packet.length, /*client*/, /*client port*/data.getPort());
                //socket.send(dataToSend);
                return;
            }
        }
        Utils.log("No specified port found..:"+data.getPort(), true);
    }
}
