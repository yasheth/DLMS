package rm.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import centralRepo.interfaces.HostEnum;
import centralRepo.interfaces.RepositoryOperationEnum;
import server.interfaces.RMEnum;

public class RMUDPRequestHandler implements Runnable {
	private DatagramPacket packet;
	private HostEnum host;

	public RMUDPRequestHandler(DatagramPacket packet,HostEnum host) {
		super();
		this.packet = packet;
		this.host = host;
	}

	@Override
	public void run() {
		System.out.println("Inside run() method.");
		byte[] data = packet.getData();
		if (data == null || data.length == 0)
			return;
		else {
			
			String dataString = new String(data);
			String[] dataArray = dataString.trim().split("#");
			RMEnum operation = RMEnum.valueOf(dataArray[0]);
			RMImpl repo = RMImpl.getRMDB();
			System.out.println("Data string received for processing is - " + dataString);

			DatagramPacket replyPacket;
			boolean resultBool;
			String resultString = null;
			try (DatagramSocket socket = new DatagramSocket();) {
				switch (operation) {

				// request = dataString format RMEnumOperation#<HOST_NAME>
				case FAULT_TOLERANCE:
					System.out.println("Operation requested:  FAULT_TOLERANCE");
					if(dataArray[1].equals(host.name())) {
						System.out.println("Request is for replica managed by this RM.");
						if(repo.faultCorrection(dataArray[1])) {
							System.out.println("Replica DB is updated succesfully.");
						}else System.out.println("operation failed to update replica DB.");
					}else System.out.println("operation is for some other RM.");
					break;

				// request = dataString format RMEnumOperation#<HOST_NAME>
				case HIGH_AVAILABILITY:
					System.out.println("Operation requested: HIGH_AVAILABILITY");
					if(dataArray[1].equals(host.name())) {
						System.out.println("Request is for replica managed by this RM.");
						if(repo.replicaRecovery(dataArray[1])) {
							System.out.println("Replica is recovered succesfully.");
						}else System.out.println("operation failed to start replica.");
					}else System.out.println("operation is for some other RM.");
					break;

				default:
					System.out.println("Default Operation.");
					replyPacket = new DatagramPacket(new byte[0], 0, packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					System.out.println("Returning empty byte array.");
				}
			} catch (SocketException e) {
				System.out.println("Issue with opening socket connection.");
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Issue with sending data packet.");
				e.printStackTrace();
			}
		}
	}
}