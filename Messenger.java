package orgKTNet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;

/**
 * This class handles the communication among agents' via knoweldge request and reply messages.
 * The inbox of an agent is implemented using linkedBlockingQueue which is a thread-safe data 

structure.
 * @author linly
 * @version OrgKTNet 1.0
 */
public class Messenger {
	public Queue<Message> inbox = new LinkedBlockingQueue<Message>();
	protected OrgMember host;

	public Messenger(OrgMember host) {
		this.host = host;
	}

	/**
	 * This agent plays the role of a knowledge source and answers a knowledge request by sending back a message under two conditions:
	 * (a) this agent is learning one or more of the same areas as the requester (Since I'm learning this area as well, we should keep in touch);
	 * (b) this agent possess that specialty which it is not currently improving, but the knowledge level is still higher than that of the requester. 
	 * when the recipient processes the message, it re-contacts this agent and then makes the transfer. @see OrgMetransferLearning()
	 * Tip: Create a separate answers HashMap instead of changing directly on the requests HashMap to avoid
	 * potential concurrency issues. Otherwise there will be concurrency exceptions.
	 * @param msg
	 * 		the original request message.
	 * @param RBPrefence
	 * 		the replier's reply behavior preference
	 */
	public void answerRequest(Message msg, int RBPref) {
		HashMap<Integer, Double> requests = msg.getContent();
		HashMap<Integer, Double> answers = new HashMap<Integer, Double> ();
		for (Map.Entry<Integer, Double> request : requests.entrySet()){
			int area = request.getKey();
			SpecialtyArea sa = host.myExpertise.get(area);
			if(sa != null){
				if ((sa.getState() == -1 || sa.getState() == 0) && host.getExpertise(area)>= request.getValue()){
					answers.put(area, host.getExpertise(area));
					//the moment this specific knowledge is visited, it is "used", no matter it will eventually be helpful or not, so setState(0)
					//but this condition only applies for the host's knowledge area that is not currently growing (not task-required)
					//task-required area knowledge remains to have state = 1
					sa.setState(0);
				}
				if(RBPref == 1 && (sa.getState() == 2 || sa.getState() == 1))
					answers.put(area, host.getExpertise(area));
			}
			else if(RBPref == 1){
				for(int i = 0; i < host.getTaskEnvironment().currentTask.length; i++)
					if(area == host.getTaskEnvironment().currentTask[i])
						answers.put(area, 0.0);
			}
		}
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		//in order to reduce the number of useless messages, only send out replies that include potentially helpful knowledge
		if(answers.size()>0)
			send(false, msg.getFrom(), answers, msg.getBaseCl());
	}

	/**
	 * Empty the inbox
	 */
	public void reset() {
		inbox.clear();
	}

	/**
	 * Create a message and then insert the message into the receiver's inbox.
	 * This method searches among all agents to find the specified receiver.
	 * @param isRequest
	 * 		indicates whether this message is a knowledge request (=true) or not (=false).
	 * @param receiver
	 * 		the receiver agent's ID
	 * @param answers
	 * 		indicates (a) the areas that the recipient needs more knowledge if the message is a knowledge request,
	 * 		and (b)the source's expertise in each task-required area if the message is the reply to a request.
	 * @param cl
	 * 		the related classifier whose condition and action parts indicate the knowledge source and recipient's
	 * 		network positions and their structural relation at the point when the request was sent out.
	 * @param sTime
	 * 		the time when the request is sent out
	 * @param rTime
	 * 		the time when the reply is sent out; the value of this parameter is zero if the message is a request.
	 */
	public void send(boolean isRequest, String receiver, HashMap<Integer, Double> answers, XClassifier cl) {
		Message msg = new Message(isRequest, receiver, host.getID(), answers, cl);
		Iterable<OrgMember> members = RunState.getInstance().getMasterContext().getObjects(OrgMember.class);
		for(OrgMember om: members)
			if(om.getID() == receiver){
				om.myMessenger.inbox.offer(msg);
				break;
			}
	}

}