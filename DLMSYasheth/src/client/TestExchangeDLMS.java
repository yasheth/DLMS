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
public class TestExchangeDLMS {

	static String userID = "raw";
	static LibraryInterface object1,object2 = null;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		/*
		 * TEST CASES
		 * 
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
		 * MCG USER with MCG1234,CON1234 borrow again
		 * Exchange MCG1234 CON9999 - already have 1 book from CON
		 * 
		 * T6
		 * CON USER with MCG1234,CON1234
		 * Exchange MCG1234 MON0001- success
		 * 
		 * T7
		 * CON USER with MCG1234,CON1234
		 * Exchange CON1234 MCG1234- fail
		 * 
		 */
		
		ORB orb = ORB.init(args, null);
		org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
		NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
		object1 = (LibraryInterface) LibraryInterfaceHelper.narrow(ncRef.resolve_str("MCG"));
		object2 = (LibraryInterface) LibraryInterfaceHelper.narrow(ncRef.resolve_str("CON"));
		
		Runnable task = () -> {
			try {
				//T1-T5
				System.out.println(object1.borrowItem("MCGU0001", "MCG1234"));
				System.out.println();
				
				System.out.println("TEST 1");
				printDash();
				System.out.println(object1.exchangeItem("MCGU0001", "MCG0001","MCG9999"));
				printStar();
				System.out.println();
				System.out.println("TEST 2");
				printDash();
				System.out.println(object1.exchangeItem("MCGU0001", "MCG1234","MCG1234"));
				printStar();
				System.out.println();
				System.out.println("TEST 3");
				printDash();
				System.out.println(object1.borrowItem("MCGU0001", "MCG0001"));
				System.out.println(object1.exchangeItem("MCGU0001", "MCG0001","MCG1234"));
				printStar();
				System.out.println();
				System.out.println("TEST 4");
				printDash();
				System.out.println(object1.exchangeItem("MCGU0001", "MCG1234","CON1234"));
				printStar();
				System.out.println();
				
				System.out.println("TEST 5");
				printDash();
				System.out.println(object1.borrowItem("MCGU0001", "MCG1234"));
				System.out.println(object1.exchangeItem("MCGU0001", "MCG1234","CON9999"));
				printStar();
				System.out.println();
				
				System.out.println(object2.borrowItem("CONU0001", "MCG1234"));
				System.out.println(object2.borrowItem("CONU0001", "CON1234"));
				System.out.println();
				
				System.out.println("TEST 6");
				printDash();
				System.out.println(object2.exchangeItem("CONU0001", "MCG1234","MON0001"));
				printStar();
				System.out.println();
				
				System.out.println("TEST 7");
				printDash();
				System.out.println(object2.exchangeItem("CONU0001", "CON1234","MON1234"));
				printStar();
				System.out.println();
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		};
		
		Runnable task2 = () -> {
			try {
				

			} catch (Exception e) {

				e.printStackTrace();
			}
		};

		

		Thread thread = new Thread(task);
		Thread thread2 = new Thread(task2);
		thread2.start();
		thread.start();
	}
		
	private static void printDash() {
		System.out.println("------------------------------------------------------------------------------------------------");
	}

	private static void printStar() {
		System.out.println("************************************************************************************************");
	}
	
	public static void writeLog(String logData) throws IOException {
		logData = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + " : " + logData+"\n";
		Files.write(Paths.get("src/client/logs/" + userID + "Log.txt"), logData.getBytes(),StandardOpenOption.APPEND);
	}

}
