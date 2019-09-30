package centralrepository.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RepositoryImpl {
	private static final Logger log = LogManager.getLogger(RepositoryImpl.class);
	private static final HashMap<String, HashSet<String>> libServers = new LinkedHashMap<>();;
	private static RepositoryImpl repoDB;

	private RepositoryImpl() {

	}

	public static RepositoryImpl getRepoDB() {
		if (repoDB == null)
			repoDB = new RepositoryImpl();

		return repoDB;
	}

	/**
	 * @param libraryConnectionCode
	 *            : Library connection name as CONCS or CONSS where CONCS is for CON
	 *            client-server comm and CONSS is for server-server comm or RM for replica manager or SQ  for sequencer.
	 * @param hostName
	 *            : replica number as host1, host2 or host3.
	 * @param ip
	 *            : host ip address.
	 * @param port:
	 *            : host port no.
	 * @return
	 */
	public boolean addLibraryServerDetails(String libraryConnectionCode, String hostName, String ip, int port) {
		synchronized (this) {
			log.debug("inside addLibraryServerDetails()method.");
			log.debug("with params as - libraryConnectionCode: " + libraryConnectionCode + " hostName: " + hostName
					+ " ip: " + ip + " port: " + port);
			String connectionString = null, key = null;
			if (libraryConnectionCode.endsWith("CS")) {
				key = libraryConnectionCode;
				connectionString = new String(hostName + "#" + ip + "#" + port);
			} else if (libraryConnectionCode.endsWith("SS")) {
				key = hostName;
				connectionString = new String(libraryConnectionCode + "#" + ip + "#" + port);
			} else if(libraryConnectionCode.startsWith("RM")) {
				key = libraryConnectionCode;
				connectionString = new String(hostName + "#" + ip + "#" + port);
			} else if(libraryConnectionCode.startsWith("SQ")) {
				key = libraryConnectionCode;
				connectionString = new String(ip + "#" + port);
			}

			if (libServers.containsKey(key) && libServers.get(key) != null) {
				libServers.get(key).add(connectionString);
			} else {
				HashSet<String> set = new HashSet<>();
				set.add(connectionString);
				libServers.put(key, set);
			}
			log.debug("returning true.");
			return true;
		}
	}
	/**
	 * 
	 * @param key
	 *            : It can either like CONCS/MONCS/MCGCS or HOST1/HOST2/HOST3
	 * @return : reply will be something like
	 *         Host1#<ip>#<port>@Host2#<ip>#<port>@Host3#<ip>#<port> if key is
	 *         CONCS/MONCS/MCGCS or
	 *         CONSS#<ip>#<port>@MCGSS#<ip>#<port>@MONSS#<ip>#<port> if key is
	 *         HOST1/HOST2/HOST3 with corresponding ip/port
	 */
	public String getLibraryServerDetails(String key) {
		synchronized (this) {
			log.debug("inside getLibraryServerDetails() method.");
			log.debug("with params as - key: " + key);
			String connectionStrings = "";
			if (libServers.containsKey(key)) {
				for (String s : libServers.get(key)) {
					connectionStrings = connectionStrings.concat(s).concat("@");
				}
				connectionStrings = connectionStrings.substring(0, connectionStrings.length() - 1);
			}
			log.debug("returning " + connectionStrings);
			return connectionStrings;
		}
	}

}
