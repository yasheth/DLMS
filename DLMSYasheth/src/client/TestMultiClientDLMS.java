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
public class TestMultiClientDLMS {

	static String userID = "raw";
	static LibraryInterface object1, object2 = null;
	static LibraryInterface object3 = null;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		/*
		 * TEST CASES 
		 * Multithreaded and Atomicity 
		 * Thread 1-MON USER Borrow MON0001 Return 
		 * Thread 2-CON USER Borrow MON0001 Return 
		 * T2 of above should fail coz of wait and output should be waitlist
		 */

		ORB orb = ORB.init(args, null);
		org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
		NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
		object1 = (LibraryInterface) LibraryInterfaceHelper.narrow(ncRef.resolve_str("MON"));
		object2 = (LibraryInterface) LibraryInterfaceHelper.narrow(ncRef.resolve_str("CON"));

		Runnable task = () -> {
			try {
				printDash();
				System.out.println(object1.borrowItem("MONU0001", "MON0001"));
				new java.util.Timer().schedule( 
				        new java.util.TimerTask() {
				            @Override
				            public void run() {
				            	System.out.println(object1.returnItem("MONU0001", "MON0001"));
				            }
				        }, 
				        1000 
				);
				printDash();

			} catch (Exception e) {

				e.printStackTrace();
			}
		};

		Runnable task2 = () -> {
			try {
				printStar();
				System.out.println(object2.borrowItem("CONU0001", "MON0001"));
				System.out.println(object2.returnItem("CONU0001", "MON0001"));
				printStar();

			} catch (Exception e) {

				e.printStackTrace();
			}
		};

		Thread thread = new Thread(task);
		Thread thread2 = new Thread(task2);
		thread.start();
		thread2.start();
	}

	private static void printDash() {
		System.out.println("------------------------------------------------------------------------------------------------");
	}

	private static void printStar() {
		System.out.println("************************************************************************************************");
		
	}

	public static void writeLog(String logData) throws IOException {
		logData = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + " : " + logData + "\n";
		Files.write(Paths.get("src/client/logs/" + userID + "Log.txt"), logData.getBytes(), StandardOpenOption.APPEND);
	}

}
