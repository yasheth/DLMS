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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Endpoint;

import elements.Book;
import serverinterface.LibraryInterface;

/**
 * @author Yash Sheth
 *
 */
@WebService(endpointInterface = "serverinterface.LibraryInterface")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class CONServer implements LibraryInterface {

	static HashMap<String, Book> library = new HashMap<>();
	static HashMap<String, ArrayList<String>> waitlist = new HashMap<>();
	static HashMap<String, ArrayList<Book>> borrowers = new HashMap<>();
	static String serverNickName = "CON";
	static String fileContent = "";
	static CONServer conserver = new CONServer();

	public static void main(String args[]) throws Exception {
		Runnable task = () -> {
			try {
				conserver.activateServer(args);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
		Runnable task2 = () -> {
			try {
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
	}

	/**
	 * @throws AlreadyBoundException
	 * @throws IOException
	 * 
	 */
	private static void activateServer(String args[]) throws AlreadyBoundException, IOException {
		if (Files.exists(Paths.get("src/server/logs/" + serverNickName + "Log.txt"))) {
			conserver.writeLog("Concordia Server is Back Online!\n");
		} else {
			PrintWriter writer = new PrintWriter("src/server/logs/" + serverNickName + "Log.txt", "UTF-8");
			conserver.writeLog("Concordia Server is Online");
			writer.close();
		}
		System.out.println("Concordia Server is Online!");
		System.out.println(conserver.addItem("Server", "CON0001", "init", 1));
		System.out.println(conserver.addItem("Server", "CON9999", "con", 10));
		System.out.println(conserver.addItem("Server", "CON1234", "java", 100));
		Endpoint endpoint = Endpoint.publish("http://localhost:3000/comp", conserver);

	}

	@Override
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
	public synchronized String addItem(String managerID, String itemID, String itemName, int quantity) {
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
		return fileContent;
	}

	@Override
	public synchronized String removeItem(String managerID, String itemID, int quantity) {
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
			} else if (quantity > library.get(itemID).getQuantity()) {
				System.out.println("Unable to Remove " + quantity + " copies of " + itemID + " from Library.");
				fileContent = managerID + " Failed to Remove " + quantity + " copies of " + itemID
						+ " from Library. REMOVEITEM - FAILED";
				writeLog(fileContent);
			} else {
				Book newBookDetails = library.get(itemID);
				int newQuantity = newBookDetails.getQuantity() - quantity;
				newBookDetails.setQuantity(newQuantity);
				library.put(itemID, newBookDetails);
				System.out.println("Successfully Removed " + quantity + " Copies of" + itemID + " from Library.");
				fileContent = managerID + " Successfully Removed " + quantity + " Copies of" + itemID
						+ " from Library. REMOVEITEM - COMPLETED";
				writeLog(fileContent);
			}
		} else {
			System.out.println("No Books matching the given ID.");
			fileContent = managerID + " failed to remove Book. No Books matching the given ID " + itemID
					+ " REMOVEITEM - FAILED";
			writeLog(fileContent);
		}
		return fileContent;
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
			sb.append(book.getID() + " :" + book.getName() + " , Quantity:- " + book.getQuantity() + "\n");
		}
		fileContent = managerID + " requested to view library inventory. LISTITEMS - COMPLETED";
		writeLog(fileContent);
		return sb.toString();
	}

	@Override
	public synchronized String borrowItem(String userID, String itemID) {
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
						return fileContent;
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
					return fileContent;

				} else {
					requiredBook.reserveBook();
					ArrayList<Book> newBookList = new ArrayList<>();
					newBookList.add(requiredBook);
					borrowers.put(userID, newBookList);
					fileContent = userID + " borrowed " + itemID + " from the library.  BORROW - COMPLETED";
					writeLog(fileContent);
					fileContent = userID + " borrowed " + itemID + " from the library. Happy Reading!";
					return fileContent;
				}

			} else {
				fileContent = userID + " requested " + itemID
						+ " from the library. No copies available.  BORROW - FAILED";
				writeLog(fileContent);
				if (waitlist.containsKey(itemID)) {
					if (waitlist.get(itemID).contains(userID))
						return "Already in waitlist for the item";
					else
						return "waitlist";
				} else
					return "waitlist";
			}
		} else if (itemID.startsWith("MCG")) {

			if (contactOtherServer("CHEC" + userID, 4444).equals("0")) {
				writeLog("\nBorrow Request forwarded to McGill Server");
				return contactOtherServer("BORR" + userID + itemID, 4444);
			} else
				return "You already have borrowed 1 book from McGill Library.";

		} else if (itemID.startsWith("MON")) {

			if (contactOtherServer("CHEC" + userID, 5555).equals("0")) {
				writeLog("\nBorrow Request forwarded to Montreal Server");
				return contactOtherServer("BORR" + userID + itemID, 5555);
			} else
				return "You already have borrowed 1 book from Montreal Library.";

		} else {
			fileContent = userID + " requested " + itemID + " from the library. Item not found. BORROW - FAILED";
			writeLog(fileContent);
			return fileContent;
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
				sb.append(book.getID() + " : " + itemName + ", Quantity:- " + book.getQuantity() + "\n");
			}
		}

		sb.append(contactOtherServer("FIND" + userID + itemName, 4444));
		sb.append("\n" + contactOtherServer("FIND" + userID + itemName, 5555));

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
				sb.append(book.getID() + " : " + itemName + ", Quantity:- " + book.getQuantity() + "\n");
			}
		}
		return sb.toString();
	}

	@Override
	public synchronized String returnItem(String userID, String itemID) {
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
					return "Book returned Successfully.";
				}
			}
			fileContent = userID + " tried to return " + itemID
					+ " to the library but hasnt borrowed any book with matching ID. RETURN - FAILED";
			writeLog(fileContent);
			return "You havent borrowed any book with " + itemID + " ID.";
		} else if (itemID.startsWith("MCG")) {
			return contactOtherServer("RETU" + userID + itemID, 4444);

		} else if (itemID.startsWith("MON")) {
			return contactOtherServer("RETU" + userID + itemID, 5555);

		} else {
			fileContent = userID + " tried to return " + itemID
					+ " to the library but hasnt borrowed any book with matching ID. RETURN - FAILED";
			writeLog(fileContent);
			return "You havent borrowed any books at the moment.";
		}
	}

	@Override
	public synchronized String addToWaitingList(String userID, String itemID) {
		if (itemID.startsWith("MCG")) {
			contactOtherServer("WAIT" + userID + itemID, 4444);

		} else if (itemID.startsWith("MON")) {
			contactOtherServer("WAIT" + userID + itemID, 5555);

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
		return fileContent;
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
			aSocket = new DatagramSocket(3333);
			byte[] buffer = new byte[100000];
			System.out.println("Concordia UDP Socket 3333 Initiated for inter library communication");
			while (true) {
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				String requestMessage = new String(request.getData()).trim();
				String response = null;
				System.out.println("REQUEST MESSAGE" + requestMessage);
				requestMessage = new String(request.getData(), 0, request.getLength(), "UTF-8");
				if (requestMessage.startsWith("FIND")) {

					response = serverRequestFind(requestMessage.substring(4, 12), requestMessage.substring(12));
				} else if (requestMessage.startsWith("BORR")) {
					response = borrowItem(requestMessage.substring(4, 12), requestMessage.substring(12));
				} else if (requestMessage.startsWith("WAIT")) {
					response = addToWaitingList(requestMessage.substring(4, 12), requestMessage.substring(12));
				} else if (requestMessage.startsWith("CHEC")) {
					response = String.valueOf(checkBorrowedBooks(requestMessage.substring(4)));
				} else if (requestMessage.startsWith("RETU")) {
					response = returnItem(requestMessage.substring(4, 12), requestMessage.substring(12));
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
				} else {
					response = "Invalid Request Parameters.";
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
	public String exchangeItem(String userID, String oldItemID, String newItemID) {
		boolean oldFlag = false, newFlag = false, validFlag = false;

		if (oldItemID.startsWith(serverNickName)) {
			if (oldBookCheck(userID, oldItemID).equals("true"))
				oldFlag = true;
		} else if (oldItemID.startsWith("MCG")) {
			if (contactOtherServer("OLDB" + userID + oldItemID, 4444).equals("true"))
				oldFlag = true;
		} else if (oldItemID.startsWith("MON")) {
			if (contactOtherServer("OLDB" + userID + oldItemID, 5555).equals("true"))
				oldFlag = true;
		}

		if (newItemID.startsWith(serverNickName)) {
			if (newBookCheck(userID, newItemID).equals("true"))
				newFlag = true;
		} else if (newItemID.startsWith("MCG")) {
			if (contactOtherServer("NEWB" + userID + newItemID, 4444).equals("true"))
				newFlag = true;
		} else if (newItemID.startsWith("MON")) {
			if (contactOtherServer("NEWB" + userID + newItemID, 5555).equals("true"))
				newFlag = true;
		}

		if (newItemID.substring(0, 3).equals(oldItemID.substring(0, 3))) {
			validFlag = true;
		} else {
			if (!newItemID.startsWith(serverNickName)) {
				if (newItemID.startsWith("MCG")) {
					if (contactOtherServer("CHEC" + userID, 4444).equals("0"))
						validFlag = true;
				}
				if (newItemID.startsWith("MON")) {
					if (contactOtherServer("CHEC" + userID, 5555).equals("0"))
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
			return fileContent;
		} else {
			fileContent = "Exchange Sequence Failed. One or more requirements dont match. Try Again";
			writeLog(fileContent);
			return fileContent;
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
}
