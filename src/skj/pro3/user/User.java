package skj.pro3.user;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

/*
    PORTY: TCP: 7000
    UDP:
        Agent: 8000-8003
        User(A): ?
        User(O):

 */

public class User {

    //Parameters: Agent UDP IP:PORT
    public static void main(String[] args) {
        System.out.println("Client start on port");

        if(args.length != 2) throw new IllegalArgumentException();
        User user = null;
        try {
            InetAddress address = InetAddress.getByName(args[0]);
            int port = Integer.parseInt(args[1]);
            user = new User(address, port);
        } catch (IllegalArgumentException e){
            System.err.println("Invalid port value");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter message to send:");

        while (true) {
            System.out.print("> ");
            String msg = scanner.nextLine();
            user.sendMessage(msg);
        }
    }

    int agentPort;
    InetAddress agentIp;
    DatagramSocket socket;

    public User(InetAddress address, int port){
        this.agentPort = port;
        this.agentIp = address;

        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        receiveMessages();
    }

    public void sendMessage(String message) {
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, agentIp, agentPort);
        try {
            socket.send(packet);
        } catch (IOException e) {
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }}
        ).start();
    }

}
