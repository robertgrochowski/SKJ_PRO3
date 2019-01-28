package skj.pro3.user;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;


public class Recipient {

    //Parameters: Agent UDP IP:PORT
    public static void main(String[] args) {
        System.out.println("Waiting for messages");

        if(args.length != 1) throw new IllegalArgumentException();
        try {
            int myPort = Integer.parseInt(args[0])+1000;
            new Recipient(myPort);
        } catch (IllegalArgumentException e){
            System.err.println("Invalid port value");
        }
    }

    DatagramSocket socket;

    public Recipient(int myPort){
        try {
            socket = new DatagramSocket(myPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        receiveMessages();
    }

    public void sendMessage(String message, InetAddress ip, int port) {
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, ip, port);
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
                    String msg = new String(packet.getData());
                    System.out.println("[RECEIVED]: " + msg);
                    System.out.println("Making echo on port: "+packet.getPort());
                    sendMessage("echo: " + msg, packet.getAddress(), packet.getPort());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }}
        ).start();
    }

}
