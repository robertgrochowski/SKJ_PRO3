package skj.pro3.utils;

import java.io.BufferedWriter;
import java.io.IOException;

public class Utils {

    private static final String DELIMITER = ";";

    public static void w(BufferedWriter bw, String msg) throws IOException {
        bw.write(msg);
        bw.newLine();
        bw.flush();
    }

    public static boolean checkSyntax(String message, int expectedLength)
    {
        String split[] = message.split(DELIMITER);

        if(split.length != expectedLength)
            return false;

        for(int i = 0; i < expectedLength; i++)
            if(split[i].length() < 1)
                return false;

        return true;
    }

    public static String buildMessage(String... args)
    {
        StringBuilder sb = new StringBuilder();

        for(String s : args)
        {
            sb.append(s);
            sb.append(DELIMITER);
        }

        return sb.deleteCharAt(sb.length()-1).toString();
    }

    public static void log(String message) {
        System.out.println(message);
    }

    public static void log(String message, boolean error) {
        System.err.println("[ERROR]:"+message);
    }
}
