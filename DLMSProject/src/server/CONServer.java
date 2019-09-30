/**
 * 
 */
package server;

import java.io.IOException;

import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import centralRepo.interfaces.HostEnum;
import centralRepo.interfaces.Repository;
import centralRepo.interfaces.RepositoryOperationEnum;
import elements.Book;
import server.interfaces.DBTypeEnum;
import server.interfaces.LibraryOperationsEnum;
import server.interfaces.OperationsEnum;
import serverinterface.GeneralException;
import serverinterface.LibraryInterface;

/**
 * @author Yash Sheth CONCS port 3010 CONSS port 3011
 */
public class CONServer implements LibraryInterface {

	static HashMap<String, Book> library = new HashMap<>();
	static HashMap<String, ArrayList<String>> waitlist = new HashMap<>();
	static HashMap<String, ArrayList<Book>> borrowers = new HashMap<>();
	static String serverNickName = "CON";
	private static int portCS = 3010;
	private static int portSS = 3011;
	private static HostEnum host = HostEnum.HOST_3;
	static String fileContent = "";
	static CONServer conserver = new CONServer();
	private static int expectedSequence = 1;
	static TreeMap<Integer,String> requestQueue=new TreeMap<>();
	private static DatagramSocket aSocket = null;

	public static void main(String args[]) throws Exception {
		Runnable task = () -> {
			try {
				//CS Communication
				conserver.activateServer();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
		Runnable task2 = () -> {
			try {
				//SS Communication
				conserver.initiateUDPSocket();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};

		Thread thread = new Thread(task);
		Thread thread2 = new Thread(task2);
		thread.start();
		thread2.start();
		// central repository.. register

		try (DatagramSocket aSocket = new DatagramSocket();) {
			byte[] dataByte = new byte[5000];
			System.out.println("Registering CONSS details with central repo.");
			String dataString = "ADD_LIBRARY_SERVER_DETAILS#" + serverNickName.concat("SS") + "#"
					+ HostEnum.HOST_3.name() + "#" + InetAddress.getLocalHost().getHostAddress() + "#" + portSS;
			DatagramPacket packet = new DatagramPacket(dataString.getBytes(), dataString.getBytes().length,
					InetAddress.getByName(Repository.CENTRAL_REPOSITORY_HOSTNAME), Repository.CENTRAL_REPOSITORY_PORT);
			aSocket.send(packet);
			packet = new DatagramPacket(dataByte, dataByte.length);
			aSocket.receive(packet);
			if (packet.getData().toString().equals("TRUE"))
				System.out.println("CONSS connection details are registered.");

			System.out.println("Registering CONCS details with central repo.");
			dataString = "ADD_LIBRARY_SERVER_DETAILS#" + serverNickName.concat("CS") + "#" + HostEnum.HOST_3.name()
					+ "#" + InetAddress.getLocalHost().getHostAddress() + "#" + portCS;
			packet = new DatagramPacket(dataString.getBytes(), dataString.getBytes().length,
					InetAddress.getByName(Repository.CENTRAL_REPOSITORY_HOSTNAME), Repository.CENTRAL_REPOSITORY_PORT);
			aSocket.send(packet);
			dataByte = new byte[5000];
			packet = new DatagramPacket(dataByte, dataByte.length);
			aSocket.receive(packet);
			if (packet.getData().toString().equals("TRUE"))
				System.out.println("CONCS connection details are registered.");

		} catch (SocketException e) {
			System.out.println("There is an error creating or accessing a Socket.");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			System.out.println("The IP address of a host could not be determined.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Issue with I/O over aSocket connection.");
			e.printStackTrace();
		}

		System.out.println("Concordia server is up.");
	}

	/**
	 * @throws AlreadyBoundException
	 * @throws IOException
	 * 
	 */
	private void activateServer() throws AlreadyBoundException, IOException {

		if (Files.exists(Paths.get("src/server/logs/" + serverNickName + "Log.txt"))) {
			conserver.writeLog("Concordia Server is Back Online!");
		} else {
			PrintWriter writer = new PrintWriter("src/server/logs/" + serverNickName + "Log.txt", "UTF-8");
			conserver.writeLog("Concordia Server is Online");
			writer.close();
		}
		System.out.println("Concordia Server is Online!");

		
		try {
			aSocket = new DatagramSocket(3030);
			byte[] buffer = new byte[100000];
			System.out.println("Concordia UDP Socket 3030 Initiated for inter library communication. CONCS");
			while (true) {
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				String requestMessage = new String(request.getData()).trim();
				System.out.println("REQUEST MESSAGE" + requestMessage);
				requestMessage = new String(request.getData(), 0, request.getLength(), "UTF-8");

				String[] dataArray = requestMessage.trim().split("#");
				int sequenceNumber = Integer.parseInt(dataArray[2]);
				
				if (checkSequence(sequenceNumber)) {
					performOperation(aSocket, dataArray);
				}else if(sequenceNumber>expectedSequence){
					requestQueue.put(sequenceNumber,requestMessage);
				}else{
					//do nothing. repeated UDP Message.
				}
				
			}
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket != null)
				aSocket.close();
		}
	}
	
	private void performOperation(DatagramSocket aSocket,String[] dataArray){
		LibraryOperationsEnum operation = LibraryOperationsEnum.valueOf(dataArray[3]);
		String feIP = dataArray[0];
		int fePort = Integer.parseInt(dataArray[1]);
		DatagramPacket replyPacket;
		boolean resultBool;
		String resultString = null;
		int resultInt;
		try {
			switch (operation) {

			case USER_EXISTS:
				System.out.println("Operation reuqested: USER_EXISTS");

				// check this

				resultBool = true;// userExists(dataArray[4]);
				resultString = resultBool ? "TRUE" : "FALSE";
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				aSocket.send(replyPacket);
				System.out.println("Result of operation : " + resultString);
				break;

			case ADD_ITEM:
				System.out.println("Operation reuqested: ADD_ITEM");
				resultBool = addItem(dataArray[4], dataArray[5], dataArray[6],
						Integer.parseInt(dataArray[7]));
				resultString = resultBool ? "TRUE" : "FALSE";
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				aSocket.send(replyPacket);
				System.out.println("Result of operation : " + resultString);
				break;

			case REMOVE_ITEM:
				System.out.println("Operation reuqested: REMOVE_ITEM");
				resultInt = removeItem(dataArray[4], dataArray[5], Integer.parseInt(dataArray[6]));
				resultString = String.valueOf(resultInt);
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				aSocket.send(replyPacket);
				System.out.println("Result of operation : " + resultString);
				break;

			case LIST_AVAILABLE_ITEM:
				System.out.println("Operation reuqested: LIST_AVAILABLE_ITEM");
				resultString = listAvailableItems(dataArray[4]);
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				aSocket.send(replyPacket);
				System.out.println("Result of operation : " + resultString);
				break;

			case BORROW_ITEM:
				System.out.println("Operation reuqested: BORROW_ITEM");
				resultInt = borrowItem(dataArray[4], dataArray[5]);
				resultString = String.valueOf(resultInt);
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				aSocket.send(replyPacket);
				System.out.println("Result of operation : " + resultString);
				break;

			case FIND_ITEM:
				System.out.println("Operation reuqested: FIND_ITEM");
				resultString = findItem(dataArray[4], dataArray[5]);
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				aSocket.send(replyPacket);
				System.out.println("Result of operation : " + resultString);
				break;

			case RETURN_ITEM:
				System.out.println("Operation reuqested: RETURN_ITEM");
				resultBool = returnItem(dataArray[4], dataArray[5]);
				resultString = resultBool ? "TRUE" : "FALSE";
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				aSocket.send(replyPacket);
				System.out.println("Result of operation : " + resultString);
				break;

			case ADD_TO_WAITING_LIST:
				System.out.println("Operation reuqested: ADD_TO_WAITING_LIST");
				resultBool = addToWaitingList(dataArray[4], dataArray[5]);
				resultString = resultBool ? "TRUE" : "FALSE";
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				aSocket.send(replyPacket);
				System.out.println("Result of operation : " + resultString);
				break;

			case ADD_TO_WAITING_LIST_OVERLOADED:
				System.out.println("Operation reuqested: ADD_TO_WAITING_LIST_OVERLOADED");
				resultBool = addToWaitingListOverloaded(dataArray[4], dataArray[5], dataArray[6]);
				resultString = resultBool ? "TRUE" : "FALSE";
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				aSocket.send(replyPacket);
				System.out.println("Result of operation : " + resultString);
				break;

			case EXCHANGE_ITEM:
				System.out.println("Operation reuqested: EXCHANGE_ITEM");
				resultInt = exchangeItem(dataArray[4], dataArray[5], dataArray[6]);
				resultString = String.valueOf(resultInt);
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				aSocket.send(replyPacket);
				System.out.println("Result of operation : " + resultString);
				break;

			default:
				System.out.println("Default Operation.");
				replyPacket = new DatagramPacket(new byte[0], 0, InetAddress.getByName(feIP), fePort);
				aSocket.send(replyPacket);
				System.out.println("Returning empty byte array.");
			}
			//updates local as well as other server sequence number
			updateSequence();

			//signals server to check their request Queue
			System.out.println("McGill : "+contactOtherServer("checkRequestQueue", 3021));
			System.out.println("Montreal : "+contactOtherServer("checkRequestQueue", 3031));
			
			//check local request Queue
			checkRequestQueue();
			
		} catch (IOException | GeneralException e) {
			GeneralException exception;
			if (!(e instanceof GeneralException)) {
				exception = new GeneralException(
						"Issue opening aSocket connection or sending data packet.");
			} else
				exception = (GeneralException) e;

			try {
				DatagramSocket sock = new DatagramSocket();
				String exceptionString = "EXCEPTION#" + exception.reason;
				DatagramPacket packet = new DatagramPacket(exceptionString.getBytes(),
						exceptionString.getBytes().length, InetAddress.getByName(feIP), fePort);
				sock.send(packet);
				sock.close();
			} catch (IOException e2) {
				System.out.println("Exception message IOException exception.");
				e2.printStackTrace();
			}
			e.printStackTrace();
		}

	}
	
	private void updateSequence(){
		expectedSequence++;
		System.out.println("McGill : "+contactOtherServer("updateSequence", 3021));
		System.out.println("Montreal : "+contactOtherServer("updateSequence", 3031));
	}
	
	private boolean checkSequence(int sequenceNumber) {
		if (sequenceNumber == expectedSequence) {
			return true;
		} else {
			return false;
		}
	}
	
	private void checkRequestQueue(){
		if(!requestQueue.isEmpty()){
			for(Map.Entry<Integer,String> entry : requestQueue.entrySet()) {
				  int key = entry.getKey();
				  if(key==expectedSequence){
					  String requestMessage=entry.getValue();
					  String[] dataArray = requestMessage.trim().split("#");
					  performOperation(aSocket, dataArray);
					  requestQueue.remove(key);
				  }
				}
		}
	}

	public synchronized String connect(String userID) {
		System.out.println("server connect request received from : " + userID);
		if (userID.startsWith("CON")) {
			System.out.println(userID + " connected to the server");
			fileContent = userID + " connected to the server";
			writeLog(fileContent);
			return "trueConnected to Concordia Library Server.\nWelcome " + userID;
		} else {
			System.out.println(userID + " failed to connect to the server. Unauthorized User.");
			fileContent = userID + " failed to connect to the server. Unauthorized User.";
			writeLog(fileContent);
			return "falsCould not Connect to Concordia Library Server.\nSorry " + userID;
		}
	}

	@Override
	public synchronized boolean addItem(String managerID, String itemID, String itemName, int quantity) {
		if (library.containsKey(itemID)) {
			Book newBookDetails = library.get(itemID);
			int newQuantity = newBookDetails.getQuantity() + quantity;
			newBookDetails.setQuantity(newQuantity);
			library.put(itemID, newBookDetails);
		} else {
			Book newBook = new Book(itemID, itemName, quantity);
			library.put(itemID, newBook);
		}
		fileContent = managerID + " added " + itemID + " :" + itemName + ", Quantity: " + quantity
				+ " ADDITEM - COMPLETED";
		writeLog(fileContent);
		if (waitlist.containsKey(itemID))
			checkWaitlist(itemID);
		return true;
	}

	@Override
	public synchronized int removeItem(String managerID, String itemID, int quantity) {
		if (library.containsKey(itemID)) {
			if (quantity == -1) {
				try {
					removeUser(itemID);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				library.remove(itemID);
				System.out.println("Successfully Removed " + itemID + " from Library.");
				fileContent = managerID + " Successfully Removed " + itemID + " from Library. REMOVEITEM - COMPLETED";
				writeLog(fileContent);
				return 1;
			} else if (quantity > library.get(itemID).getQuantity()) {
				System.out.println("Unable to Remove " + quantity + " copies of " + itemID + " from Library.");
				fileContent = managerID + " Failed to Remove " + quantity + " copies of " + itemID
						+ " from Library. REMOVEITEM - FAILED";
				writeLog(fileContent);
				return 0;
			} else {
				Book newBookDetails = library.get(itemID);
				int newQuantity = newBookDetails.getQuantity() - quantity;
				newBookDetails.setQuantity(newQuantity);
				library.put(itemID, newBookDetails);
				System.out.println("Successfully Removed " + quantity + " Copies of" + itemID + " from Library.");
				fileContent = managerID + " Successfully Removed " + quantity + " Copies of" + itemID
						+ " from Library. REMOVEITEM - COMPLETED";
				writeLog(fileContent);
				return 2;
			}
		} else {
			System.out.println("No Books matching the given ID.");
			fileContent = managerID + " failed to remove Book. No Books matching the given ID " + itemID
					+ " REMOVEITEM - FAILED";
			writeLog(fileContent);
			return 0;
		}

	}

	public void removeUser(String itemID) throws Exception {
		for (Entry<String, ArrayList<Book>> entry : borrowers.entrySet()) {
			ArrayList<Book> books = entry.getValue();
			for (Book b : books) {
				if (b.getID().equals(itemID)) {
					books.remove(b);
				}
			}
			borrowers.put(entry.getKey(), books);
		}
	}

	@Override
	public synchronized String listAvailableItems(String managerID) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n\n------LIST OF BOOKS IN LIBRARY------\n");
		for (HashMap.Entry<String, Book> entry : library.entrySet()) {
			Book book = entry.getValue();
			sb.append(book.getID() + "#" + book.getName() + "#" + book.getQuantity() + "@");
		}
		fileContent = managerID + " requested to view library inventory. LISTITEMS - COMPLETED";
		writeLog(fileContent);
		return sb.toString();
	}

	@Override
	public synchronized int borrowItem(String userID, String itemID) {
		if (library.containsKey(itemID)) {
			boolean borrowedFlag = false;

			if (borrowers.containsKey(userID)) {
				ArrayList<Book> newBookList = borrowers.get(userID);

				for (Book b : newBookList) {
					if (b.getID().equals(itemID)) {
						borrowedFlag = true;
						fileContent = userID + " already has " + itemID + " borrowed from the library. BORROW - FAILED";
						writeLog(fileContent);
						fileContent = userID + " already has " + itemID
								+ " borrowed from the library. Check your Reading List. Happy Reading!";
						return 0;
					}
				}
			}

			if (!borrowedFlag && library.get(itemID).getQuantity() > 0) {
				Book requiredBook = library.get(itemID);

				if (borrowers.containsKey(userID)) {
					ArrayList<Book> newBookList = borrowers.get(userID);
					requiredBook.reserveBook();
					newBookList.add(requiredBook);
					borrowers.put(userID, newBookList);
					fileContent = userID + " borrowed " + itemID + " from the library.  BORROW - COMPLETED";
					writeLog(fileContent);
					fileContent = userID + " borrowed " + itemID + " from the library. Happy Reading!";
					return 1;

				} else {
					requiredBook.reserveBook();
					ArrayList<Book> newBookList = new ArrayList<>();
					newBookList.add(requiredBook);
					borrowers.put(userID, newBookList);
					fileContent = userID + " borrowed " + itemID + " from the library.  BORROW - COMPLETED";
					writeLog(fileContent);
					fileContent = userID + " borrowed " + itemID + " from the library. Happy Reading!";
					return 1;
				}

			} else {
				fileContent = userID + " requested " + itemID
						+ " from the library. No copies available.  BORROW - FAILED";
				writeLog(fileContent);
				if (waitlist.containsKey(itemID)) {
					if (waitlist.get(itemID).contains(userID))
						return 0;
					else
						return 0;
				} else
					return 0;
			}
		} else if (itemID.startsWith("MCG")) {

			if (contactOtherServer("CHEC" + userID, 3021).equals("0")) {
				writeLog("\nBorrow Request forwarded to McGill Server");
				return Integer.parseInt(contactOtherServer("BORR" + userID + itemID, 3021));
			} else
				return 2;

		} else if (itemID.startsWith("MON")) {

			if (contactOtherServer("CHEC" + userID, 3031).equals("0")) {
				writeLog("\nBorrow Request forwarded to Montreal Server");
				return Integer.parseInt(contactOtherServer("BORR" + userID + itemID, 3031));
			} else
				return 2;

		} else {
			fileContent = userID + " requested " + itemID + " from the library. Item not found. BORROW - FAILED";
			writeLog(fileContent);
			return -1;
		}
	}

	@Override
	public synchronized String findItem(String userID, String itemName) {
		StringBuilder sb = new StringBuilder();
		fileContent = userID + " requested to find Book with title '" + itemName + "' from the library.";
		writeLog(fileContent);
		for (HashMap.Entry<String, Book> entry : library.entrySet()) {
			Book book = entry.getValue();
			if (book.getName().equals(itemName)) {
				sb.append(book.getID() + "#" + itemName + "#" + book.getQuantity() + "@");
			}
		}

		sb.append(contactOtherServer("FIND" + userID + itemName, 3021));
		sb.append(contactOtherServer("FIND" + userID + itemName, 3031));

		if (sb.toString().length() == 1)
			return "No Books with given Name found. Sorry.";
		else
			return sb.toString();
	}

	public synchronized String serverRequestFind(String userID, String itemName) {
		StringBuilder sb = new StringBuilder();
		fileContent = userID + " requested to find Book with title '" + itemName + "' from the library.";
		writeLog(fileContent);
		for (HashMap.Entry<String, Book> entry : library.entrySet()) {
			Book book = entry.getValue();
			if (book.getName().equals(itemName)) {
				sb.append(book.getID() + "#" + itemName + "#" + book.getQuantity() + "@");
			}
		}
		return sb.toString();
	}

	@Override
	public synchronized boolean returnItem(String userID, String itemID) {
		if (borrowers.containsKey(userID)) {
			for (Book b : borrowers.get(userID)) {
				if (b.getID().equals(itemID)) {
					ArrayList<Book> newBookList = borrowers.get(userID);
					newBookList.remove(b);
					library.get(itemID).returnBook();
					if (newBookList.size() == 0) {
						borrowers.remove(userID);
					}
					fileContent = userID + " returned " + itemID + " to the library. RETURN - COMPLETED";
					writeLog(fileContent);
					if (waitlist.containsKey(itemID))
						checkWaitlist(itemID);
					return true;
				}
			}
			fileContent = userID + " tried to return " + itemID
					+ " to the library but hasnt borrowed any book with matching ID. RETURN - FAILED";
			writeLog(fileContent);
			return false;
		} else if (itemID.startsWith("MCG")) {
			return Boolean.getBoolean(contactOtherServer("RETU" + userID + itemID, 3021));

		} else if (itemID.startsWith("MON")) {
			return Boolean.getBoolean(contactOtherServer("RETU" + userID + itemID, 3031));

		} else {
			fileContent = userID + " tried to return " + itemID
					+ " to the library but hasnt borrowed any book with matching ID. RETURN - FAILED";
			writeLog(fileContent);
			return false;
		}
	}

	@Override
	public synchronized boolean addToWaitingList(String userID, String itemID) {
		if (itemID.startsWith("MCG")) {
			contactOtherServer("WAIT" + userID + itemID, 3021);

		} else if (itemID.startsWith("MON")) {
			contactOtherServer("WAIT" + userID + itemID, 3031);

		} else if (waitlist.containsKey(itemID)) {
			ArrayList<String> newWaitlist = waitlist.get(itemID);
			newWaitlist.add(userID);
			waitlist.put(itemID, newWaitlist);
		} else {
			ArrayList<String> newWaitlist = new ArrayList<String>();
			newWaitlist.add(userID);
			waitlist.put(itemID, newWaitlist);
		}
		fileContent = userID + " added to waitlist of " + itemID + " in the library.";
		writeLog(fileContent);
		return true;
	}

	public synchronized void checkWaitlist(String itemID) {
		int newQuantity = library.get(itemID).getQuantity();
		ArrayList<String> queue = waitlist.get(itemID);

		if (newQuantity > 0) {
			for (int i = newQuantity; i > 0; i--) {
				if (!queue.isEmpty()) {
					if (!queue.get(0).startsWith(serverNickName) && borrowers.containsKey(queue.get(0))) {
						queue.remove(0);
						i++;
						continue;
					}
					if (borrowers.containsKey(queue.get(0))) {
						ArrayList<Book> newList = borrowers.get(queue.get(0));
						Book requiredBook = library.get(itemID);
						requiredBook.reserveBook();
						newList.add(requiredBook);
						borrowers.put(queue.get(0), newList);
					} else {
						ArrayList<Book> newList = new ArrayList<>();
						Book requiredBook = library.get(itemID);
						requiredBook.reserveBook();
						newList.add(requiredBook);
						borrowers.put(queue.get(0), newList);
					}
					System.out.println(queue.get(0) + " gets " + itemID);
					writeLog(queue.get(0) + "in the waitlist gets " + itemID);
					queue.remove(0);
				}
			}
		}

		if (queue.isEmpty())
			waitlist.remove(itemID);
		else
			waitlist.put(itemID, queue);
	}

	public synchronized int checkBorrowedBooks(String userID) {
		if (borrowers.containsKey(userID))
			return borrowers.get(userID).size();
		else
			return 0;
	}

	public synchronized void writeLog(String logData) {
		logData = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + " : " + logData + "\n";
		try {
			Files.write(Paths.get("src/server/logs/" + serverNickName + "Log.txt"), logData.getBytes(),
					StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String contactOtherServer(String message, int serverPort) {
		DatagramSocket aSocket = null;
		String response = "";
		try {
			aSocket = new DatagramSocket();
			InetAddress aHost = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(message.getBytes(), message.length(), aHost, serverPort);
			aSocket.send(request);
			System.out.println("Request message sent from the " + serverNickName + " client to server with port number "
					+ serverPort + " is: " + new String(request.getData()));
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

	private void initiateUDPSocket() throws NotBoundException {
		DatagramSocket aSocket = null;
		try {
			aSocket = new DatagramSocket(3011);
			byte[] buffer = new byte[100000];
			System.out.println("Concordia UDP Socket 3011 Initiated for inter library communication. CONSS");
			while (true) {
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				String requestMessage = new String(request.getData()).trim();
				String response = null;
				System.out.println("REQUEST MESSAGE" + requestMessage);
				requestMessage = new String(request.getData(), 0, request.getLength(), "UTF-8");
				if (requestMessage.contains("#")) {
					getData(request.getAddress(), request.getPort());
				} else {
					if (requestMessage.startsWith("FIND")) {

						response = serverRequestFind(requestMessage.substring(4, 12), requestMessage.substring(12));
					} else if (requestMessage.startsWith("BORR")) {
						response = String
								.valueOf(borrowItem(requestMessage.substring(4, 12), requestMessage.substring(12)));
					} else if (requestMessage.startsWith("WAIT")) {
						response = String.valueOf(
								addToWaitingList(requestMessage.substring(4, 12), requestMessage.substring(12)));
					} else if (requestMessage.startsWith("CHEC")) {
						response = String.valueOf(checkBorrowedBooks(requestMessage.substring(4)));
					} else if (requestMessage.startsWith("RETU")) {
						response = String
								.valueOf(returnItem(requestMessage.substring(4, 12), requestMessage.substring(12)));
					} else if (requestMessage.startsWith("OLDB")) {
						System.out.println("OLDB");
						System.out.println(requestMessage.substring(4, 12));
						System.out.println(requestMessage.substring(12));
						response = oldBookCheck(requestMessage.substring(4, 12), requestMessage.substring(12));
					} else if (requestMessage.startsWith("NEWB")) {
						System.out.println("NEWB");
						System.out.println(requestMessage.substring(4, 12));
						System.out.println(requestMessage.substring(12));
						response = newBookCheck(requestMessage.substring(4, 12), requestMessage.substring(12));
					} else if(requestMessage.equals("updateSequence")){
						expectedSequence++;
						response="Updated Sequence Number";
					}else {
						response = "Invalid Request Parameters.";
					}
				}
				System.out.println("RESPONSE SENT" + response);
				DatagramPacket reply = new DatagramPacket(response.getBytes(), response.length(), request.getAddress(),
						request.getPort());
				aSocket.send(reply);
			}
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket != null)
				aSocket.close();
		}
	}

	@Override
	public int exchangeItem(String userID, String oldItemID, String newItemID) throws GeneralException {
		boolean oldFlag = false, newFlag = false, validFlag = false;

		if (oldItemID.startsWith(serverNickName)) {
			if (oldBookCheck(userID, oldItemID).equals("true"))
				oldFlag = true;
		} else if (oldItemID.startsWith("MCG")) {
			if (contactOtherServer("OLDB" + userID + oldItemID, 3021).equals("true"))
				oldFlag = true;
		} else if (oldItemID.startsWith("MON")) {
			if (contactOtherServer("OLDB" + userID + oldItemID, 3031).equals("true"))
				oldFlag = true;
		}

		if (newItemID.startsWith(serverNickName)) {
			if (newBookCheck(userID, newItemID).equals("true"))
				newFlag = true;
		} else if (newItemID.startsWith("MCG")) {
			if (contactOtherServer("NEWB" + userID + newItemID, 3021).equals("true"))
				newFlag = true;
		} else if (newItemID.startsWith("MON")) {
			if (contactOtherServer("NEWB" + userID + newItemID, 3031).equals("true"))
				newFlag = true;
		}

		if (newItemID.substring(0, 3).equals(oldItemID.substring(0, 3))) {
			validFlag = true;
		} else {
			if (!newItemID.startsWith(serverNickName)) {
				if (newItemID.startsWith("MCG")) {
					if (contactOtherServer("CHEC" + userID, 3021).equals("0"))
						validFlag = true;
				}
				if (newItemID.startsWith("MON")) {
					if (contactOtherServer("CHEC" + userID, 3031).equals("0"))
						validFlag = true;
				}
			} else {
				validFlag = true;
			}
		}

		// MCGUser CON1234,MCG123 -> exchange MCG1234 CON0001 - fails already
		// have 1 book.
		// if same - no check | if different newOtherID check book borrowed
		// already (if 0 - valid else invalid)

		if (oldFlag && newFlag && validFlag) {
			fileContent = "Initiating Exchange Sequence";
			fileContent += returnItem(userID, oldItemID) + "\n";
			fileContent += borrowItem(userID, newItemID) + "\n";
			fileContent += "Exchange Sequence Successfull";
			writeLog(fileContent);
			return 1;
		} else {
			fileContent = "Exchange Sequence Failed. One or more requirements dont match. Try Again";
			writeLog(fileContent);
			return 0;
		}
	}

	public String oldBookCheck(String userID, String itemID) {
		if (itemID.startsWith(serverNickName)) {
			if (borrowers.containsKey(userID)) {
				for (Book b : borrowers.get(userID)) {
					if (b.getID().equals(itemID)) {
						return "true";
					}
				}
			}
		}
		return "false";
	}

	public String newBookCheck(String userID, String itemID) {

		if (library.containsKey(itemID)) {
			if (library.get(itemID).getQuantity() > 0) {
				if (borrowers.containsKey(userID)) {
					for (Book b : borrowers.get(userID)) {
						if (b.getID().equals(itemID)) {
							return "falsUser already has the new requested item";
						}
					}
					return "true";
				} else {
					return "true";
				}
			}
		}
		return "false";
	}

	@Override
	public boolean addToWaitingListOverloaded(String userID, String itemID, String oldItemID) throws GeneralException {
		return false;
	}

	public static boolean serverHandler(boolean restartServer) throws Exception {
		System.out.println("inside serverHandler() method.");
		System.out.println("parameter value is - restartServer: " + restartServer);

		System.out.println("Updating database.");
		List<String> serverDetails = null;
		try (DatagramSocket aSocket = new DatagramSocket()) {
			String request = RepositoryOperationEnum.GET_LIBRARY_SERVER_DETAILS.name() + "#CONCS";
			DatagramPacket packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
					InetAddress.getByName(Repository.CENTRAL_REPOSITORY_HOSTNAME), Repository.CENTRAL_REPOSITORY_PORT);
			aSocket.send(packet);
			byte[] replybuffer = new byte[5000];
			aSocket.receive(new DatagramPacket(replybuffer, replybuffer.length));
			String reply = new String(replybuffer);
			serverDetails = new ArrayList<>(Arrays.asList(reply.split("@")));
		} catch (SocketException e) {
			System.out.println("There is an error creating or accessing a Socket.");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			System.out.println("The IP address of a host could not be determined.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Issue with I/O over aSocket connection.");
			e.printStackTrace();
		}
		int thisHostDetailsIndex = -1;
		for (int i = 0; i < serverDetails.size(); i++) {
			if (serverDetails.get(i).contains(host.name()))
				thisHostDetailsIndex = i;
		}
		serverDetails.remove(thisHostDetailsIndex);
		conserver.updateDatabase(serverDetails.get(0), serverDetails.get(1));
		System.out.println("Database updated.");
		if (restartServer) {
			System.out.println("Restarting server and registering details with central repo.");
			main(null);
		}
		return true;
	}

	private boolean updateDatabase(String host1, String host2) {
		synchronized (this) {
			System.out.println("inside updateDatabase() method.");
			System.out.println("call parameters: host1-" + host1 + " host2-" + host2);
			String[] details = host1.split("#");
			String ip = details[1];
			int port = Integer.parseInt(details[2]);
			byte[] replyBytes = new byte[5000];
			String book_database_string = null, waiting_list_string = null, borrowed_books_string = null;
			try (DatagramSocket aSocket = new DatagramSocket()) {
				System.out.println("Invoking method on " + details[0] + " Condordia server.");
				String request = OperationsEnum.UPDATE_DB.name();
				DatagramPacket packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
						InetAddress.getByName(ip), port);
				aSocket.send(packet);
				packet = new DatagramPacket(replyBytes, replyBytes.length);
				aSocket.receive(packet);
				ip = packet.getAddress().getHostAddress();
				port = packet.getPort();
				System.out.println("temporary connection stablished " + details[0] + " Condordia server.");
				boolean serverReply = new String(replyBytes).equals("TRUE") ? true : false;

				if (serverReply) {

					// get book db values
					System.out.println("getting book database details.");
					request = DBTypeEnum.BOOK_DATABASE.name();
					packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
							InetAddress.getByName(ip), port);
					aSocket.send(packet);
					replyBytes = new byte[5000];
					aSocket.receive(new DatagramPacket(replyBytes, replyBytes.length));
					book_database_string = new String(replyBytes);
					System.out.println("server reply - " + book_database_string);
					// unmarshall it and update DB
					unmarshallData(DBTypeEnum.BOOK_DATABASE, book_database_string);

					// get borrow list values
					System.out.println("getting borrowed books database details.");
					request = DBTypeEnum.BORROW_LIST.name();
					packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
							InetAddress.getByName(ip), port);
					aSocket.send(packet);
					replyBytes = new byte[5000];
					aSocket.receive(new DatagramPacket(replyBytes, replyBytes.length));
					borrowed_books_string = new String(replyBytes);
					System.out.println("server reply - " + borrowed_books_string);
					// unmarshall it and update DB
					unmarshallData(DBTypeEnum.BORROW_LIST, borrowed_books_string);

					// get waiting values
					System.out.println("getting waiting list database details.");
					request = DBTypeEnum.WAITING_LIST.name();
					packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
							InetAddress.getByName(ip), port);
					aSocket.send(packet);
					replyBytes = new byte[5000];
					aSocket.receive(new DatagramPacket(replyBytes, replyBytes.length));
					waiting_list_string = new String(replyBytes);
					System.out.println("server reply - " + waiting_list_string);
					// unmarshall it and update DB
					unmarshallData(DBTypeEnum.WAITING_LIST, waiting_list_string);

					System.out.println("Server DB is updated.");
					// final reply to free host1 system out of synchronization
					request = "TRUE";
					packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
							InetAddress.getByName(ip), port);
					aSocket.send(packet);
				}

			} catch (SocketException e) {
				System.out.println("There is an error creating or accessing a Socket.");
				e.printStackTrace();
			} catch (UnknownHostException e) {
				System.out.println("The IP address of a host could not be determined.");
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Issue with I/O over Socket connection.");
				e.printStackTrace();
			}
		}

		return true;
	}

	public static void unmarshallData(DBTypeEnum dbTypeEnum, String data) {
		String[] dataArray = data.split("@");
		switch (dbTypeEnum) {

		case BOOK_DATABASE:
			String[] bookDetails;
			Book tempBook;
			library.clear();
			for (String record : dataArray) {
				bookDetails = record.split("#");
				tempBook = new Book(bookDetails[0], bookDetails[1], Integer.parseInt(bookDetails[3]));
				library.put(tempBook.getID(), tempBook);
			}
			System.out.println("Updated Books database.");

			break;

		case BORROW_LIST:
			String[] borrowedDetails;
			borrowers.clear();
			for (String record : dataArray) {
				borrowedDetails = record.split("#");
				for (int i = 1; i < borrowedDetails.length; i++) {
					if (borrowers.containsKey(borrowedDetails[i])) {
						ArrayList<Book> new_list = borrowers.get(borrowedDetails[1]);
						new_list.add(new Book(borrowedDetails[0]));
						borrowers.put(borrowedDetails[i], new_list);
					} else {
						ArrayList<Book> new_list = new ArrayList<>();
						new_list.add(new Book(borrowedDetails[0]));
						borrowers.put(borrowedDetails[i], new_list);
					}
				}
			}
			System.out.println("Updated borrowed books details.");

			break;

		case WAITING_LIST:
			String[] waitingDetails;
			ArrayList<String> waitingUserList;
			waitlist.clear();
			for (String record : dataArray) {
				waitingUserList = new ArrayList<>();
				waitingDetails = record.split("#");
				for (int i = 1; i < waitingDetails.length; i++) {
					waitingUserList.add(waitingDetails[i]);
				}
				waitlist.put(waitingDetails[0], waitingUserList);
			}
			System.out.println("Updated borrowed books details.");

			break;

		}

	}

	// from this server
	public static String marshallData(DBTypeEnum dbTypeEnum) {
		System.out.println("inside marshallData() method.");
		System.out.println("method params are: dbTypeEnum- " + dbTypeEnum.name());
		StringBuilder returnString = new StringBuilder();
		switch (dbTypeEnum) {
		case BOOK_DATABASE:
			for (Book book : library.values()) {
				returnString.append(book.getID() + "#").append(book.getName() + "#").append(book.getQuantity() + "@");
			}
			break;

		case BORROW_LIST:
			HashMap<String, List<String>> borrowedBooks = new HashMap<>();

			// convert borrowers to borrowedBooks
			for (String user : borrowers.keySet()) {
				for (Book b : borrowers.get(user)) {
					String bookid = b.getID();
					if (borrowedBooks.containsKey(bookid)) {
						List<String> new_list = borrowedBooks.get(bookid);
						new_list.add(user);
						borrowedBooks.put(bookid, new_list);
					} else {
						List<String> new_list = new ArrayList<>();
						new_list.add(user);
						borrowedBooks.put(bookid, new_list);
					}
				}
			}

			for (String bookId : borrowedBooks.keySet()) {
				returnString.append(bookId + "#");
				StringBuilder temp = new StringBuilder();
				for (String userId : borrowedBooks.get(bookId)) {
					temp.append(userId + "#");
				}
				returnString.append(temp.substring(0, temp.length() - 1)).append("@");
			}
			break;

		case WAITING_LIST:
			for (String bookId : waitlist.keySet()) {
				returnString.append(bookId + "#");
				StringBuilder temp = new StringBuilder();
				for (String userId : waitlist.get(bookId)) {
					temp.append(userId + "#");
				}
				returnString.append(temp.substring(0, temp.length() - 1)).append("@");
			}
			break;

		}
		System.out.println("returning result as " + returnString.substring(0, returnString.length() - 1));
		return returnString.substring(0, returnString.length() - 1);
	}

	public void getData(InetAddress address, int port) {
		synchronized (this) {
			System.out.println("inside getData() method.");
			System.out.println("reqest params: address-" + address.getHostAddress() + " port-" + port);
			try (DatagramSocket socket = new DatagramSocket()) {
				byte[] byteBuffer = new byte[5000];
				String replyString = "TRUE";
				DatagramPacket replyPacket = new DatagramPacket(replyString.getBytes(), replyString.getBytes().length,
						address, port);
				socket.send(replyPacket);
				System.out.println("sent reply as " + replyString);

				byteBuffer = new byte[5000];
				DatagramPacket requestPacket = new DatagramPacket(byteBuffer, byteBuffer.length);
				socket.receive(requestPacket);
				System.out.println("received request for " + replyPacket.getData().toString() + " database.");
				DBTypeEnum dbTypeEnum = DBTypeEnum.valueOf(new String(requestPacket.getData().toString()));
				replyString = marshallData(dbTypeEnum);
				replyPacket = new DatagramPacket(replyString.getBytes(), replyString.getBytes().length, address, port);
				System.out.println("replying as - " + replyString);
				socket.send(replyPacket);

				byteBuffer = new byte[5000];
				requestPacket = new DatagramPacket(byteBuffer, byteBuffer.length);
				socket.receive(requestPacket);
				System.out.println("received request for " + replyPacket.getData().toString() + " database.");
				dbTypeEnum = DBTypeEnum.valueOf(new String(requestPacket.getData().toString()));
				replyString = marshallData(dbTypeEnum);
				replyPacket = new DatagramPacket(replyString.getBytes(), replyString.getBytes().length, address, port);
				System.out.println("replying as - " + replyString);
				socket.send(replyPacket);

				byteBuffer = new byte[5000];
				requestPacket = new DatagramPacket(byteBuffer, byteBuffer.length);
				socket.receive(requestPacket);
				System.out.println("received request for " + replyPacket.getData().toString() + " database.");
				dbTypeEnum = DBTypeEnum.valueOf(new String(requestPacket.getData().toString()));
				replyString = marshallData(dbTypeEnum);
				replyPacket = new DatagramPacket(replyString.getBytes(), replyString.getBytes().length, address, port);
				System.out.println("replying as - " + replyString);
				socket.send(replyPacket);

				// waiting for final receive
				byteBuffer = new byte[5000];
				requestPacket = new DatagramPacket(byteBuffer, byteBuffer.length);
				socket.receive(requestPacket);
				if (requestPacket.getData().toString().equals("TRUE")) {
					System.out.println("request server database is updated.");
				}
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
		}
	}

}
