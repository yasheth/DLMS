package server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPFinder {

	public static void main(String[] args) throws UnknownHostException {
		InetAddress localhost = InetAddress.getLocalHost(); 
        System.out.println("System IP Address : " + (localhost.getHostAddress()).trim()); 
  

	}

}
