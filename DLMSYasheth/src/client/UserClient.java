/**
 * 
 */
package client;


import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.Naming;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

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
	static LibraryInterface obj = null;
	static boolean valid = false;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Scanner input = new Scanner(System.in);
		System.out.println("Enter your user ID : ");
		userID = input.nextLine();
		if (userID.toUpperCase().charAt(3) == 'U' && userID.length() == 8) {
			if (userID.startsWith("CON")) {
				obj = (LibraryInterface) Naming.lookup("rmi://localhost:3333/CON");
				valid = true;
			} else if (userID.startsWith("MCG")) {
				obj = (LibraryInterface) Naming.lookup("rmi://localhost:4444/MCG");
				valid = true;
			} else if (userID.startsWith("MON")) {
				obj = (LibraryInterface) Naming.lookup("rmi://localhost:5555/MON");
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

			output=obj.connect(userID);
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
						output = obj.borrowItem(userID, itemID);

						if (output.equals("waitlist")) {
							System.out.println("\nBook " + itemID+ " not available. You want to be added to the waitlist? \nY for Yes. N for No.");
							String reply = input.nextLine();
							if (reply.equalsIgnoreCase("Y")) {
								System.out.println(obj.addToWaitlist(userID, itemID));
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
						output = obj.returnItem(userID, itemID);
						System.out.println(output);
						writeLog(output);
						break;

					case "3":
						System.out.println("\nEnter Book Name: ");
						itemName = input.nextLine();
						output = obj.findItem(userID, itemName);
						System.out.println("Result for Find "+itemName+"\n"+output);
						writeLog(output);
						break;
						
					case "4":
						System.out.println("Exchange Information");
						System.out.println("\nEnter Old Book ID: ");
						String oldItemID = input.nextLine();
						System.out.println("\nEnter Old Book ID: ");
						String newItemID = input.nextLine();
						output=obj.exchangeItem(userID, newItemID, oldItemID);
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
