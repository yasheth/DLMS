/**
 * 
 */
package client;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.Naming;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import serverinterface.LibraryInterface;



/**
 * @author Yash Sheth
 *
 */
public class UserClient {

	static String userID = "raw";
	static boolean exit = false;
	static String itemID, itemName;
	static int quantity;
	static String output;
	static LibraryInterface libraryInterface;
	static boolean valid = false;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		
		Scanner input = new Scanner(System.in);
		System.out.println("Enter your user ID : ");
		userID = input.nextLine();
		Service compService = null;
		if (userID.toUpperCase().charAt(3) == 'U' && userID.length() == 8) {
			if (userID.startsWith("CON")) {
				URL compURL = new URL("http://localhost:3000/comp?wsdl");
				QName compQName = new QName("http://server/", "CONServerService");
				compService = Service.create(compURL, compQName);
				libraryInterface = compService.getPort(LibraryInterface.class);
				valid = true;
			} else if (userID.startsWith("MCG")) {
				URL compURL = new URL("http://localhost:4000/comp?wsdl");
				QName compQName = new QName("http://server/", "MCGServerService");
				compService = Service.create(compURL, compQName);
				libraryInterface = compService.getPort(LibraryInterface.class);
				valid = true;
			} else if (userID.startsWith("MON")) {
				URL compURL = new URL("http://localhost:5000/comp?wsdl");
				QName compQName = new QName("http://server/", "MONServerService");
				compService = Service.create(compURL, compQName);
				libraryInterface = compService.getPort(LibraryInterface.class);
				valid = true;
			} else {
				System.out.println("Incorrect ID.");
			}
		}else{
			System.out.println("Invalid user ID. Run the client again. Goodbye.");
		}

		if (valid) {
			if (Files.exists(Paths.get("src/client/logs/" + userID + "Log.txt"))) {
				writeLog("User is Back Online!\n");
			} else {
				PrintWriter writer = new PrintWriter("src/client/logs/" + userID + "Log.txt", "UTF-8");
				writer.println(userID + " LOG FILE\nUser is Online");
				writer.close();
			}

			output=libraryInterface.connect(userID);
			if (output.startsWith("true")) {
				System.out.println(output.substring(4));
				output = output.substring(4);
				writeLog(output);
				while (!exit) {
					System.out.println("\nPlease select the operation.");
					System.out.println("1. Borrow Books From Library ");
					System.out.println("2. Return Books To Library");
					System.out.println("3. Find Book In Library");
					System.out.println("4. Exchange Book From Library");
					System.out.println("5. Exit");
					String choice = input.nextLine();
					switch (choice) {

					case "1":
						System.out.println("\nEnter Book ID: ");
						itemID = input.nextLine();
						output = libraryInterface.borrowItem(userID, itemID);

						if (output.equals("waitlist")) {
							System.out.println("\nBook " + itemID+ " not available. You want to be added to the waitlist? \nY for Yes. N for No.");
							String reply = input.nextLine();
							if (reply.equalsIgnoreCase("Y")) {
								System.out.println(libraryInterface.addToWaitingList(userID, itemID));
							} else {
								System.out.println("Sorry we couldnt serve you. Try again next time.");
								break;
							}
						} else {
							System.out.println(output);
							writeLog(output);
						}
						break;

					case "2":
						System.out.println("\nEnter Book ID: ");
						itemID = input.nextLine();
						output = libraryInterface.returnItem(userID, itemID);
						System.out.println(output);
						writeLog(output);
						break;

					case "3":
						System.out.println("\nEnter Book Name: ");
						itemName = input.nextLine();
						output = libraryInterface.findItem(userID, itemName);
						System.out.println("Result for Find "+itemName+"\n"+output);
						writeLog(output);
						break;
						
					case "4":
						System.out.println("Exchange Information");
						System.out.println("\nEnter Old Book ID: ");
						String oldItemID = input.nextLine();
						System.out.println("\nEnter Old Book ID: ");
						String newItemID = input.nextLine();
						output=libraryInterface.exchangeItem(userID, newItemID, oldItemID);
						System.out.println(output);
						writeLog(output);
						break;
						
					case "5":
						System.out.println("Exited");
						exit = true;
						output = "Disconnected!";
						System.out.println(output);
						writeLog(output);
						input.close();
						System.exit(1);
						break;

					default:
						System.out.println("Choice should be in range of 1-4");
						break;
					}
				}
			}
		}
	}
	
	public static void writeLog(String logData) throws IOException {
		logData = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + " : " + logData+"\n";
		Files.write(Paths.get("src/client/logs/" + userID + "Log.txt"), logData.getBytes(),StandardOpenOption.APPEND);
	}

}
