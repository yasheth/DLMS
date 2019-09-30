package centralrepository.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import centralRepo.interfaces.RepositoryOperationEnum;

public class UDPRequestHandler implements Runnable {
	private static final Logger log = LogManager.getLogger(UDPRequestHandler.class);
	private DatagramPacket packet;

	public UDPRequestHandler(DatagramPacket packet) {
		super();
		this.packet = packet;
	}

	@Override
	public void run() {
		log.debug("Inside run() method.");
		byte[] data = packet.getData();
		if (data == null || data.length == 0)
			return;
		else {
			
			String dataString = new String(data);
			String[] dataArray = dataString.trim().split("#");
			RepositoryOperationEnum operation = RepositoryOperationEnum.valueOf(dataArray[0]);
			RepositoryImpl repo = RepositoryImpl.getRepoDB();
			log.debug("Data string received for processing is - " + dataString);

			DatagramPacket replyPacket;
			boolean resultBool;
			String resultString = null;
			try (DatagramSocket socket = new DatagramSocket();) {
				switch (operation) {

				// request = dataString format enumOperation#CONSS#Host1#ip#port
				// response = dataString format TRUE/FALSE
				case ADD_LIBRARY_SERVER_DETAILS:
					log.debug("Operation reuqested:  ADD_LIBRARY_SERVER_DETAILS");
					resultBool = repo.addLibraryServerDetails(dataArray[1], dataArray[2], dataArray[3], Integer.parseInt(dataArray[4]));
					resultString = resultBool?"TRUE":"FALSE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : " + resultString);
					break;

				// request = dataString format enumOperation#CONSS
				// response = dataString format string as Host1#<ip>#<port>@Host2#<ip>#<port>@Host3#<ip>#<port>
				case GET_LIBRARY_SERVER_DETAILS:
					log.debug("Operation reuqested: GET_LIBRARY_SERVER_DETAILS");
					resultString = repo.getLibraryServerDetails(dataArray[1]);
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : " + resultString);
					break;

				default:
					log.debug("Default Operation.");
					replyPacket = new DatagramPacket(new byte[0], 0, packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Returning empty byte array.");
				}
			} catch (SocketException e) {
				log.error("Issue with opening socket connection.", e);
				e.printStackTrace();
			} catch (IOException e) {
				log.error("Issue with sending data packet.", e);
				e.printStackTrace();
			}
		}
	}
}