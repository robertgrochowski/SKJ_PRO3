package skj.pro3.utils;

import java.io.BufferedWriter;
import java.io.IOException;

public class Utils {

    public static void w(BufferedWriter bw, String msg) throws IOException {
        bw.write(msg);
        bw.newLine();
        bw.flush();
    }

    public static void log(String message) {
        System.out.println(message);
    }

    public static void log(String message, boolean error) {
        if(error) System.err.println("[ERROR]:"+message);
        else log(message);
    }
}
