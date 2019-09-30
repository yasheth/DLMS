package serverinterface;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)

public interface LibraryInterface {
	
	String addItem(String managerID, String itemID, String itemName, int quantity);
	String removeItem(String managerID, String itemID, int quantity);
	String listAvailableItems(String managerID) ;
	
	String borrowItem(String userID, String itemID);
	String findItem(String userID, String itemName);
	String returnItem(String userID, String itemID);
	
	String addToWaitingList(String userID, String itemID);
	
	String exchangeItem(String userID, String newItemID, String oldItemID);
	String connect(String userID);
}
