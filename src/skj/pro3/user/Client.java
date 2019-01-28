package skj.pro3.user;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Client {

    //Parameters: [AGENT IP] [REMOTE LISTEN UDP(in fact agent's)] [REMOTE SEND UDP]
    public static void main(String[] args) {
        System.out.println("Client start on port");

        if(args.length != 3) throw new IllegalArgumentException("Bad parameters amount");
        Client user = null;
        try {
            InetAddress address = InetAddress.getByName(args[0]);
            int remoteListen = Integer.parseInt(args[1]);
            int remoteSend = Integer.parseInt(args[2]);
            user = new Client(address, remoteListen, remoteSend);
        } catch (IllegalArgumentException e){
            System.err.println("Invalid parameter(s) value(s)");
            System.exit(-1);
        } catch (UnknownHostException e) {
            System.err.println("Invalid host address");
            System.exit(-1);
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter message to send:");
        System.out.print("> ");

        while (true) {
            String msg = scanner.nextLine();
            user.sendMessage(msg);
        }
    }

    int agentReceivePort; //+1000
    int remoteSendPort; //+1000
    InetAddress agentIp;
    DatagramSocket socket;

    public Client(InetAddress address, int remoteListen, int remoteSend){
        this.agentReceivePort = remoteListen;
        this.agentIp = address;

        this.remoteSendPort = remoteSend;

        try {
            socket = new DatagramSocket(remoteSend+1000); //have same listen port as remote send port
        } catch (SocketException e) {
            System.out.println(remoteSend + 1000);
            e.printStackTrace();
            System.exit(-1);
        }

        receiveMessages();
    }

    public void sendMessage(String message) {
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, agentIp, (agentReceivePort+1000));
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Cannot send UDP message:");
            e.printStackTrace();
        }
    }

    public void receiveMessages() {

        new Thread(()-> {
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[1460], 1460);
                    socket.receive(packet);

                    System.out.println("[RECEIVED]: " + new String(packet.getData()));
                    System.out.print("> ");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }}
        ).start();
    }

}
