package orgKTNet;

import java.io.BufferedWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import repast.simphony.engine.environment.RunEnvironment;

/**
 * This class handles the different sets of classifiers. Each set of classifiers is stored in an arrayList with changeable size.
 * The class provides constructors for constructing the empty population, match and action sets.
 * It executes GA on the action set and updates classifier-related parameters in each set. 
 * It also handles addition, deletion and subsumption of classifiers.
 * 
 * @author Martin V. Butz
 * @version XCSJava 1.0
 * @since JDK1.1
 */
public class XClassifierSet implements Serializable {
	protected static final long serialVersionUID = 1L;

	/**
	 * The cons' parameters are necessary for all kinds of calculations in a classifier set.
	 * The cons is static so that there is only one instance for all classifier sets. 
	 * @see the class Constants class, which is not reconstructed each time a new
	 */
	protected static Constants cons = new Constants();

	/**
	 * The aggregate numerosity of all classifiers in one set.
	 */
	protected int numerositySum;

	/**
	 * Each set keeps a reference to the parent set out of which it was generated.
	 * For example, this field of the match set is the population set.
	 * For the population set, this field is set to null.
	 */
	protected XClassifierSet parentSet;

	/**
	 * The set of classifiers
	 */
	protected ArrayList<XClassifier> clSet;

	//public int addedClassNumber;
	//public int removedClassifiersNumber;

	/**
	 * Construct a new, empty population set
	 */
	public XClassifierSet() {
		numerositySum = 0;
		parentSet = null;
		clSet = new ArrayList<XClassifier>();
	}

	/**
	 * Constructs a match set out of the population set given a specific classifier condition (String state).
	 * After the construction, check whether all possible actions have been covered in the match set.
	 * If not, a covering mechanism is triggered to generate classifiers whose action parts are the missing action(s).
	 * The condition parts of these "covering" classifiers are set to be the value of the "state" parameter. 
	 * The "covering" classifiers will be added to both the match and the population sets (traverse backward the parentSet pointer to the population set).
	 * If the maximal size limit of the population set is reached during covering, one classifier needs to be deleted from that set. @see #deleteFromPopulation
	 * If a macro-classifier is eliminated from the population set, it needs to be removed from the match set as well.
	 * In case the deletion causes the missing of another action, the covering and deletion procedures are placed inside a do...while loop.
	 * Initially the population set is empty, @see XClassifierSet#XClassifierSet(), so all entries of the actionCovered array is "flase."
	 * Then through the do...while loop new classifiers with those specific actions are built and added to both the match
	 * @param state
	 *            the specific classifier condition string.
	 * @param pop
	 * 			  The current population of classifiers.
	 * @param counter
	 *            The number of problems the XCS has learned from so far.
	 * @param numberOfActions
	 *            The number of possible actions.
	 */
	public XClassifierSet(String state, XClassifierSet pop, int counter, int numberOfActions) {
		parentSet = pop;
		numerositySum = 0;
		clSet = new ArrayList<XClassifier>();

		boolean[] actionCovered = new boolean[numberOfActions];
		for (int j = 0; j < actionCovered.length; j++) {
			actionCovered[j] = false;
		}

		for (XClassifier cl : pop.getClassifierSet()) {
			if (cl.match(state)) {
				if(cl.getNumerosity()==0)
					System.out.println("XClassifierSetL93");
				addClassifier(cl);
				actionCovered[cl.getAction()] = true;
			}
		}

		boolean flag;
		do {
			flag = false;
			for (int i = 0; i < actionCovered.length; i++) {
				if (!actionCovered[i]) {
					XClassifier newCl = new XClassifier(numerositySum + 1, counter, state, i);
					addClassifier(newCl);
					pop.addClassifier(newCl);
				}
			}
			while (pop.numerositySum > Constants.maxPopSize) {
				XClassifier cdel = pop.deleteFromPopulation();
				if (cdel != null){
					Iterator<XClassifier> it = this.clSet.iterator();
					while (it.hasNext()) {
						XClassifier c = it.next();
						if(c.equal(cdel.getCondition(), cdel.getAction())){
							c.addNumerosity(-1);
							numerositySum--;
							//In the if statement below, use <= instead of == because sometimes the numerosity of the 
							//classifier has been reduced to zero, addNumerosity(-1) makes the value becomes -1
							if (c.getNumerosity() <= 0) {
								it.remove();//Do NOT use removeClassifier(c), which will causes concurrent modification exception
								if (!isActionCovered(c.getAction())) {
									flag = true;
									actionCovered[c.getAction()] = false;
								}
							}
						}
					}
				}
			}
		} while (flag);
	}

	/**
	 * Construct an action set out of the given match set.
	 * @param matchSet
	 *            The current match set
	 * @param action
	 *            The chosen action for the action set.
	 */
	public XClassifierSet(XClassifierSet matchSet) {
		parentSet = matchSet;
		numerositySum = 0;
		clSet = new ArrayList<XClassifier>();
	}

	/**
	 * Add a given classifier to the current classifier set.
	 * If an identical classifier already exists, increase the numerosity of this classifier in the set;
	 * otherwise add the new classifier (its initial numerosity 1) to the set. 
	 * Increase the numerosity sum of the current set and all its parent sets (if any).
	 * @param cl
	 * 		the given classifier
	 */
	public void addClassifier(XClassifier cl) {
		XClassifier oldcl = getMatchClassifier(cl.getCondition(), cl.getAction(), true);
		if (oldcl != null)
			oldcl.addNumerosity(cl.getNumerosity());
		else
			//add a duplicate classifier to the population set.
			//add(cl) will add a reference of the same object(cl) to two sets
			clSet.add(new XClassifier(cl, true));
		numerositySum += cl.getNumerosity();
	}

	/**
	 * Delete one classifier from the population set based on roulette wheel selection.
	 * Consider the deletion vote for the classifier. @see XClassifier#getDelProp
	 * This method must be called by the population set.
	 * @return
	 * 		the macro-classifier whose micro-classifiers got decreased by one.
	 */
	protected XClassifier deleteFromPopulation() {
		double meanFitness = getFitnessSum() / (double) numerositySum;
		double sum = 0.;

		for (XClassifier cl : clSet) {
			sum += cl.getDelProp(meanFitness);
		}

		double choicePoint = sum * Constants.drand();
		sum = 0.;
		Iterator<XClassifier> it = clSet.iterator();
		while (it.hasNext()) {
			XClassifier cl = it.next();
			sum += cl.getDelProp(meanFitness);
			if (sum > choicePoint) {
				cl.addNumerosity(-1);
				numerositySum --;
				if (cl.getNumerosity() <= 0)
					it.remove();
				return cl;
			}
		}
		return null;
	}

	/**
	 * First find the most general subsumer classifier in the action set; then subsume
	 * all action-set classifiers that are more specific than the selected one.
	 * The subsumed classifiers are removed from both the action and the population sets.
	 * This method must be called by the action set. 
	 * @see XClassifier#isSubsumer
	 * @see XClassifier#isMoreGeneral
	 */
	protected void doActionSetSubsumption() {
		XClassifierSet pop = this;
		while (pop.parentSet != null)
			pop = pop.parentSet;

		XClassifier subsumer = null;
		for (XClassifier cl : clSet) {
			if (cl.isSubsumer())
				if (subsumer == null || cl.isMoreGeneral(subsumer))
					subsumer = cl;
		}

		if (subsumer != null) {
			Iterator<XClassifier> it = clSet.iterator();
			while (it.hasNext()) {
				XClassifier c = it.next();
				if (subsumer.isMoreGeneral(c)) {
					int num = c.getNumerosity();
					subsumer.addNumerosity(num);
					c.addNumerosity((-1) * num);
					//there is no change in the numerosity sum
					pop.removeClassifier(c);
					it.remove();
				}
			}
		}
	}


	/**
	 * @return the classifier set
	 */
	public ArrayList<XClassifier> getClassifierSet() {
		return clSet;
	}

	/**
	 * @return the sum of classifier fitness.
	 */
	protected double getFitnessSum() {
		double sum = 0.;
		for (XClassifier cl : clSet)
			sum += cl.getFitness();
		return sum;
	}

	/**
	 * Search the match set for a classifier whose condition and action parts match those specified by parameters.
	 * @param condition
	 * 		the specified condition
	 * @param action
	 * 		the specified action
	 * @param identical
	 * 		indicate whether the two classifier should be exactly the same in condition and action.
	 * @see XClassifier#equals(String, int)
	 * @see XClassifier#match(String)
	 * @return 
	 * 		a classifier with more specific condition and action parts.
	 */
	public XClassifier getMatchClassifier(String condition, int action, boolean identical) {
		for (XClassifier cl : clSet){
			if (identical && cl.equal(condition, action))
				return cl;
			else if(!identical && cl.match(condition) && cl.getAction() == action)
				return new XClassifier(-1, -1, condition, action);
		}
		return null;
	}
	/**
	 * @return the number of micro-classifiers in the set.
	 */
	public int getNumerositySum() {
		return numerositySum;
	}

	/**
	 * @return the sum of the prediction values of all classifiers in the set.
	 */
	protected double getPredictionSum() {
		double sum = 0.;

		for (XClassifier cl : clSet) {
			sum += cl.getPrediction() * cl.getNumerosity();
		}
		return sum;
	}

	/**
	 * @return the number of macro-classifiers in the set.
	 */
	public int getSize() {
		return clSet.size();
	}

	/**
	 * @return the average of the time stamps in the set.
	 */
	protected double getTimeStampAverage() {
		return getTimeStampSum() / numerositySum;
	}

	/**
	 * @return the sum of the time stamps of all micro-classifiers in the set, 
	 */
	protected double getTimeStampSum() {
		double sum = 0.;

		for (XClassifier cl : clSet) {
			sum += cl.getTimeStamp() * cl.getNumerosity();
		}
		return sum;
	}

	/**
	 * RECURSIVELY increase the numerositySum values of a classifier set and all parent sets.
	 * This method intends to keep the numerosity sums of all sets, mainly the population set, up to date.
	 * @param nr
	 * 		the value to be added to the numerosity sum
	 */
	protected void increaseNumerositySum(int nr)
	{
		numerositySum += nr;
		if (parentSet != null)
			parentSet.increaseNumerositySum(nr);
	}

	/**
	 * Insert GA-generated classifiers into the population set while keeping the latter's maximal size limit.
	 * The parent classifiers o the newly generated classifiers have already been inserted into the population set.
	 * Conduct GA subsumption in certain probability. @see Constants#doGASubsumption
	 * 
	 * @see #subsumeXClassifier
	 * @see #addXClassifierToPopulation
	 * @see Constants#maxPopSize
	 * @see #deleteFromPopulation
	 * @param cl1
	 *            The first classifier generated through GA.
	 * @param cl2
	 *            The second classifier generated through GA.
	 * @param cl1P
	 *            The first parent of the two new classifiers.
	 * @param cl2P
	 *            The second parent of the two new classifiers.
	 */
	protected void insertDiscoveredXClassifiers(XClassifier cl1, XClassifier cl2,
			XClassifier cl1P, XClassifier cl2P) {
		XClassifierSet pop = this;
		while (pop.parentSet != null)
			pop = pop.parentSet;

		if (Constants.doGASubsumption) {
			subsumeXClassifier(cl1, cl1P, cl2P);
			subsumeXClassifier(cl2, cl1P, cl2P);
		} else {
			pop.addClassifier(cl1);
			pop.addClassifier(cl2);
		}

		while (pop.numerositySum > Constants.maxPopSize)
			pop.deleteFromPopulation();
	}

	/**
	 * Return whether the specified action is covered by some classifier in this set.
	 * @param action
	 * 		the specified action
	 */
	protected boolean isActionCovered(int action) {
		for (XClassifier cl : clSet) {
			if (cl.getAction() == action)
				return true;
		}
		return false;
	}

	/**
	 * Print the classifier set to the specified writer (a file).
	 * @param writer
	 * @throws Exception
	 */
	public void printSet(BufferedWriter writer) throws Exception {
		writer.write("Averages:");
		writer.write("Prediction: " + (getPredictionSum() / numerositySum)
				+ " Fitness: " + (getFitnessSum() / numerositySum)
				+ " TimeStamp: " + (getTimeStampSum() / numerositySum)
				+ " Numerosity: " + numerositySum + "\n");
		writer.write("Classifier Prediction PredicError Fitness Numerosity Experience ActionSetSize TimeStamp" + "\n");
		for (XClassifier cl : clSet) {
			cl.printXClassifier(writer);
		}
	}

	/**
	 * Removes a probably macro-level classifier from the classifier set. 
	 * This protected method is the last step of classifier deletion and is called by other more complex classifier-deleting methods,
	 * so it neither updates the numerosity of the set nor recursively removes classifiers in the parent set. Those related clear-ups are done by the caller methods.
	 * @see #deleteFromPopulation, #doActionSetSubsumption
	 * @param cl
	 * 		the specified classifier
	 */
	protected boolean removeClassifier(XClassifier cl)
	{
		return clSet.remove(cl);
	}

	/**
	 * This method executes the genetic algorithm (GA) in certain probability. If a GA takes place, two
	 * classifiers are selected by roulette wheel selection, possibly recombined (cross-over), mutated and then inserted.
	 * 
	 * @see Constants#theta_GA
	 * @see #selectXClassifierRW
	 * @see XClassifier#twoPointCrossover
	 * @see XClassifier#applyMutation
	 * @see Constants#predictionErrorReduction
	 * @see Constants#fitnessReduction
	 * @see #insertDiscoveredXClassifiers
	 * @param time
	 *            The actual number of problems the XCS has learned from so far.
	 * @param numberOfActions
	 *            The number of possible actions.
	 */
	public void runGA(int time, int numberOfActions) {
		// Don't do a GA if the theta_GA threshold is not reached, yet
		if (clSet.isEmpty() || time - getTimeStampAverage() < Constants.theta_GA)
			return;

		setTimeStamps(time);

		double fitSum = getFitnessSum();
		// Select two XClassifiers with roulette Wheel Selection
		XClassifier cl1P = selectXClassifierRW(fitSum);
		XClassifier cl2P = selectXClassifierRW(fitSum);

		XClassifier cl1 = new XClassifier(cl1P, false);
		XClassifier cl2 = new XClassifier(cl2P, false);

		cl1.twoPointCrossover(cl2);

		cl1.applyMutation(numberOfActions);
		cl2.applyMutation(numberOfActions);

		cl1.setPrediction((cl1.getPrediction() + cl2.getPrediction()) / 2.);
		cl1.setPredictionError(Constants.predictionErrorReduction
				* (cl1.getPredictionError() + cl2.getPredictionError()) / 2.);
		cl1.setFitness(Constants.fitnessReduction
				* (cl1.getFitness() + cl2.getFitness()) / 2.);
		cl2.setPrediction(cl1.getPrediction());
		cl2.setPredictionError(cl1.getPredictionError());
		cl2.setFitness(cl1.getFitness());

		insertDiscoveredXClassifiers(cl1, cl2, cl1P, cl2P);
	}

	/**
	 * Select one classifier using roulette wheel selection according to the fitnesses of the classifiers.
	 * @param fitSum
	 * @return
	 * 		the selected classifier
	 */
	protected XClassifier selectXClassifierRW(double fitSum) {
		double choiceP = Constants.drand() * fitSum;
		double sum = 0.;
		for (XClassifier cl : clSet) {
			sum += cl.getFitness();
			if (sum > choiceP)
				return cl;
		}
		return null;
	}

	/**
	 * Set the time stamp of all classifiers in the set to the given time.
	 * @param time
	 */
	protected void setTimeStamps(int time) {
		for (XClassifier cl : clSet)
			cl.setTimeStamp(time);
	}

	/**
	 * Try to subsume a classifier in the current set (usually the action set).
	 * When there are more than one subsumer candidates, choose one randomly.
	 * If no subsumer was found, add the classifier to the population set,
	 * where an identical classifier may exist. @see #addClassifier
	 * @param cl
	 * 		The classifier this method tries to subsume.
	 */
	protected void subsumeXClassifier(XClassifier cl)
	{
		Vector<XClassifier> choices = new Vector<XClassifier>();
		for (XClassifier c : clSet) {
			if (c.subsumes(cl))
				choices.addElement(c);
		}

		if (choices.size() > 0) {
			int choice = (int) (Constants.drand() * choices.size());
			choices.elementAt(choice).addNumerosity(1);
			increaseNumerositySum(1);
			return;
		}
		
		XClassifierSet pop = this;
		while (pop.parentSet != null)
			pop = pop.parentSet;
		pop.addClassifier(cl);
	}

	/**
	 * Try to subsume a classifier in its parents. If this effort is unsuccessful,
	 * try to subsume the same classifier in the current set (call the other subsumeXClassifier function).
	 * @see #subsumeXClassifier
	 */
	protected void subsumeXClassifier(XClassifier cl, XClassifier cl1P,
			XClassifier cl2P) {
		if (cl1P != null && cl1P.subsumes(cl)) {
			increaseNumerositySum(1);
			cl1P.addNumerosity(1);
		} else if (cl2P != null && cl2P.subsumes(cl)) {
			increaseNumerositySum(1);
			cl2P.addNumerosity(1);
		} else {
			subsumeXClassifier(cl); 
		}
	}

	/**
	 * Update the fitness of the classifiers in the set.
	 * First calculate the accuracy values of each classifier and the accuracy sums.
	 * Then update the fitness of each classifier using accuracy values and sums.
	 * @see XClassifier#updateFitness
	 */
	protected void updateFitnessSet() {
		double accuracySum = 0.;

		double[] accuracies = new double[clSet.size()];

		for (int i = 0; i < clSet.size(); i++) {
			XClassifier cl = clSet.get(i);
			accuracies[i] = cl.getAccuracy();
			accuracySum += accuracies[i] * cl.getNumerosity();
		}
 
		for (int i = 0; i < clSet.size(); i++) {
			XClassifier cl = clSet.get(i);
			cl.updateFitness(accuracySum, accuracies[i]);
		}
	}

	/**
	 * This method is called when an agent has finished its task for the current problem.
	 * Recall that the agent obtains beneficial classifiers and temporary rewards
	 * from reply messages while carrying out the task. These classifiers are accumulated in the action set,
	 * with their temporary fitness continuously updated. @see #update(Message, double).
	 * This method puts a final touch on these accumulated classifiers by finalizing the values of their attributes.
	 * To be more conservative, the prediction error is updated before the prediction. The fitness is updated after
	 * prediction and prediction error. Then the fitness is updated.
	 * In the end, this method executes action-set subsumption if it is selected. @see Constants#doActionSetSubsumption
	 * @see Constants#gamma
	 * @see XClassifier#increaseExperience
	 * @see XClassifier#updatePreError
	 * @see XClassifier#updatePrediction
	 * @see XClassifier#updateActionSetSize
	 * @see #updateFitnessSet
	 * @see #doActionSetSubsumption
	 */
	public void updateSet() {
		if(clSet==null)
			return;
		for (XClassifier cl : clSet) {
			double reward = cl.getTempFitness();
			cl.increaseExperience();
			cl.updatePreError(reward);
			cl.updatePrediction(reward);
			cl.updateActionSetSize(numerositySum);
			cl.setTempFitness(0.0);
		}
		updateFitnessSet();

		if (Constants.doActionSetSubsumption)
			doActionSetSubsumption();
	}

	/**
	 * Update current action set when the host agent is unsatisfied with its performance.
	 * The host agent will change its preference to the opposite (0-->1 or 1-->0) if (a) it has no contacts (no social influence), or
	 * (b) among all better-performing contacts, the number of bonding or bridging preferences is equal (balanced social influence), 
	 * or (c) no one performs better than the agent. 
	 * In other conditions, the host agent will take the majority preference of its better-performing, close contacts, or take the
	 * preference of any one of its best-performing close contacts (there may be more than one). 
	 * After determining the new preference, the agent will remove from its current action set all classifiers that do not match this preference.
	 * Only when a contact is active in knowledge transfer at this problem-solving period will the focal agent learns from it, because otherwise the contact's actionSet is null.
	 * @param myEnv
	 * 		the focal agent's task environment
	 */
	public void updateSet(Environment myEnv) {
		int currentSCPref = myEnv.focalMember.getSCPreference();
		int currentNDPref = myEnv.focalMember.getNDPreference();
		ArrayList<OrgMember> closeContacts = myEnv.getCloseContacts();
		if(closeContacts == null){
			System.out.println("This agent has no contact.");
			myEnv.focalMember.setSCPreference ((Constants.drand() < 0.5)? currentSCPref:(1-currentSCPref)); // if it is 1 - currentSCPref, then 0-->1 and 1-->0
			myEnv.focalMember.setNDPreference ((Constants.drand() < 0.5)? currentNDPref:(1-currentNDPref)) ;
		}
		else{
			int numOfBondPref = 0;
			int numOfMoreDegree = 0;
			int minTC = myEnv.timeCost;
			int bestSCPref = (Constants.drand() < 0.5)? currentSCPref:(1-currentSCPref);
			int bestNDPref = (Constants.drand() < 0.5)? currentNDPref:(1-currentNDPref);
			for (OrgMember contact : closeContacts) {
				int contactTC = contact.getTaskEnvironment().timeCost;
				//**If contactTC = 1, the agent finishes early because it initially has all required knowledge.
				//**Then the agent's knowledge transfer strategies may or may not be worth learning.
				//Or, let contactTC > 0, maybe it means the agent has relatively diverse knowledge accumulated from before?
				if (myEnv.timeCost > contactTC && contactTC > 1) {
					if(Constants.switch_MOB == true){
						numOfBondPref = (contact.getSCPreference() == 0)? (numOfBondPref++) : (numOfBondPref--);
						numOfMoreDegree = (contact.getNDPreference() == 0)? (numOfMoreDegree--) : (numOfMoreDegree++);
					}
					else if(minTC > contactTC){
						minTC = contactTC;
						bestSCPref = contact.getSCPreference();
						bestNDPref = contact.getNDPreference();
					}
				}
			}
			if(Constants.switch_MOB == true){
				if(numOfBondPref > 0) myEnv.focalMember.setSCPreference(0);
				else if(numOfBondPref < 0) myEnv.focalMember.setSCPreference(1);
				else myEnv.focalMember.setSCPreference((Constants.drand() < 0.5)? currentSCPref:(1-currentSCPref));
				
				if(numOfMoreDegree > 0) myEnv.focalMember.setNDPreference(1);
				else if(numOfMoreDegree < 0) myEnv.focalMember.setSCPreference(0);
				else myEnv.focalMember.setNDPreference((Constants.drand() < 0.5)? currentNDPref:(1-currentNDPref));
			}
			else{
				//when minTC == myEnv.timeCost (no one else is better), the focal member will change its preferences anyway.
				//bestSCPref and bestNDPref are defined using flip values (50% percent probability of flip).
				myEnv.focalMember.setSCPreference(bestSCPref);
				myEnv.focalMember.setNDPreference(bestNDPref);		
			}
		}
		int newSCPref = myEnv.focalMember.getSCPreference();
		int newNDPref = myEnv.focalMember.getNDPreference();
		int newStrategy = newSCPref*4 + newNDPref*2 + myEnv.focalMember.getRBPreference();
		if(myEnv.focalMember.strategyChain.peekLast() != newStrategy) myEnv.focalMember.strategyChain.add(newStrategy);
		if(clSet==null)
			return;
		for(Iterator<XClassifier> classifiers = clSet.iterator(); classifiers.hasNext();){
			XClassifier cl = classifiers.next();
			int clPref = cl.getAction();
			String preferences = String.format("%3s", Integer.toBinaryString(clPref)).replace(
					" ", "0");
			if(Integer.parseInt(preferences.substring(0,1))!= newSCPref || Integer.parseInt(preferences.substring(1,2)) != newNDPref)		
				classifiers.remove();//just remove it from the actionSet so that it won't be added into the population set later
		}
	}

	/**
	 * Update the temporary fitness (used to calculate the final fitness) of classifiers.
	 * This method is called every time when the focal agent acquires knowledge from a reply message. @see OrgMember#step()
	 * Each message corresponds to a classifier in the focal agent's action set, which was added when the agent sent out the request.
	 * The temporary fitness of this classifier will be increased by rewards from the message - acquired knowledge divided by the time cost,
	 * that is, the average learning rate resulting from that classifier. 
	 * @param msg
	 * 		the message from which the focal agent acquires useful knowledge
	 * @param reward
	 * 		the value to be added to the temporary fitness
	 * @return
	 * 		true if an update happens
	 */
	public boolean updateSet(Message msg, double reward) {
		String msgClCond = msg.getBaseCl().getCondition();
		int msgClAct = msg.getBaseCl().getAction();
		for (XClassifier cl : clSet) {
			if (cl.equal(msgClCond, msgClAct)) {
				//Use accumulation because one classifier may correspond to multiple knowledge sources
				//and thus get rewards for multiple times.
				cl.setTempFitness(cl.getTempFitness() + reward); 
				return true;
			}
		}
		return false;
	}
}
