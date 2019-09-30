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

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import repository.LibraryInterface;
import repository.LibraryInterfaceHelper;


/**
 * @author Yash Sheth
 *
 */
public class TestClientDLMS {

	static String userID = "raw";
	static LibraryInterface object1,object2 = null;
	static LibraryInterface object3 = null;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		/*
		 * TEST CASES
		 * Basics and Concurrency 
		 * TU1 MCG USER Borrow MCG0001,CON0001 Return MCG0001 Find java Borrow MCG1234
		 * TU2 CON USER Borrow CON0001, waitlist Y.
		 * TU1 MCG USER Return CON0001
		 * TU2 CON USER Return CON0001 DONE
		 *
		 * Basics
		 * TM CON Manager list, add, remove all, list DONE
		 * 
		 * Multithreaded and Atomicity
		 * Thread 1-MON USER Borrow MON0001 Return
		 * Thread 2-CON USER Borrow MON0001 Return
		 * 1 of above should fail
		 * 
		 * Exchange Conditions
		 * T1
		 * MCG USER with MCG1234
		 * Exchange MCG0001 MCG9999 - fails doesnt have MCG0001
		 * 
		 * T2
		 * MCG USER with MCG1234
		 * Exchange MCG1234 MCG1234 - already have MCG1234
		 * 
		 * T3
		 * MCG USER with MCG1234, MCG0001
		 * Exchange MCG0001 MCG1234 - already has MCG1234
		 * 
		 * T4
		 * MCG USER with MCG1234
		 * Exchange MCG1234 CON1234 - success
		 * 
		 * T5
		 * MCG USER with MCG1234,CON1234
		 * Exchange MCG1234 CON9999 - already have 1 book from CON
		 * 
		 * T6
		 * CON USER with MCG1234,CON1234
		 * Exchange MCG1234 MON0001- success
		 * 
		 * T7
		 * CON USER with MCG1234,CON1234
		 * Exchange MCG1234 MON0001- success
		 * 
		 * T8
		 * CON USER with MCG1234,CON1234
		 * Exchange MCG1234 CON- success
		 */
		
		Runnable task = () -> {
			try {
				ORB orb = ORB.init(args, null);
				org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
				NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
				object1 = (LibraryInterface) LibraryInterfaceHelper.narrow(ncRef.resolve_str("MCG"));
				object2 = (LibraryInterface) LibraryInterfaceHelper.narrow(ncRef.resolve_str("CON"));
				object3 = (LibraryInterface) LibraryInterfaceHelper.narrow(ncRef.resolve_str("MON"));
//				Basics and Concurrency 
//				TU1 MCGU0001 Borrow MCG0001,CON0001 Return MCG0001 Find java Borrow MCG1234
//				TU2 CONU1001 USER Borrow CON0001, waitlist Y.
//				TU1 MCGU0001 USER Return CON0001
//				TU2 CONU1001 USER Return CON0001

				userID="MCGU0001";
				if (Files.exists(Paths.get("src/client/logs/" + userID + "Log.txt"))) {
					writeLog("User is Back Online!\n");
				} else {
					PrintWriter writer = new PrintWriter("src/client/logs/" + userID + "Log.txt", "UTF-8");
					writer.println(userID + " LOG FILE\nUser is Online");
					writer.close();
				}
				userID="CONU1001";
				if (Files.exists(Paths.get("src/client/logs/" + userID + "Log.txt"))) {
					writeLog("User is Back Online!\n");
				} else {
					PrintWriter writer = new PrintWriter("src/client/logs/" + userID + "Log.txt", "UTF-8");
					writer.println(userID + " LOG FILE\nUser is Online");
					writer.close();
				}
				
				System.out.println("TESTING BASIC USER FUNCTIONS");
				printDash();
				System.out.println(object1.borrowItem("MCGU0001", "MCG0001"));
				System.out.println(object1.borrowItem("MCGU0001", "CON0001"));
				System.out.println("Return MCG0001 "+object1.returnItem("MCGU0001", "MCG0001"));
				System.out.println(object1.borrowItem("MCGU0001", "MCG8787"));
				System.out.println("Return MCG0001 "+object1.returnItem("MCGU0001", "MCG0001"));
				System.out.println("Find java \n"+object1.findItem("MCGU0001", "java"));
				printDash();
				System.out.println("Borrow CON0001. server response: "+object2.borrowItem("CONU1001", "CON0001"));
				System.out.println(object2.addToWaitlist("CONU1001", "CON0001"));
				printDash();
				System.out.println(object1.returnItem("MCGU0001", "CON0001"));
				printDash();
				System.out.println(object2.borrowItem("CONU1001", "CON0001"));
				System.out.println("Return CON0001 "+object2.returnItem("CONU1001", "CON0001"));
				printStar();

				userID="MONM7777";
				if (Files.exists(Paths.get("src/client/logs/" + userID + "Log.txt"))) {
					writeLog("Manager is Back Online!\n");
				} else {
					PrintWriter writer = new PrintWriter("src/client/logs/" + userID + "Log.txt", "UTF-8");
					writer.println(userID + " LOG FILE\nManager is Online");
					writer.close();
				}

				System.out.println("TESTING BASIC MANAGER FUNCTIONS");
				printDash();
				System.out.println(object3.addItem("MONM7777", "MON2020", "Montreal T20" , 10));
				System.out.println("List "+object3.listItemAvailability("MONM7777"));
				System.out.println(object3.removeItem("MONM7777", "MON2020",-1));
				System.out.println(object3.listItemAvailability("MONM7777"));
				printStar();
				
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		};
		

		Thread thread = new Thread(task);
		thread.start();
	}
		
	private static void printDash() {
		System.out.println("------------------------------------------------------------------------------------------------");
	}

	private static void printStar() {
		System.out.println("\n************************************************************************************************");
		System.out.println("************************************************************************************************\n");
	}
	
	public static void writeLog(String logData) throws IOException {
		logData = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + " : " + logData+"\n";
		Files.write(Paths.get("src/client/logs/" + userID + "Log.txt"), logData.getBytes(),StandardOpenOption.APPEND);
	}

}
