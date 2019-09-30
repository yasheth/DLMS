package rm.model;

import centralRepo.interfaces.HostEnum;
import server.ConLibraryServerMain;

public class RMImpl {
	private HostEnum host = HostEnum.HOST_1;
	private static RMImpl rmDB;
	private static int faultCounter = 0;

	private RMImpl() {
	}

	public static RMImpl getRMDB() {
		if (rmDB == null)
			rmDB = new RMImpl();

		return rmDB;
	}

	public synchronized boolean faultCorrection(String hostName) {
		if (hostName.equals(host.name()))
			faultCounter++;
		if (faultCounter == 3) {
			if (ConLibraryServerMain.serverHandler(false)) {
				System.out.println("Replica DB updated successfully.");
				faultCounter = 0;
				return true;
			} else return false;
		} else return true;

	}

	public synchronized boolean replicaRecovery(String hostName) {
		if(hostName.equals(host.name()) && ConLibraryServerMain.serverHandler(true)) {
			System.out.println("Replica recovered and DB updated successfully.");
			return true;
		} else return false;
	}
}
