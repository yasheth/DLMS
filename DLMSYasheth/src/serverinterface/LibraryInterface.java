/**
 * 
 */
package serverinterface;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Yash Sheth
 *
 */
public interface LibraryInterface extends Remote{
	String connect(String message)throws RemoteException, IOException;
	String addItem (String managerID,String itemID,String itemName,int quantity)throws RemoteException, IOException;
	String removeItem (String managerID,String itemID,int quantity)throws RemoteException, IOException;
	String listItemAvailability (String managerID)throws RemoteException, IOException;
	String borrowItem (String userID,String itemID)throws RemoteException, IOException, NotBoundException;
	String findItem (String userID,String itemName)throws RemoteException, IOException, NotBoundException;
	String returnItem (String userID,String itemID)throws RemoteException, IOException, NotBoundException;
	String exchangeItem (String userID,String newItemID,String oldItemID)throws RemoteException, IOException, NotBoundException;
	String oldBookCheck(String userID,String itemID)throws RemoteException, IOException, NotBoundException;
	String newBookCheck(String userID,String itemID)throws RemoteException, IOException, NotBoundException;
	String addToWaitlist(String userID, String itemID)throws RemoteException, IOException, NotBoundException;
	String serverRequestFind(String userID, String itemName) throws RemoteException, IOException, NotBoundException;
	int checkBorrowedBooks(String userID)throws RemoteException, IOException, NotBoundException;
}
