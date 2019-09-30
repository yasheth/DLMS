package sequencer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import centralRepo.interfaces.Repository;
import centralRepo.interfaces.RepositoryOperationEnum;
import sequencer.model.UDPListener;

public class SequencerMain {

	private static final Logger log = LogManager.getLogger(SequencerMain.class);
	
	public static void main(String[] args) {
		int port = 2999;
		String componentName = "SQ";
		log.debug("Inside main() method.");
		// setting up UDP listener thread.
		Thread udpThread = new Thread(new UDPListener(port));
		udpThread.start();
		
		System.out.println("Starting UDP thread for FE-Sequencer comm.");

		try(DatagramSocket socket = new DatagramSocket();){
			byte[] dataByte =  new byte[5000];
			System.out.println("Registering Sequencer details with central repo.");
			String dataString = RepositoryOperationEnum.ADD_LIBRARY_SERVER_DETAILS.name()+"#"+componentName+"#"+""+"#"+InetAddress.getLocalHost().getHostAddress()+"#"+port;
			DatagramPacket packet = new DatagramPacket(dataString.getBytes(), dataString.getBytes().length, InetAddress.getByName(Repository.CENTRAL_REPOSITORY_HOSTNAME), Repository.CENTRAL_REPOSITORY_PORT);
			socket.send(packet);
			packet = new DatagramPacket(dataByte, dataByte.length);
			socket.receive(packet);
			dataString = new String(packet.getData());
			if(dataString.trim().equals("TRUE"))
				System.out.println("SQ connection details are registered.");
			
		} catch (SocketException e) {
			System.out.println("There is an error creating or accessing a Socket.");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			System.out.println("The IP address of a host could not be determined.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Issue with I/O over socket connection.");
			e.printStackTrace();
		}
		log.debug("Sequencer is up.");
	}

}
