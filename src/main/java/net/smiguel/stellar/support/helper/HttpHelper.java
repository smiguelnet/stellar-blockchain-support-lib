package net.smiguel.stellar.support.helper;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HttpHelper {

    public static String getHostAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();

        } catch (UnknownHostException e) {
            //Used only for demonstration purpose
            e.printStackTrace();
            return null;
        }
    }
}
