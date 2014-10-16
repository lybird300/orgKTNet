package orgKTNet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import sun.security.provider.SystemSigner;

/**
 * Each instance of this class is associated with an agent and its current task.
 * It executes XCS-determined actions and evaluates the results.
 * @author Yuan Lin
 * @version OrgKTNet 1.0
 */
public class Environment {
	/**
	 * The associated agent
	 */
	protected OrgMember focalMember;
	/**
	 * The organizational-level classifier set
	 */
	//public XClassifierSet orgCS;
	/**
	 * The time (number of steps) it takes an agent to complete a task; used for performance evaluation
	 */
	public int timeCost;
	/**
	 * The number of problems that the agent can tolerate when its performance is below the average.
	 */
	public int timesOfBadPerform;
	/**
	 * The focal agent's current tasks (task-required areas)
	 */
	public int[] currentTask = null; 
	/**
	 * The agent's current level of knowledge in each task-required area.
	 * Key = specialty area; Value = level of knowledge
	protected HashMap<Integer, Integer> taskExperience = null; */
	/**
	 * The organization maintains a single expert index
	 * @see ExpertIndex.java
	 */
	protected static ExpertIndex index = null;
	/**
	 * The organizational level recorder of time cost, used to calculate the average time cost of all agents
	 * @see OrgBuilder#build(Context)
	 */
	protected static double[] avgTimeCost = null;
	/**
	 * 	The list of potential sources as a HashMap. The key of each entry is the ID of an agent (a potential source).
	 * 	The value of each entry is a string indicating the agent's network position and its relation with the focal agent.
	 * 	The value string will become part of the classifier that corresponds to a certain pair of knowledge source and recipient.
	 * 	The key and the value cannot be exchanged since agent ID is unique but network position is not.
	 */
	protected HashMap<String, String> sourceList = null;

	/**
	 * Construct an agent's task environment.
	 * The variable memberArea indicates the number of each agent's initial specialty areas.
	 * @param member
	 * 		The associated agent (organizational member)
	 * @param net
	 * 		The organizational social network, which will affect and be shaped by the agent's knowledge transfer actions.
	 */
	public Environment(OrgMember member, ExpertIndex ei, double[] atc) {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int taskRange = (Integer) params.getValue("taskRange");
		currentTask = new int[taskRange];
		for(int i = 0; i < currentTask.length; i++)
			currentTask[i] = 0;
		focalMember = member;
		timeCost = 0;
		timesOfBadPerform = 0;
		index = ei;
		//orgCS = xs;
		avgTimeCost = atc;
		sourceList = new HashMap<String, String>();
	}

	/**
	 * Add the focal agent to the organizational expert index.
	 */
	public void addToExpertIndex() {
		index.addExpert(focalMember);	
	}

	/**
	 * This method examines whether a specific agent has the preferred relation with the focal agent in order to be selected into the source list.
	 * If the agent has already been in the source list, do nothing and return false.
	 * @param om
	 * 		the agent to be tested
	 * @param preference
	 * 		the preferable type of social capital (0 - bonding social capital, 1 - bridging social capital)
	 * @return
	 * 		{@code true} if the agent has been selected and added to the source list
	 * 		{@code false} otherwise.
	 */
	public boolean addToSourceList(ContextJungNetwork<OrgMember> net, OrgMember om, int preference){
		if(focalMember.getID() == om.getID()
				|| sourceList.containsKey(om.getID())) return false;
		String partialCl = null;
		//int relationType = getRelation(omID);
		double localConst = NetworkAnalysis.localConstraint(net, focalMember, om);
		if((localConst >= 0.7 && preference == 0)|| (localConst <= 0.3 && preference == 1)) {
			if(Constants.switch_DKS == 2)
				//partialCl is a string variable with a length of 3(non-focal member's position)
				//+ 1 (the first bit of the action part; social capital preference) = 5 bits 
				partialCl = getPosition(om.getID(), false) + Integer.toString(preference);
	    	String agentA = focalMember.getID();
	    	String agentB = om.getID();
			int row = (Integer.parseInt(agentA) <= Integer.parseInt(agentB))? (Integer.parseInt(agentA) - 1):(Integer.parseInt(agentB) - 1);
			int col = (Integer.parseInt(agentA) <= Integer.parseInt(agentB))? (Integer.parseInt(agentB) - 1):(Integer.parseInt(agentA) - 1);
			//Since the effect of tie strength > Constants.tieDecayMin has been considered into network constraint,
			//next we only consider those ties whose strength <= the min value. These ties have been removed from the network
			//As long as the historical strength >= 0, the reconnection probability = 1
			double reconnectProb = 1.;
			double oldWeight = NetworkAnalysis.tieHistory[row][col];
			if(oldWeight < 0) reconnectProb = 1.0/(1 + Math.exp(-0.1*oldWeight));
			if(Constants.drand() <= reconnectProb){
				sourceList.put(om.getID(), partialCl);
				return true;
			}
			else return false;
		}
		else return false;
	}
	
	/**
	 * This method simply adds the agent which has the parameter ID into the sourceList.
	 * It is for a list of sources that are selected randomly or from the expert index, in contrast to selected based on social capital.
	 * @param omID
	 * 		the ID of the agent to be added
	 * @return
	 * 		whether the agent has been successfully added into the source list
	 */
	public boolean addToSourceList(String omID){
		if(focalMember.getID() == omID
				|| sourceList.containsKey(omID)) return false;
		sourceList.put(omID, null);
		return true;
	}

	/**
	 * This method updates the taskExperience field based on the agent's myTask and myExpertise fields.
	 * @param initialExperience
	 * 		It indicates whether the method is called to calculate the initial knowledge gap.
	 * 		If so, the learningConstant array needs to be filled.
	 * @return
	 * 		a HashMap that records the agent's latest levels of knowledge in task-required areas

	public HashMap<Integer, Integer> calTaskExperience() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int requiredLevel = (Integer) params.getValue("maxKnowLevel");
		int[] task = focalMember.getTask();
		HashMap<Integer, Integer> experience = focalMember.showExpertise();
		taskExperience = new HashMap<Integer, Integer>();
		for (int i = 0; i < task.length; i++) {
			int value = 0;
			if(experience.containsKey(task[i])){
				int exp = experience.get(task[i]);
				// Keep the agent's knowledge levels within the maximally possible value
				if (exp > 0)
					value = exp > requiredLevel ? requiredLevel : exp;
			} 
			//otherwise "value" remains to be zero
			this.taskExperience.put(task[i], value);
		}
		return taskExperience;
	}
	*/
	/**
	 * This method compares the agent's experience with its task requirements.
	 * @return
	 * 		a list of specialty areas in which the focal agent needs more experience.
	 */
	public HashMap<Integer, Double> checkGaps (){
		HashMap<Integer, Double> gaps = new HashMap<Integer, Double> ();
		int requiredLevel = (Integer) RunEnvironment.getInstance().getParameters().getValue("maxKnowLevel");
		for(int area : currentTask){
			double currentLevel = focalMember.getExpertise(area);
			if(currentLevel < requiredLevel)
				gaps.put(area, currentLevel);
		}
		return gaps;
	}
	
	/**
	 * Collect a list of potential knowledge sources from the organizational social ContextJungNetwork based on the agent's preferred type of social capital.
	 * If the agent prefers bonding social capital, then potential sources are those whom it strongly connects with or shares many common contacts with.
	 * If the agent prefers bridging social capital, then potential sources are those whom it weakly connect with or share few common contacts with.
	 * @param scPreference
	 * 		the preferable type of social capital (0 - bonding social capital, 1 - bridging social capital)
	 * 		or random selection (-1) or selection from the expert index only (-2)
	 */
	public int collectSource(int scPreference, int ndPreference) {
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		if(scPreference == -1){
			Parameters params = RunEnvironment.getInstance().getParameters();
			int orgSize = (Integer) params.getValue("orgSize");
			ArrayList<OrgMember> allMembers = new ArrayList<OrgMember>();
			for(Object om : context.getObjects(OrgMember.class))
				allMembers.add((OrgMember)om);
			while(sourceList.size() < Constants.numOfRandSrc){
				int randPos = RandomHelper.getUniform().nextIntFromTo(0, orgSize - 1);
				addToSourceList(allMembers.get(randPos).getID());
			}
			return sourceList.size();
		}
		if(scPreference == -2){
			for(Integer area: checkGaps().keySet()){
				ArrayList<String> areaExperts = index.searchExpert(area.intValue());
				if(areaExperts != null)
					for(String areaExpert: areaExperts)
						addToSourceList(areaExpert);
			}
			return sourceList.size();
		}	
		if (scPreference == 0){
			for (OrgMember om: net.getAdjacent(focalMember)){
				addToSourceList(net, om, 0);
				if(ndPreference == 1){
					for(OrgMember omNeighbor: net.getAdjacent(om))
						addToSourceList(net, omNeighbor, 0);
				}
			}
			return sourceList.size();
		}
		if (scPreference == 1){
			if(ndPreference == 1){
				for(Object om : context.getObjects(OrgMember.class))
					addToSourceList(net, (OrgMember)om, 1);
			}
			else{
				for (OrgMember om: net.getAdjacent(focalMember))
					addToSourceList(net, om, 1);
			}
			return sourceList.size();
		}
		return -1;
	}

	/*public HashMap<String, String> collectSource(int preference) {
	Iterable<Object> members = net.getNodes();
	HashMap<String, String> sourceList = new HashMap<String, String>();
	for (Object member : members) {
		int relationType = getRelation((OrgMember) member);
		boolean qualified = false;
		if (preference == 0 && (relationType == 0 || relationType == 1))
			qualified = true;
		if (preference == 1 && (relationType == 2 || relationType == 3))
			qualified = true;
		if (qualified) {
			String partialCl = getPosition((OrgMember) member, false)
					+ String.format("%2s", Integer.toBinaryString(relationType)).replace(" ", "0");
			sourceList.put(partialCl, ((OrgMember) member).getID());
		}
	}
	return sourceList;
}*/
	/**
	 * Calculates the average node degree of the network
	 * @param numOfNodes
	 * 		the size of the network
	 */
/*	public double getAvgDC() {
		Context<Object> context = RunState.getInstance().getMasterContext();
		Network<OrgMember> net = (Network<OrgMember>) context.getProjection("socialNetwork");
		int numOfNodes = (Integer)RunEnvironment.getInstance().getParameters().getValue("orgSize");
		int sumDegree = 0; 
		for (OrgMember node : net.getNodes())
			sumDegree += net.getDegree(node);
		return (double) sumDegree / (double) numOfNodes;
	}*/
	/**
	 * Calculate the average strength of the agent's ties with other agents in the organizational social network.
	 * @param om
	 * 		the focal agent
	 * @return
	 * 		the average tie strength
	 */
	public double getAvgTieStrength(OrgMember om) {
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		int sumStrength = 0;
		for (RepastEdge<OrgMember> tie : net.getEdges(om))
			sumStrength += tie.getWeight();
		return (double) sumStrength / (double) net.getDegree(om);
	}
	


	/**
	 * Calculate the density of the agent's ego social network.
	 * @param om
	 * 		the focal agent
	 * @return
	 * 		ego network density
	 */
	public double getEgoNetworkDensity(OrgMember om) {
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		int numOfNodes = net.getDegree(om) + 1;
		// "+1" refers to the ego.
		if (numOfNodes < 2)
			return 0.0;
		else {
			int numOfEdges = 0;
			ArrayList<OrgMember> nodes = new ArrayList<OrgMember>();
			for (Object neighbor : net.getAdjacent(om))
				nodes.add((OrgMember) neighbor);
			// add the ego
			nodes.add(om); 
			for (int i = 0; i < numOfNodes; i++) {
				OrgMember nodeA = nodes.get(i);
				for (int j = i + 1; j < numOfNodes; j++) {
					OrgMember nodeB = nodes.get(j);
					if (net.isAdjacent(nodeA, nodeB))
						numOfEdges++;
				}
			}
			int maxNumOfEdges = numOfNodes * (numOfNodes - 1) / 2;
			return (double) numOfEdges / (double) maxNumOfEdges;
		}
	}

	/**
	 * Evaluate an agent's network position and store the information in a string that will become part of a classifier.
	 * In the current version, the condition part of a classifier has 7 bits:
	 * The first 4 bits (indexed as 0-3) describe the state of the focal agent (i.e., the recipient, the agent originally associated with this environment instance),
	 * whereas the remaining 3 bits (indexed as 4-6) describe that of the source.
	 * The bit at Position 0 (or 4) indicates whether the degree of the recipient (or the source) is larger than the average node degree of the network.
	 * The bit at Position 1 (or 5) indicates whether the average tie strength of the recipient (or the source) is larger than the average tie strength of the network.
	 * The bit at Position 2 (or 6) indicates whether the density of the recipient (or the source)'s ego network is larger than the global network density.
	 * I design the classifier condition as such to signify the source and the recipient's bonding/bridging social capital at different levels of the network structure (node, dyad, and sub-network)
	 * The bit at Position 3 indicates how many of the required specialty areas the recipient originally has.
	 * An agent and its social contacts tend to have similar specialty areas(not at the beginning but over time),
	 * so if an agent has already had some though not the maximal level of knowledge at all required areas,
	 * there is a chance that it can get needed knowledge from its close contacts instead of other distant sources.
	 * Such information is not collected at the source side since it is not something as visible to the recipient as the source's network position.
	 * Recall that an agent's XCS simulates schema-based cognitive processes and is thus subjected to human cognitive limits.
	 * @param omID
	 * 		the ID of an agent whose network position this method intends to capture.
	 * @param isFocalMember
	 * 		indicates whether the agent is the one originally associated with this task environment
	 * @return
	 * 		the string indicative of the agent's network position 
	 */
	public String getPosition(String omID, boolean isFocalMember) {
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		Parameters params = RunEnvironment.getInstance().getParameters();
		double avgDC = (Double) params.getValue("avgDegreeCentrality");
		double globalDensity = (Double) params.getValue("networkDensity");
		double avgTieStrength = (Double) params.getValue("avgTieStrength");
		
		//casting into integer will round the value down (e.g., 7/2 = 3)
		char[] p = new char[7/2];
		
		OrgMember om = null;
		if(isFocalMember)
			om = focalMember;
		else{
			for(OrgMember member : net.getNodes())
				if(member.getID() == omID){
					om = member;
					break;
				}
		}
		int omDC = net.getDegree(om);
		double omEgoDensity = getEgoNetworkDensity(om);
		double omAvgTieStrength = getAvgTieStrength(om);

		if (omDC > avgDC)
			p[0] = '1';
		else
			p[0] = '0';

		if (omAvgTieStrength > avgTieStrength)
			p[1] = '1';
		else
			p[1] = '0';

		if (omEgoDensity > globalDensity)
			p[2] = '1';
		else
			p[2] = '0';
		
		for(int k = 0; k < p.length; k++)
			if(Constants.switch_EST && RandomHelper.nextDoubleFromTo(0, 1) <= Constants.estErrProb){
				if(p[k] == '1') p[k] = '0';
				else p[k] = '1';
			}

		String position = new String(p);

		if (isFocalMember) {
			if (checkGaps().size() > focalMember.myExpertise.size() / 2)
				position += '0';
			else
				position += '1';
		}
		return position;
	}

	/**
	 * Evaluate the focal agent's network relation with another agent.
	 * There are four types of relations. 
	 * Two agents may be connected and the strength of the tie is higher or lower than the average tie strength in the network.
	 * Or, they may be disconnected and the ratio of their common contacts to the focal agent's degree is higher or lower than a threshold.
	 * @see Constants#commonContactThresh 
	 * @param secondID
	 * 		the ID of another agent
	 * @return
	 * 		the type of relation: 0 - strongly connected, 1 - unconnected but sharing many common contacts,
	 * 		2 - weakly connected, 3 - unconnected and sharing few common contacts;
	 * 		return -1 if the other agent is identical to the focal agent.
	 */
	public int getRelation(String secondID) {
		int returnValue = -1;
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		OrgMember secondMember = null;
		for(Object om : context.getObjects(OrgMember.class))
			if(((OrgMember)om).getID() == secondID){
				secondMember = (OrgMember) om;
				break;
			}
		if(secondMember == null)
			System.out.println("empty second member");
		if (net.isAdjacent(focalMember, secondMember)) {
			double tieStrength = net.getEdge(focalMember, secondMember).getWeight();
			Parameters params = RunEnvironment.getInstance().getParameters();
			double avgTieStrength = (Double) params.getValue("avgTieStrength");
			if (tieStrength > avgTieStrength)
				returnValue = 0;
			else
				returnValue = 2;
		}
		else {
			ArrayList<OrgMember> mNeighbors = new ArrayList<OrgMember>();
			ArrayList<OrgMember> oNeighbors = new ArrayList<OrgMember>();
			for (OrgMember mNeighbor : net.getAdjacent(focalMember))
				mNeighbors.add(mNeighbor);
			for (OrgMember oNeighbor : net.getAdjacent(secondMember))
				oNeighbors.add(oNeighbor);
			mNeighbors.retainAll(oNeighbors);
			double ratio = (double) mNeighbors.size() / (double) net.getDegree(focalMember);
			if (ratio >= Constants.commonContactThresh)
				returnValue = 1;
			else
				returnValue = 3;
		}
		if(Constants.switch_EST && RandomHelper.nextDoubleFromTo(0, 1) <= Constants.estErrProb)
			switch(returnValue){
				case 0:
					returnValue = 2;
					break;
				case 2:
					returnValue = 0;
					break;
				case 1:
					returnValue = 3;
					break;
				case 3:
					returnValue = 1;
					break;
			}
		return returnValue;
	}

	/**
	 * @return
	 * 		the focal agent's social contacts, whose connections with the focal agent has a strength higher than the average. 
	 */
	public ArrayList<OrgMember> getCloseContacts() {
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		double avgStrength = getAvgTieStrength(focalMember);
		//if(avgStrength == 0)
			//return null;
		ArrayList<OrgMember> closeContacts = new ArrayList<OrgMember>();
		for (OrgMember contact : net.getAdjacent(focalMember)) {
			double cStrength = net.getEdge(focalMember, contact).getWeight();
			if (cStrength > avgStrength)
				closeContacts.add(contact);
		}
		return closeContacts;
	}
	
	/**
	 * @return the current list of potential knowledge sources.
	 */
	public HashMap<String, String> getSourceList(){
		return sourceList;
	}

	/**
	 * Modify the relation between the focal agent and another agent in the organizational social network.
	 * Increase the strength of an existing tie by 1, or build a new tie of strength 1.0. Thus, the strength of ties is positively related with the frequence of interaction
	 * The classifier (as a rule of action) leading to this modification may demand a low degree of the focal agent (the last bit is '0', not '1' or '#').
	 * In this circumstance, the degree of the focal agent should not exceed the average node degree of the network.
	 * If adding a new tie violates this degree constraint, the weakest tie will be removed to make a room for the new tie.
	 * Removal should come first; otherwise the tie just added may be mistakenly removed as for its low strength.
	 * @param cl
	 * 		the classifier that leads to this tie modification
	 * @param id
	 * 		the other agent's ID
	 */
	public void modifyTie(XClassifier cl, String id, double payoff) {
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		//double avgDC = (Double) RunEnvironment.getInstance().getParameters().getValue("avgDegreeCentrality"); 
		OrgMember om = null;
		Iterable<OrgMember> members = RunState.getInstance().getMasterContext().getObjects(OrgMember.class);
		for(OrgMember member: members)
			if(member.getID().equals(id)){
				om = member;
				break;
			}
		if(net.isAdjacent(focalMember, om) && payoff > 0) {
			RepastEdge<OrgMember> tie = net.getEdge(focalMember, om);
			//double oldWeight = tie.getWeight();
			tie.setWeight(tie.getWeight() + Constants.tieIncrease);
			//double newWeight = tie.getWeight();
			//if(newWeight - oldWeight > 1){
			//System.out.println("Environment.java L538. CurrentWeight = " + tie.getWeight() + "\n");
			//}
			NetworkAnalysis.updateTieStrength(Integer.parseInt(focalMember.getID()), Integer.parseInt(om.getID()), Constants.tieIncrease, false);
		}
		else if (net.isAdjacent(focalMember, om) && payoff <= 0) {
			RepastEdge<OrgMember> tie = net.getEdge(focalMember, om);
			double newWeight = tie.getWeight() - Constants.negOverPos*Constants.tieIncrease;
			if(newWeight <= Constants.tieDecayMin)
				net.removeEdge(tie);
			else tie.setWeight(newWeight);
			//System.out.println("Environment.java L538. CurrentWeight = " + tie.getWeight() + "\n");
			NetworkAnalysis.updateTieStrength(Integer.parseInt(focalMember.getID()), Integer.parseInt(om.getID()), -1*Constants.tieIncrease*Constants.negOverPos, false);
		}
		else if(!net.isAdjacent(focalMember, om) && payoff > 0){
			/*if(Constants.switch_DKS == 2){
				//String action = Integer.toBinaryString(cl.getAction());
				//char signalBit = action.charAt(action.length() - 1);
				//use ">" instead of ">=" for the situation that avgDC = 0
				//if (focalMember.getNDPreference() == 0 && net.getDegree(focalMember) > avgDC
					//		&& avgDC >= (net.size()-1) * Constants.denseNetThresh){
				if (focalMember.getNDPreference() == 0 && net.getDegree(focalMember) > 0){
					Iterator<RepastEdge<OrgMember>> it = net.getEdges(focalMember).iterator();
					RepastEdge<OrgMember> weakTie = it.next();
					while (it.hasNext()) {
						RepastEdge<OrgMember> e = it.next();
						if (e.getWeight() < weakTie.getWeight())
							weakTie = e;
					}
					net.removeEdge(weakTie);
				}
			}*/   	
			//if the historical tie strength is negative, the reactivated tie's initial strength is at least half of a new tie (0.5)
			//if the historical tie strength is positive, it will be added to the initial strength, represent that fewer construction efforts are needed.
			//Since this statement locates at a else branch that the tie does not exist, NetworkAnalysis.tieHistory[row][col] should be not more than Constants.tieDecayMin
			double newRecord = NetworkAnalysis.updateTieStrength(Integer.parseInt(focalMember.getID()), Integer.parseInt(om.getID()), Constants.tieIncrease, false);
			//System.out.println("EnvironmentL569: the new history strength is" + newRecord +"\n");
			net.addEdge(focalMember, om, Constants.tieIncrease + Math.max(-0.5, newRecord - Constants.tieIncrease));
			//System.out.println("EnvironmentL574: the initial strength is" + (Constants.tieIncrease + Math.max(-0.5, newRecord - Constants.tieIncrease)) +"\n");
		}
		else
			NetworkAnalysis.updateTieStrength(Integer.parseInt(focalMember.getID()), Integer.parseInt(om.getID()), -1*Constants.tieIncrease*Constants.negOverPos, false);
	}
	/**
	 * Add the time this agent spent in its task to the sum of agents' time cost.
	 * Zero time cost means the agent is initially qualified for its task (i.e., no need to obtain more knowledge).
	 * If this is the case, reduce the number of active learners by 1.
	 */
	public void reportTimeCost() {
		if(timeCost == 0){ //no need to learn or self-learn at the first step
			int activeLearner = (Integer) RunEnvironment.getInstance().getParameters().getValue("activeLearner");
			activeLearner--;
			RunEnvironment.getInstance().getParameters().setValue("activeLearner", activeLearner);
			return;
		}
		int currentProblem = (Integer) RunEnvironment.getInstance().getParameters().getValue("currentProblem");
		avgTimeCost[currentProblem - 1] += timeCost;
		if(avgTimeCost[currentProblem-1] <= 0)
			System.out.println("EnvironmentL503");
	}
	/**
	 * Reset some fields of the task environment to prepare for a new task assignment.
	 * This function is called after the task has been assigned. Thus, for existing specialty areas
	 * that are not task-required, their states should be "-1". For all existing areas that are task-required areas,
	 * their states should be "2"
	 */
	public void reset() {
		timeCost = 0;
		if(sourceList!=null) sourceList.clear();
		for (Entry<Integer, SpecialtyArea> expertise : focalMember.myExpertise.entrySet()){
			boolean flag = false;
			int task = expertise.getKey();
			SpecialtyArea sa = expertise.getValue();
			for(int j = 0; j < currentTask.length; j++)
				if(task == currentTask[j]){
					flag = true;
					break;
				}
			if(flag) sa.setState(2);
			else sa.setState(-1);			
		}
	}

	public double getReplyProb(String requesterID) {
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		ContextJungNetwork<OrgMember> replicNet = net;
		OrgMember secondParty = null;
		Iterable<OrgMember> members = RunState.getInstance().getMasterContext().getObjects(OrgMember.class);
		for(OrgMember member: members)
			if(member.getID() == requesterID){
				secondParty = member;
				break;
			}
		//The tie weight used here is not the actual value: we can only know that after the reply actually happens.
		//So the tie weight used here (initial or added value) is an assumed value.
		if(net.isAdjacent(focalMember, secondParty)){
			double oldWeight = replicNet.getEdge(focalMember, secondParty).getWeight();
			replicNet.getEdge(focalMember, secondParty).setWeight(oldWeight+1.0);
		}
		else
			replicNet.addEdge(focalMember, secondParty, 1.0);
		double utilityOfReply = NetworkAnalysis.constraint(replicNet, focalMember);
		double utilityOfPostpone = NetworkAnalysis.constraint(net, focalMember);
		int preferenceConstant = -1;
		if(focalMember.getSCPreference() < 0){//the value of preference can be 0, 1, -1, or -2, so < 0 means either -1 or -2
			preferenceConstant = (Constants.drand() < 0.5)? 1:(-1);
		}
		else{
			//bonding preference = 0; bridging preference = 1
			preferenceConstant = (focalMember.getSCPreference() == 0)? 1:(-1);
		}
		double replyProb = 1/(1 + Math.exp(preferenceConstant*(utilityOfReply - utilityOfPostpone)));
		return replyProb;
	}

	/**
	 * Remove ties which have become negative because there are too many non-replied messages
	 */
	public void clearLocalNet() {
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		ArrayList<RepastEdge<OrgMember>> tieCollection = new ArrayList<RepastEdge<OrgMember>>();
		int row = 0;
		int col = 0;
		String agentA = null;
		String agentB = null;
		double oldWeight = 0.;
		double newWeight = 0.;
		for(RepastEdge<OrgMember> localTie : net.getEdges(focalMember)){
			agentA = localTie.getSource().getID();
			agentB = localTie.getTarget().getID();
			row = (Integer.parseInt(agentA) <= Integer.parseInt(agentB))? (Integer.parseInt(agentA) - 1):(Integer.parseInt(agentB) - 1);
			col = (Integer.parseInt(agentA) <= Integer.parseInt(agentB))? (Integer.parseInt(agentB) - 1):(Integer.parseInt(agentA) - 1);
			oldWeight = localTie.getWeight();
			//newWeight = oldWeight - Constants.negOverPos*NetworkAnalysis.tieUsePerProblem[row][col];
			if(NetworkAnalysis.tieUsePerProblem[row][col] > 0){
				newWeight = oldWeight - Constants.negOverPos;
				NetworkAnalysis.updateTieStrength(Integer.parseInt(agentA), Integer.parseInt(agentB), newWeight - oldWeight, false);
				NetworkAnalysis.tieUsePerProblem[row][col] = 0; //this is necessary, since when update historical tie strength, it gets deleted only once
				if(newWeight <= Constants.tieDecayMin)
					tieCollection.add(localTie);
				else localTie.setWeight(newWeight);
			}
		}
		for(RepastEdge<OrgMember> tie: tieCollection)
			net.removeEdge(tie);
	}
}