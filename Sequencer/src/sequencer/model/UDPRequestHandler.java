package sequencer.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import centralRepo.interfaces.Repository;
import server.interfaces.LibraryOperationsEnum;

public class UDPRequestHandler implements Runnable {
	private static final Logger log = LogManager.getLogger(UDPRequestHandler.class);
	private DatagramPacket packet;
	private int sequenceNumber;

	public UDPRequestHandler(DatagramPacket packet,int sequenceNumber) {
		super();
		this.packet = packet;
		this.sequenceNumber=sequenceNumber;
	}

	@Override
	public void run() {
		log.debug("Inside run() method.");
		byte[] data = packet.getData();
		if (data == null || data.length == 0)
			return;
		else {
			
			String dataString = new String(data);
			log.debug("Data string received for processing is - " + dataString);
			String[] dataArray = dataString.trim().split("#");
	
			LibraryOperationsEnum operation = LibraryOperationsEnum.valueOf(dataArray[0]);
			String requiredServer=dataArray[1].substring(0,3);
			String parameters="";
			for(int i=1;i<dataArray.length;i++){
				parameters=parameters+dataArray[i]+"#";
			}
			parameters=parameters.substring(0,parameters.length()-1);
			
			String details=contactCentralRepository("GET_LIBRARY_SERVER_DETAILS#"+requiredServer+"CS");
			String[] detailsArray=details.split("@");
			String hostIP[] = new String[3]; 
			String hostPort[] = new String[3];
			for(int i=0;i<detailsArray.length;i++){
				String[] serverData=detailsArray[i].split("#");
				hostIP[i]=serverData[1];
				hostPort[i]=serverData[2];
			}
			
			
			DatagramPacket replyPacket;
			try (DatagramSocket socket = new DatagramSocket();) {
					log.debug("Operation reuqested:  ADD_LIBRARY_SERVER_DETAILS");
					String multicastString = packet.getAddress()+"#"+packet.getPort()+"#"+sequenceNumber+"#"+operation+"#"+parameters;
					
					for(int i=0;i<hostIP.length;i++){
						replyPacket = new DatagramPacket(multicastString.getBytes(), multicastString.getBytes().length,InetAddress.getByName(hostIP[i]), Integer.parseInt(hostPort[i]));
						socket.send(replyPacket);
						socket.send(replyPacket);
						socket.send(replyPacket);
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
	
	public String contactCentralRepository(String message) {
		DatagramSocket aSocket = null;
		String response = "";
		try {
			aSocket = new DatagramSocket();
			DatagramPacket request = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(Repository.CENTRAL_REPOSITORY_HOSTNAME), Repository.CENTRAL_REPOSITORY_PORT);
			aSocket.send(request);
			
			byte[] buffer = new byte[100000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			aSocket.receive(reply);
			response = new String(reply.getData()).trim();

		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket != null)
				aSocket.close();
		}
		System.out.println("Response sent : " + response);
		return response;
	}

}