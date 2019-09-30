package rm.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import centralRepo.interfaces.HostEnum;

public class RMUDPListener implements Runnable {
	private int port;
	private HostEnum host;

	public RMUDPListener(int port, HostEnum host) {
		super();
		this.port = port;
		this.host = host;
	}

	@Override
	public void run() {
		System.out.println("Inside run() method.");
		try (DatagramSocket socket = new DatagramSocket(port)) {
			System.out.println("UDP socket is open at " + port + " port to listen for request.");
			while (true) {
				byte[] data = new byte[5000];
				DatagramPacket packet = new DatagramPacket(data, data.length);
				socket.receive(packet);
				Thread reqestHandler = new Thread(new RMUDPRequestHandler(packet, host));
				reqestHandler.start();
			}
		} catch (SocketException e) {
			System.out.println("Issue with opening socket connection over " + port);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Issue with creating packet from data received on socket.");
			e.printStackTrace();
		}
	}


}
