package skj.pro3.user;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Recipient {

    public class MessageReceiver implements Runnable {
        DatagramSocket listenSocket;
        DatagramSocket sendSocket;

        MessageReceiver(DatagramSocket listenSocket, DatagramSocket sendSocket) {
            this.listenSocket = listenSocket;
            this.sendSocket = sendSocket;
            new Thread(this).start();
        }

        @Override
        public void run()
        {
            try {
                while(!Thread.currentThread().isInterrupted())
                {
                    DatagramPacket packet = new DatagramPacket(new byte[1460], 1460);
                    listenSocket.receive(packet);
                    String msg = new String(packet.getData());
                    System.out.println("[RECEIVED] on port["+listenSocket.getLocalPort()+"]: " + msg);
                    System.out.println("[BACK-MESSAGE]: Sending back echo message");
                    sendMessage(sendSocket, "echo [" + msg.trim() + "] ", packet.getAddress(), packet.getPort());
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Parameters: -l [LISTEN PORTS] -S [SENDING PORTS]
    public static void main(String[] args) {

        List<Integer> listenPorts = new ArrayList<>();
        List<Integer> sendingPorts = new ArrayList<>();
        Boolean listen = null;

        for(String arg: args)
        {
            if (arg.equals("-l"))
                listen = true;
            else if (arg.equals("-s"))
                listen = false;
            else if(listen == null)
                throw new IllegalArgumentException("Invalid parameters syntax");
            else {
                try {
                    if (listen) listenPorts.add(Integer.parseInt(arg));
                    else sendingPorts.add(Integer.parseInt(arg));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid port value");
                }
            }
        }

        if (listenPorts.size() != sendingPorts.size() || listenPorts.size() < 1) {
            throw new IllegalArgumentException("Listen ports amount must be equal to send ports amount");
        }

        new Recipient(listenPorts, sendingPorts);
        System.out.println("Listening for messages...");
    }

    private Recipient(List<Integer> listenPorts, List<Integer> sendingPorts){
        try {
            DatagramSocket[] listenSocket = new DatagramSocket[listenPorts.size()];
            DatagramSocket[] sendSocket = new DatagramSocket[sendingPorts.size()];
            for (int i = 0; i < listenPorts.size(); i++) {
                listenSocket[i] = new DatagramSocket(listenPorts.get(i));
                sendSocket[i] = new DatagramSocket(sendingPorts.get(i));

                new MessageReceiver(listenSocket[i], sendSocket[i]);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(DatagramSocket socket, String message, InetAddress ip, int port) {
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, ip, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
