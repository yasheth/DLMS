package centralrepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import centralRepo.interfaces.Repository;
import centralrepository.model.UDPListener;

public class CentralRepositoryMain {

	private static final Logger log = LogManager.getLogger(CentralRepositoryMain.class);

	public static void main(String[] args) {
		log.debug("Inside main() method.");
		/*try {
			Registry registry = LocateRegistry.createRegistry(2000);
			Repository repository = new RepositoryImpl();
			registry.rebind("Repository", repository);
			log.debug("Created repository and binded at port 2000.");
		} catch (AccessException e) {
			log.error("Issue binding to registry." + "\n" + e.getMessage(),e);
			e.printStackTrace();
		} catch (RemoteException e) {
			log.error("Issue either creating registry or from creating repository instance." + "\n" + e.getMessage(),e);
			e.printStackTrace();
		}*/
		// setting up UDP listener thread.
		Thread udpThread = new Thread(new UDPListener(Repository.CENTRAL_REPOSITORY_PORT));
		udpThread.start();
		log.debug("Central Repository is up.");
	}

}
