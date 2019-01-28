package skj.pro3.user;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;


public class Recipient {

    //Parameters: [LISTEN PORT] [SENDING PORT]
    public static void main(String[] args) {
        System.out.println("Waiting for messages");

        if(args.length != 2) throw new IllegalArgumentException();
        try {
            int myListenPort = Integer.parseInt(args[0]);
            int mySendPort = Integer.parseInt(args[1]);
            new Recipient(myListenPort, mySendPort);
        } catch (IllegalArgumentException e){
            System.err.println("Invalid port(s) value(s)");
            System.exit(-1);
        }
    }

    DatagramSocket sendSocket;
    DatagramSocket listenSocket;

    public Recipient(int myListenPort, int mySendPort){
        try {
            listenSocket = new DatagramSocket(myListenPort);
            sendSocket = new DatagramSocket(mySendPort);
            receiveMessages();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message, InetAddress ip, int port) {
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, ip, port);
        try {
            sendSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveMessages() {
        new Thread(()-> {
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[1460], 1460);
                    listenSocket.receive(packet);
                    String msg = new String(packet.getData());
                    System.out.println("[RECEIVED]: " + msg);
                    System.out.println("[BACK-MESSAGE]: Sending back echo message");
                    sendMessage("echo [" + msg.trim() + "] ", packet.getAddress(), packet.getPort());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }}
        ).start();
    }
}
