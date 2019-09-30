package serverinterface;

public interface LibraryInterface {
	
	boolean addItem(String managerID, String itemID, String itemName, int quantity) throws GeneralException;
	int removeItem(String managerID, String itemID, int quantity) throws GeneralException;
	String listAvailableItems(String managerID) throws GeneralException;
	
	int borrowItem(String userID, String itemID) throws GeneralException;
	String findItem(String userID, String itemName) throws GeneralException;
	boolean returnItem(String userID, String itemID) throws GeneralException;
	
	boolean addToWaitingList(String userID, String itemID) throws GeneralException;
	
	boolean addToWaitingListOverloaded(String userID, String itemID, String oldItemID) throws GeneralException;
	
	int exchangeItem(String userID, String newItemID, String oldItemID) throws GeneralException;
}
