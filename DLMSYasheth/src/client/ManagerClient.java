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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import serverinterface.LibraryInterface;



/**
 * @author Yash Sheth
 *
 */
public class ManagerClient {

	static String userID = "raw";
	static boolean exit = false;
	static String itemID, itemName;
	static int quantity;
	static String output;
	static boolean valid = false;
	static LibraryInterface libraryInterface;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Scanner input = new Scanner(System.in);
		System.out.println("Enter your user ID : ");
		userID = input.nextLine();
		Service compService = null;
		if (userID.toUpperCase().charAt(3) == 'M' && userID.length() == 8) {
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
		} else {
			System.out.println("Invalid user ID. Run the client again. Goodbye.");
		}
		
		
		if (valid) {
			if (Files.exists(Paths.get("src/client/logs/" + userID + "Log.txt"))) {
				writeLog("Manager is Back Online!\n");
			} else {
				PrintWriter writer = new PrintWriter("src/client/logs/" + userID + "Log.txt", "UTF-8");
				writer.println(userID + " LOG FILE\nManager is Online");
				writer.close();
			}

			output = libraryInterface.connect(userID);
			if (output.startsWith("true")) {
				System.out.println(output.substring(4));
				output = output.substring(4);
				writeLog(output);
				while (!exit) {
					System.out.println("Please select the operation.");
					System.out.println("1. Add Books To Library ");
					System.out.println("2. Remove Books From Library");
					System.out.println("3. List Books In Library");
					System.out.println("4. Exit");
					String choice = input.nextLine();
					switch (choice) {

					case "1":
						System.out.println("\nEnter Book ID: ");
						itemID = input.nextLine();
						System.out.println("\nEnter Book Name: ");
						itemName = input.next();
						System.out.println("\nEnter No. of Quantities to Add: ");
						quantity = input.nextInt();
						input.nextLine();
						output = libraryInterface.addItem(userID, itemID, itemName, quantity);
						writeLog(output);
						System.out.println(output);
						exit = false;
						break;

					case "2":
						System.out.println("\nEnter Book ID: ");
						itemID = input.nextLine();
						System.out.println("\nEnter No. of Quantities to Remove: ");
						quantity = input.nextInt();
						input.nextLine();
						output = libraryInterface.removeItem(userID, itemID, quantity);
						writeLog(output);
						System.out.println(output);
						exit = false;
						break;

					case "3":
						output = libraryInterface.listAvailableItems(userID);
						System.out.println(output);
						writeLog(output);
						exit = false;
						break;

					case "4":
						System.out.println("Exited");
						exit = true;
						output = "Disconnected!";
						writeLog(output);
						System.out.println(output);
						input.close();
						System.exit(1);
						break;

					default:
						System.out.println("Choice should be in range of 1-3");
						exit = false;
						break;
					}
				}
			}
		}
	}

	public static void writeLog(String logData) throws IOException {
		logData = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + " : " + logData + "\n";
		Files.write(Paths.get("src/client/logs/" + userID + "Log.txt"), logData.getBytes(), StandardOpenOption.APPEND);
	}

}
