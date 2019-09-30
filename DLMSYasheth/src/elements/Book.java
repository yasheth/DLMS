/**
 * 
 */
package elements;

/**
 * @author Yash Sheth
 *
 */
public class Book {
	private String itemName;
	private String itemID;
	private int itemQuantity;
	private boolean availabilty =false;
	
	/**
	 * @param itemID
	 * @param itemID
	 * @param itemQuantity
	 */
	public Book(String itemID,String itemName,int itemQuantity){
		this.itemID=itemID;
		this.itemName=itemName;
		this.itemQuantity=itemQuantity;
	}
	
	/**
	 * @return availability
	 */
	public Boolean getAvailabilty() {
		if(this.itemQuantity>0){
			availabilty = true;
		}
		return availabilty;
	}

	/**
	 * @param availabilty
	 */
	public void setAvailabilty(Boolean availabilty) {
		this.availabilty = availabilty;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return itemName;
	}

	/**
	 * @return getId
	 */
	public String getID() {
		return itemID;
	}
	/**
	 * @return noOfCopies
	 */
	public int getQuantity() {
		return itemQuantity;
	}
	
	/**
	 * @param name
	 */
	public void setName(String itemName) {
		this.itemName = itemName;
	}
	
	/**
	 * @param id
	 */
	public void setID(String itemID) {
		this.itemID = itemID;
	}
	
	/**
	 * @param noOfCopies
	 */
	public void setQuantity(int itemQuantity) {
		this.itemQuantity = itemQuantity;
	}
	
	/**
	 * Decrement available copies of books.
	 */
	public void reserveBook(){
		this.itemQuantity --;
	}
	
	/**
	 * Increment available copies of book for reservation.
	 */
	public void returnBook(){
		this.itemQuantity++;
	}
	
}
