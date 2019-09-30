package rm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import centralRepo.interfaces.HostEnum;
import centralRepo.interfaces.Repository;
import rm.model.RMUDPListener;

public class ReplicaManagerMain {

	public static void main(String[] args) {
			// SERVER and UDP details.
			String library = "RM";
			int portUDP = 3007;
			HostEnum host = HostEnum.HOST_3;


			// setting up UDP listener thread for FE-RM communication.
			Thread rmUDPThread = new Thread(new RMUDPListener(portUDP, host));
			rmUDPThread.start();
			System.out.println("Starting UDP thread for RM-FE comm.");

			try(DatagramSocket socket = new DatagramSocket();){
				byte[] dataByte =  new byte[5000];
				System.out.println("Registering FE details with central repo.");
				String dataString = library+"#"+HostEnum.HOST_3.name()+"#"+InetAddress.getLocalHost().getHostAddress()+portUDP;
				DatagramPacket packet = new DatagramPacket(dataString.getBytes(), dataString.getBytes().length, InetAddress.getByName(Repository.CENTRAL_REPOSITORY_HOSTNAME), Repository.CENTRAL_REPOSITORY_PORT);
				socket.send(packet);
				packet = new DatagramPacket(dataByte, dataByte.length);
				socket.receive(packet);
				if(packet.getData().toString().equals("TRUE"))
					System.out.println("RM connection details are registered.");
				
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
			
			System.out.println("RM server is up.");

	}

}
