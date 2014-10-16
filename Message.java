package orgKTNet;

import java.util.HashMap;

/**
 * An instance of this class is a message which can be a knowledge request or a reply to the request.
 * @author linly
 * @version OrgKTNet 1.0
 */
public class Message {
	protected String receiver;
	protected String sender;
	public boolean isRequest;
	protected HashMap<Integer, Double> content = null;
	protected XClassifier baseCl = null;
	//protected int sendTime = 0;
	//protected int returnTime = 0;

	public Message(boolean requestOrNot, String receiver, String sender,
			HashMap<Integer, Double> areas, XClassifier cl) {
		isRequest = requestOrNot;
		this.receiver = receiver;
		this.sender = sender;
		content = areas;
		this.baseCl = cl;
		//this.sendTime = sTime;
		//this.returnTime = rTime;
	}

	public XClassifier getBaseCl() {
		return baseCl;
	}

	public HashMap<Integer, Double> getContent() {
		return content;
	}

	public String getFrom() {
		return sender;
	}

	/*public int getReturnTime() {
		return returnTime;
	}

	public int getSendTime() {
		return sendTime;
	}*/

	public String getTo() {
		return receiver;
	}
	
	/*public void setReturnTime(int rTime) {
		returnTime = rTime;
	}*/
}
