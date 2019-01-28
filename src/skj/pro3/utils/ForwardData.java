package skj.pro3.utils;

public class ForwardData {
    public String data;
    public int port;

    public ForwardData(String data, int port) {
        this.data = data;
        this.port = port;
    }

    public String getData() {
        return data;
    }

    public int getPort() {
        return port;
    }
}
