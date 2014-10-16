package orgKTNet;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;


/**
 * Each instance of this class is an agent who represents an organizational member.
 * The class constructs, assigns tasks to, and initialize the expertise of an agent.
 * It handles the process in which one single agent pursues the max level of knowledge in every task-required area by acquiring knowledge from others.
 * It also reduces unused knowledge (knowledge obsolescence).
 * 
 * @author Yuan Lin
 * @version OrgKTNet 1.0
 */
public class OrgMember {

	/**
	 * start from "1"
	 */
	protected String myID = null;
	/**
	 * An agent's expertise is a bounded sorted map of specialty areas (the key is the index of an area and the value is a specialty-area class).
	 * I use this data structure because both the number and the contents of an agent's specialty areas are continuously changing.
	 */
	protected BoundedSortedMap<Integer, SpecialtyArea> myExpertise = null;
	/**
	 * The amount of increased learning skills every time an agent executes self-learning.
	 * It indicate how fast an organizational member can gain knowledge by self-learning.
	 * The rate of knowledge gain from knowledge transfer is some constant times more than this value.
	 * The rate of knowledge decay equals to the rate of knowledge gain.
	 */
	protected double rateOfSelfGain = 0.0;
	/**
	 * The agent acts on and gets rewards/feedback from its local environment, which
	 * reflects changes in the current tasks and network environment of the agent.
	 * @see Environment
	 */
	protected Environment taskEnv = null;
	public boolean idle = false;
	/**
	 * The agent communicates with other agents through a messenger
	 * @see Messenger.java, Message.java
	 */
	public Messenger myMessenger = null;
	/**
	 * The agent decides and adapts its knowledge-transfer behavior through a XCS
	 * @see XCS.java
	 */
	protected XCS xcs = null;
	/**
	 * Social capital preference; preference on utilizing bridging (=0) or bonding (=1) social capital
	 */
	protected int SCPreference;
	/**
	 * Nodal degree preference; preference on whether to allow for (=1) or avoid (=0) more connections
	 */
	protected int NDPreference;
	/**
	 * Reply behavior preference; preference on whether to reply only when currently has more expertise (=0) or also when it is learning that knowledge (=1)
	 */
	protected int RBPreference;
	/**
	 * Record an agent's strategy change (i.e., classifier action 0-7) while it carries out the current task.
	 * Record a new strategy only when it is different from the previous one 
	 */
	public LinkedList<Integer> strategyChain = null;

	/**
	 * Construct an agent without assigning task and initializing expertise.
	 * Both will be conducted outside this method
	 * since every new problem demands a new round of task assignment. 
	 * @param context
	 * 			The context that contains all agents
	 * @param index
	 * 			The organization-wide expert index
	 * @param memberID
	 * 			The assigned ID of this agent
	 */
	public OrgMember(Context<Object> context, ExpertIndex index, int memberID, double [] avgTimeCost) {
		myMessenger = new Messenger(this);
		taskEnv = new Environment(this, index, avgTimeCost);
		int maxAreaNum = (Integer) RunEnvironment.getInstance().getParameters().getValue("memberArea");
		myExpertise = new BoundedSortedMap<Integer, SpecialtyArea>(maxAreaNum);
		myID = Integer.toString(memberID);
		setSCPreference((Constants.drand() < 0.5)? 0:1);
		setNDPreference((Constants.drand() < 0.5)? 0:1);
		setRBPreference((Constants.drand() < 0.5)? 0:1);
		strategyChain = new LinkedList<Integer>();
		strategyChain.add(getSCPreference()*4 + getNDPreference()*2 + getRBPreference());
		if(Constants.switch_DKS == 2)
			xcs = new XCS(taskEnv);
		if(Constants.switch_SGR == true)
			rateOfSelfGain = Constants.selfGain;
		else
			rateOfSelfGain = RandomHelper.createNormal(Constants.selfGain, 0.1).nextDouble();
	}
	
	public OrgMember(){
	}

	/**
	 * Reduce the level of UNUSED knowledge at each specialty area.
	 * The rate of forgetting is equal to the rate of self learning.
	 */
	public void decayExpertise() {
		/*for (SpecialtyArea sa : myExpertise.values())
			if(sa.state != -1 && sa.decayKnow(this.rateOfSelfGain*Constants.forgetLearnRatio)< Constants.smallValue)
				sa.currentLevel = 0;
		*/
		ArrayList<Integer> removeList = new ArrayList<Integer> ();
		SpecialtyArea sa = null;
		for (Entry<Integer, SpecialtyArea> expertise : myExpertise.entrySet()){
			sa = expertise.getValue();
			if(sa.getState() == -1){ 
				double afterDecay = sa.decayKnow(this.rateOfSelfGain*Constants.forgetLearnRatio);
				if(afterDecay < Constants.smallValue)
					removeList.add(expertise.getKey());
			}
			//if(sa.getState()== 0), this knowledge has been used in this step but not task-required, it goes back to -1 
			if(sa.getState()== 0) sa.setState(-1);
			//if(sa.getState()==1), this knowledge is task-required, it goes back to 2(next step it may or may not get setState(1)
			if(sa.getState()==1) sa.setState(2);
		}
		for(Integer i : removeList)
			myExpertise.remove(i);
	}

	/**
	 * When an agent completes its task, this method changes the agent's state to idle,
	 * adds the agent to the expert index (cause it now has the maximum level of knowledge at each required area),
	 * accumulates the agent's time cost to the organizational level recorder, and reduces the total number of agents who are still working.
	 * The action set of this agent's XCS  will also be finalized with an normalizing factor. @see XClassifierSet#update(double).
	 * The method is protected and is only called inside two learning functions: selfLearning() & transferLearning().
	 * @param useTransfer
	 * 		indicates whether the agent's task-required knowledge is initially insufficient and has increased via TRANSFER learning
	 */
	protected void finishIndividualTask() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int conditionID = (Integer) params.getValue("conditionID");
		int currentProblem = (Integer) params.getValue("currentProblem");
		this.idle = true;
		if(Constants.switch_DKS == -2) taskEnv.addToExpertIndex();
		taskEnv.reportTimeCost();
		if(Constants.switch_DKS == 2){
			if(xcs.actionSet!= null) xcs.actionSet.updateSet();
			//DataIO.outputStrategyChain("output/stratComp" + Integer.toString(conditionID) + ".csv", currentProblem, this);
			/*int value = xcs.decidePreferences("#######", 8);
			String preferences = String.format("%3s", Integer.toBinaryString(value)).replace(
					" ", "0");
			this.setSCPreference(Integer.parseInt(preferences.substring(0,1)));
			this.setNDPreference(Integer.parseInt(preferences.substring(1,2)));
			this.setRBPreference(Integer.parseInt(preferences.substring(2,3)));*/
		}
		int totalLeft = (Integer) RunEnvironment.getInstance().getParameters()
				.getValue("totalLeft") - 1;
		RunEnvironment.getInstance().getParameters()
				.setValue("totalLeft", totalLeft);
		taskEnv.clearLocalNet();
	}

	/**
	 * @return this agent's ID
	 */
	public String getID() {
		return myID;
	}

	/**
	 * @return this agent's assigned task in the current problem
	 */
	public int[] getCurrentTask() {
		return taskEnv.currentTask;
	}
	
	/**
	 * @return this agent's current preference on bonding (pref = 0) or bridging (pref = 1) social capital
	 */
	public int getSCPreference(){
		return SCPreference;
	}
	public int getNDPreference(){
		return NDPreference;
	}
	public int getRBPreference(){
		return RBPreference;
	}
	/**
	 * Set this agent's social capital preference.
	 * @param PrefValue
	 */
	public void setSCPreference(int PrefValue){
		SCPreference = PrefValue;
	}
	public void setNDPreference(int PrefValue){
		NDPreference = PrefValue;
	}
	public void setRBPreference(int PrefValue){
		RBPreference = PrefValue;
	}
	/**
	 * @return this agent's task environment
	 */
	public Environment getTaskEnvironment() {
		return taskEnv;
	}

	/**
	 * @return this agent's XCS system
	 */
	public XCS getXCS() {
		return xcs;
	}

	/**
	 * Initialize the expertise of the agent by equipping it with a certain number of specialty areas.
	 * Since the specialty areas should not be repetitive, I use the HashMap data structure and track its size.
	 * The put method of HashMap will replace the old value of a certain key with the newest value,
	 * so the entries of a HashMap are always guaranteed to be unique.
	 * @param totalArea
	 * 		the total number of specialty areas from which the agent's will be randomly chosen from
	 * @param memberArea
	 * 		the number of specialty areas an agent is originally equipped with
	 * @param maxKnowLevel
	 * 		the maximal (also the task-required)level of knowledge for each area;
	 * 		the actual level of knowledge is randomly chosen from the range of [0, max].
	 */
	public void initiateExpertise(int totalArea, int memberArea, int maxKnowLevel) {
		while (myExpertise.size() < memberArea){
			SpecialtyArea sa = new SpecialtyArea((double)RandomHelper.getUniform().nextIntFromTo(1, maxKnowLevel), -1);
			myExpertise.put(RandomHelper.getUniform().nextIntFromTo(0, totalArea-1), sa, false);
		}
		taskEnv.reset();
	}

	/**
	 * Assign a task to the agent.
	 * The task is comprised of a predetermined number of UNIQUE (i.e., all different) specialty areas. 
	 * To avoid repetition I use "HashSet", whose values are always guaranteed to be unique, to store the selected areas.
	 * (When you "add" to a set an already existing element, the old one will be discarded.)
	 * This method first determines which part(s) of the existing task array will be changed by generating a set of random index numbers.
	 * Then new values are assigned to corresponding fields of the task array. Every new value is different from all old values as well as earlier prior new values.  
	 * Finally, the agent's task environment is reset based on the newly assigned task. 
	 * @param totalArea
	 * 		the total number of specialty areas from which the constituent areas of the task will be randomly chosen from
	 * @param taskRange
	 * 		the number of a task's constituent areas
	 * @param percentOfChange
	 * 		the percentage of prior-round tasks that will be changed in the current round
	 */
	public void assignTask(int totalArea, int taskRange, double percentOfChange) {
		int numOfChange = (int) (taskRange*percentOfChange);
		Set<Integer> index = new HashSet<Integer>();
		ArrayList<Integer> oldTask = new ArrayList<Integer>();
		for(int i = 0; i < taskEnv.currentTask.length; i++)
			oldTask.add(taskEnv.currentTask[i]);
		while(index.size() < numOfChange)
			index.add(RandomHelper.getUniform().nextIntFromTo(0, taskRange-1));//in nextIntFromTo, both extremes of the range are inclusive.
		for(Integer toChange: index){
			int newTask = 0;
			do{
				newTask = RandomHelper.getUniform().nextIntFromTo(0, totalArea-1);
			}while (oldTask.contains(newTask));
			taskEnv.currentTask[toChange.intValue()] = newTask;
			oldTask.add(newTask);
		}
		taskEnv.reset();
	}
	/**
	 * Set up the tasks of each agent when initializing the system
	 * @param totalArea
	 * 		the total number of specialty areas from which the constituent areas of the task will be randomly chosen from
	 * @param taskRange
	 * 		the number of a task's constituent areas
	 */
	public void initiateTask(int totalArea, int taskRange) {
		Set<Integer> setTemp = new HashSet<Integer>();
		while(setTemp.size() < taskRange)
			setTemp.add(RandomHelper.getUniform().nextIntFromTo(0, totalArea-1));
		Integer [] arrayTemp = new Integer[taskRange];
		setTemp.toArray(arrayTemp);
		for (int j = 0; j < taskRange; j++)
			taskEnv.currentTask[j] = arrayTemp[j].intValue();
	}

	/**
	 * Examine each task-required specialty area and see if the knowledge has achieved the maximal level
	 * @param taskExperience
	 * 		an attribute of the Environment class;
	 * 		it records the agent's levels of knowledge in each task-required area
	 * @return true only if all required areas have the maximal level of knowledge
	 * @return false otherwise
	 */
	public boolean meetRequirements() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int requiredLevel = (Integer) params.getValue("maxKnowLevel");
		for (int element : taskEnv.currentTask){
			if(!myExpertise.containsKey(element)){
				//myExpertise.put(element, new SpecialtyArea(0), false);
				return false;
			}
			if (myExpertise.get(element).currentLevel < requiredLevel)
				return false;
		}
		return true;
	}

	/**
	 * @return true if this agent has expertise in a specialty area indicated by the parameter key
	 */
	public boolean hasExpertise(int key){
		if (myExpertise.containsKey(key))
			return true;
		else
			return false;
	}
	
	/**
	 * @return this agent's knowledge level in a specialty area indicated by the parameter key
	 */
	public double getExpertise(int key){
		if (myExpertise.containsKey(key))
			return myExpertise.get(key).currentLevel;
		else return 0.0;
	}

	/**
	 * At every step an agent will follow the same procedure described in this method. 
	 * 
	 * If the agent does not have enough knowledge and its message inbox (implemented as a queue) is empty, it has to request knowledge from others.
	 * Each potential source, to whom the agent will send a knowledge request, is picked up via the agent's XCS system in two steps.
	 * Each pair of source and recipient can be represented by an XCS classifier that records their network positions and structural relationship. @see XClassifierSet.
	 * 
	 * Firstly, the XCS decides whether the agent should utilize its bonding or bridging social capital by weighing two corresponding types of classifiers already in the population set. @see XClassfierSet.
	 * A list of potential sources that match the preference are selected from the network, and all classifiers of the chosen type are copied into a match set.
	 * Note that the request list may be empty when the agent has no connections and yet it chooses bonding social capital.
	 * Then each potential source is evaluated by looking for its match classifier in the match set. Only when such classifier exists will the agent send a request to the source.
	 * The condition part (7 bits) of the corresponding classifier should indicate the recipient and the source's network positions. The first 2 bits of the action part (if in binary form) should indicate
	 * the type of relation between the recipient and the source. However, it does not matter what the last bit is (0, allowing for high degree, or 1, allowing for low degree).
	 * Therefore, the action part in integer form can be either d (d = 0, 2, 4, or 6) or d + 1. Every classifiers that leads to a request is copied into the action set.
	 * 
	 * Another issue that should be considered is the generality of classifiers. Match does not have to be identification, regarding that there may be "#" in a classifier. A "#" can match either "0" or "1".
	 * However, the final classifier added to the action set should have more specific condition and action parts.
	 * According to the XCS algorithm ({@link XClassifierSet#addClassifier(XClassifier)}, the condition part will probably be further generalized when the corresponding classifier is added to a set.
	 * So even if a matching classifier already exists in the match set, it won't be the one added to the action set if it is more general.
	 * Instead, a new classifier with the specific condition and action parts will be created and added.
	 * 
	 * After sending out requests, the agent conducts self-learning, which will be conducted at steps when the agent's message inbox is empty.
	 * 
	 * If the message inbox is not empty, the agent processes the messages one by one and one message per step.
	 * A message can be a request reply or a knowledge requests from others. All messages are assembled in the same inbox.
	 * The message inbox is implemented using a queue data structure, so that all messages are arranged in an FIFO (first-input-first-output) manner.
	 * The relative order of replies in the queue is not necessarily the same as the order of earlier requests. The sources differ in the load of their inboxes.
	 * Generally the agent processes the messages as they are ordered (FIFO), except when the agent is NOT idle and the message is a request from an unfamiliar organizational member.
	 * In these circumstances, the agent answers the request in a probability defined by an exponential function,
	 * in which the base is defined by Constants.baseReplyProb, and the power varies with the agent's relationship with the requester.
	 * Otherwise the agent will postpone processing the request and move it from the front of the queue to the rear of the queue.

	 * If the agent has finished its task (idle), it simply follows the FIFO rule and only handle request messages (ignore replies since they are no longer needed).
	 * When the message is a reply, the agent may or may not increase the levels of its knowledge, depending on what the source can offer.
	 * If no knowledge is gained, there is a time cost. Whatever the result is, the fitness of classifier in the action set will be updated. 
	 * The network will be changed as well: a tie between the source and the recipient will be added or strengthened.
	 * Besides suffering from opportunity costs, an agent may be too busy replying knowledge requests to acquire knowledge for its own task,
	 * when in the indox queue replies are preceded by too many requests.
	 * In this way I model the benefits and liabilities of knowledge transfer.
	 */
	public void step() {
		if (!idle){
			//An agent is likely to possess maximal level of knowledge in each required area of specialty
			//at the very beginning. In other words, it does not have to learn any more. This situation is 
			//expected to happen when the value of the global parameter "orgArea" is relatively small.
			//The following qualification test must be made before other operations; otherwise it is possible
			//for a qualified agent to keep not idle for several steps (timeCost > 0) since it has many knowledge requests.
			if (meetRequirements()){
				finishIndividualTask();
				return;
			}
			taskEnv.timeCost++;
			Constants.setSeed(1 + new Date().getTime() % 10000);
			int requiredLevel = (Integer) RunEnvironment.getInstance().getParameters().getValue("maxKnowLevel");
			boolean processMsg = false;
			Queue<Message> postPonedMsg = new LinkedList<Message> ();
			while (!myMessenger.inbox.isEmpty() && !processMsg){
				Message msg = myMessenger.inbox.poll();
				if(msg.isRequest) {
					if (RandomHelper.nextDoubleFromTo(0, 1) <= taskEnv.getReplyProb(msg.getFrom())){
						processMsg = true;
						myMessenger.answerRequest(msg, RBPreference); 
					}
					else
						postPonedMsg.offer(msg);
				}
				else{
					processMsg = true;
					double payoff = transferLearning(requiredLevel, msg.getContent(), msg.getFrom());					
					if(Constants.switch_DKS == 2)
						xcs.actionSet.updateSet(msg, payoff);
					//only when the knowledge transfer is successful will the tie be established or strengthened
					//modify tie including change tie history matrix
					taskEnv.modifyTie(msg.getBaseCl(), msg.getFrom(), payoff);
					//taskEnv.modifyTie(msg.getBaseCl(), msg.getFrom());
				}
			}
			//After the previous "while" statement, the inbox is empty if the condition below is true.
			//transferLearning (in the while statement above) and selfLearning (in the following if statement) won't happen at the same step because
			//when executing the above while statement, flag variable processMsg has been changed to "TRUE", so the following if condition is "FALSE"
			if(!processMsg){
				selfLearning(requiredLevel);
				while(!postPonedMsg.isEmpty())
					myMessenger.inbox.offer(postPonedMsg.poll());
				//If the reference list is empty or no match classifier exists, a classifier with general condition
				//and random action (see below) will be added to the action set, so that the action set won't be null
				if (myMessenger.inbox.isEmpty()) {
					String myPosition = taskEnv.getPosition(this.getID(), true);
					switch(Constants.switch_DKS){
						case 2://make decisions based on its XCS
							int value = xcs.decidePreferences(myPosition + "###", 8);
							if(strategyChain.peekLast() != value) strategyChain.add(value);
							//System.out.println("value = " + value + "\n");
							//if(value != 0) System.out.println("I'm not zero! Yeah!\n");
							String preferences = String.format("%3s", Integer.toBinaryString(value)).replace(
									" ", "0");
							this.setSCPreference(Integer.parseInt(preferences.substring(0, 1)));
							this.setNDPreference(Integer.parseInt(preferences.substring(1, 2)));
							this.setRBPreference(Integer.parseInt(preferences.substring(2, 3)));
							break;
						case -2://choose from the expert index
							this.setSCPreference(-2);
							break;
						case -1://randomly choose
							this.setSCPreference(-1);
							break;
						case 0://always use bonding social capital
							this.setSCPreference(0);
							break;
						case 1://always use bridging social capital
							this.setSCPreference(1);
							break;	
					}
					int size = taskEnv.collectSource(getSCPreference(), getNDPreference());
					HashMap<String, String> requestList = null;
					if(size > 0) requestList = taskEnv.getSourceList();
					XClassifier cl = null;
					if (requestList != null){
						for (Map.Entry<String, String> source : requestList.entrySet()) {
							if(Constants.switch_DKS == 2){
								String condition = myPosition + source.getValue().substring(0,3);
								//the value of sourceList is partialCl, which represents the source's position (3 bits), its relation type with the recipient/SCPreference (1 bit), NDPreference (1 bit), and RBPreference (1 bit)
								int action = getSCPreference()*4 + getNDPreference()*2;
								//The first classifier being tried has an action part that "avoids" degree increment.
								XClassifier cl1 = xcs.matchSet.getMatchClassifier(condition, action, false); 
								//The second classifier being tried has an action part that "allows" degree increment.
								XClassifier	cl2 = xcs.matchSet.getMatchClassifier(condition, action+1, false);
								if(cl1 != null && cl2 != null)
									cl = (Constants.drand() < 0.5)? cl1: cl2;
								else if(cl1 == null && cl2 != null)
									cl = cl2;
								else if(cl2 == null && cl1 != null)
									cl = cl1;
								else //cl1 == null && cl2 == null
									cl = new XClassifier(-1, -1, 8, myPosition + "###");
								//When adding the classifier to the action set, change the actionSetSize and timeStamp fields
								cl = xcs.addClassifierToAction(cl);
							}
							//int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
							myMessenger.send(true, source.getKey(), taskEnv.checkGaps(), cl);
							NetworkAnalysis.updateTieUse(myID, source.getKey());
						}
					}
					else if(Constants.switch_DKS == 2)
						xcs.addClassifierToAction(new XClassifier(-1, -1, 8, myPosition + "###"));
				}
			}
		}  
		else  {
			Message msg = myMessenger.inbox.poll();
			if (msg!=null){
				if(msg.isRequest)
					myMessenger.answerRequest(msg, RBPreference);
				else
					msg = null;//algorithm optimization: after that, there is no reference to the object and it will be deleted by garbage collector
			}
		}
		decayExpertise();
		/*if(xcs.actionSet!= null && Constants.switch_DKS == 2)
			xcs.actionSet.updateSet();*/
	}

	/**
	 * An agent learns by its own when it does not participate, as source or recipient, in any knowledge transfer at a certain step (t).
	 * Self-learning happens when an agent's message inbox is empty (see the "step" method in this class).
	 * Self-learning can make sure that an agent will obtain sufficient knowledge even without knowledge transfer.
	 * Self-learning function: KnowledgeLevel(t) = MaxLevel - (MaxLevel - KnowledgeLevel(0)) * t^(-learningRate)
	 * The function follows the power law of practice and is adapted from: 
	 * Levine & Prietula (2011) "How Knowledge Transfer Impacts Performance: A Multilevel Model of Benefits and Liabilities." Org Science.
	 * @param maxLevel
	 * 		the maximal (also the task-required) level of knowledge for each area
	 */
	public void selfLearning(int maxLevel) {
		for (int j = 0; j < taskEnv.currentTask.length; j++){
			int task = taskEnv.currentTask[j];
			//It is possible that the agent does not have the expertise in this area; that is, "get" returns null
			if(!myExpertise.containsKey(task))
				myExpertise.put(task, new SpecialtyArea(0, 2), false);
			/*for(java.util.Map.Entry<Integer, SpecialtyArea> e: myExpertise.entrySet()){
				System.out.println("Area" + e.getKey() + ": Level = " + e.getValue().currentLevel + "; State = " + e.getValue().getState() + "\n");
			}*/
			SpecialtyArea sa  = myExpertise.get(task);//Java HashMap get() returns a reference
			sa.setState(1);
			sa.addKnow(maxLevel, rateOfSelfGain);
		}			
		if (!idle && meetRequirements())
			finishIndividualTask();
	}
	
	/**
	 * An agent learns from transferred knowledge when it gets a request reply at a certain step (t).
	 * Transfer-learning function also follows the power law of practice, but differs from the self-learning function in the learning rate.
	 * At the current version, transfer learning only happens when the corresponding knowledge level of the source is higher than or equal to that of the recipient.
	 * Wider knowledge gap results in higher learning rate.
	 * @param maxLevel
	 * 		the maximal (also the task-required) level of knowledge for each area
	 * @param msgContent
	 * 		the source's levels of knowledge on the recipient's task-required areas (when the reply was sending out)
	 * @param timeSpent
	 * 		Note: this parameter is no longer used
	 * 		the time that has been past from the agent sending out the request to it receiving the reply,
	 * 		excluding the time that the reply has stayed in the message inbox waiting for processing.
	 * 		This period of time is used to calculate the average learning rate in relation with a specific source,
	 * 		which serves as the reward to the classifier indicative of a specific source-recipient pair.
	 * 		Note that this period of time is different from the time used in the learning function, which is the current tick
	 * @return
	 * 		the average learning rate associated with a specific knowledge source.
	 * 		It will be the reward of the corresponding classifier and used in the XCS.
	 */
	public double transferLearning(int maxLevel, HashMap<Integer, Double> msgContent, String sourceID) {
		double sumOfGain = 0.0;
		OrgMember source = null;
		Iterable<OrgMember> members = RunState.getInstance().getMasterContext().getObjects(OrgMember.class);
		for(OrgMember member: members)
			if(member.getID() == sourceID){
				source = member;
				break;
			}
		//transferLearning() is called under a condition that the focal agent is not idle,
		//so there must be at least one area in which the focal agent hasn't achieve the maximal knowledge. 
		for(int j = 0; j < taskEnv.currentTask.length; j++){
			int task = taskEnv.currentTask[j];
			if(!myExpertise.containsKey(task))
					myExpertise.put(task, new SpecialtyArea(0, 2), false);
			double sourceKL = source.getExpertise(task);
			double recipientKL = this.getExpertise(task);
			//Although msgContent contains the task, due to possible time delay,
			//source's expertise may no long have the corresponding specialty Area
			//or the same level of knowledge in that area (could be more or less)
			
			if(recipientKL == maxLevel || sourceKL == 0 || sourceKL < recipientKL)
				continue;
			else{
				myExpertise.get(task).setState(1);
				double bothLevels = 1.0;
				if(Constants.switch_KLC){
					bothLevels = sourceKL + recipientKL;
					//the last step is to normalize the value of bothLevels with maxLevel
					bothLevels /= maxLevel;
				}
				double newLevel = myExpertise.get(task).addKnow(maxLevel, bothLevels * Constants.KTOverSL*rateOfSelfGain);
				sumOfGain += newLevel - recipientKL;
			}
		}
		if (!idle && meetRequirements())
			finishIndividualTask();
		return sumOfGain;
		//return sumOfGain / timeSpent;
	}
}