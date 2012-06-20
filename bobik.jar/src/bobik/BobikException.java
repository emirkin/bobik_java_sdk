package bobik;

/**
 * BobikException - A simple wrapper around java.lang.Exception
 *
 */
public class BobikException extends Exception {
	private static final long serialVersionUID = 3991890458175163776L;
	public BobikException(Exception e) {
		
	}
	
	public BobikException(String errMessage) {
		super(errMessage, new Exception(errMessage));
	}
	
	public BobikException(String errMessage, Exception e) {
		super(errMessage, e);
	}
}
