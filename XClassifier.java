package orgKTNet;

import java.io.BufferedWriter;
import java.io.Serializable;

/**
 * Each instance of this class represents a classifier. The class has
 * different constructors that respectively generate
 * <ul>
 * <li>a copy of existing classifiers,
 * <li>a new classifier with matching condition and random action,
 * <li>a new classifier with matching condition and specified action, or
 * <li>a new completely random classifier.
 * </ul>
 * The class implements classifier mutation and crossover. It provides methods to get and change the value of each classifier
 * attribute. It also handles various types of comparisons between classifiers.
 * @author Martin V. Butz
 * @version XCSJava 1.0
 * @since JDK1.1
 */
public class XClassifier implements Serializable {

	protected static final long serialVersionUID = 1L;
	/**
	 * The condition part of a classifier is a 7-bit string.
	 * The first 4 bits indicate the knowledge requester's network position,
	 * whereas the rest 3 bits indicate a potential or an actual knowledge source's network position.
	 */
	protected String condition;
	/**
	 * The action part of a classifier is an integer from [0, 7]. Each integer from this range corresponds to a unique 3-bit string.
	 * The first bit explains whether the host agent (the recipient) views its social relationship with the corresponding knowledge source as bonding (0) or bridging (1) social capital.
	 * The second bit signifies whether the host agent allows (1) or avoids (0) expending direct connections.
	 * The third bit signifies whether the host agent will reply a request only if it currently has higher expertise (0) or also when it is currently learning that expertise (1)
	 */
	protected int action;
	/**
	 * The reward prediction value of this classifier.
	 */
	protected double prediction;
	/**
	 * The reward prediction error of this classifier.
	 */
	protected double predictionError;
	/**
	 * The fitness of the classifier in terms of the macro-classifier.
	 */
	protected double fitness;
	protected double tempFitness;
	/**
	 * In a classifier set, classifiers are arranged at the macro and micro levels.
	 * Numerosity is the number of micro-classifier included in a macro-classifier.
	 * A macro-classifier usually have multiple micro-classifiers (i.e., numerosity > 1)
	 */
	protected int numerosity;
	/**
	 * The experience of the classifier: the number of problems the classifier has learned from so far.
	 */
	protected int experience;
	/**
	 * The action set size estimate of the classifier.
	 */
	protected double actionSetSize;
	/**
	 * The time when the last GA application took place in this classifier.
	 * It equals to the number of problems that the XCS has learned from so far.
	 */
	protected int timeStamp;
	/**
	 * It includes the values of all parameters used in the model. Static assures that
	 * the Constants are not generated for each classifier separately.
	 */
	protected static Constants cons = new Constants();
	/**
	 * Construct a classifier with matching condition and random action.
	 * @param setSize
	 *            The size of the current set which the new classifier matches.
	 * @param time
	 *            The actual number of problems the XCS has learned from so far.
	 * @param numberOfActions
	 *            The number of different actions to chose from (This should be
	 *            set to the number of actions possible in the problem).
	 * @param situation
	 *            The current problem instance/perception.
	 */
	public XClassifier(double setSize, int time, int numberOfActions, String situation) {
		createMatchingCondition(situation);
		createRandomAction(numberOfActions);
		classifierSetVariables(setSize, time);
	}
	/**
	 * Construct a classifier with matching condition and specified action.
	 * @param setSize
	 *            The size of the current set which the new classifier matches.
	 * @param time
	 *            The actual number of problems the XCS has learned from so far.
	 * @param situation
	 *            The current problem instance/perception.
	 * @param act
	 *            The action of the new classifier.
	 */
	public XClassifier(double setSize, int time, String situation, int act) {
		createMatchingCondition(situation);
		action = act;
		classifierSetVariables(setSize, time);
	}
	/**
	 * Construct a new classifier similar to the parameter classifier. If it is NOT a duplicate, the experience of the new
	 * classifier is set to 0 and the numerosity is set to 1 since this is indeed a new individual classifier in a population.
	 * @param clOld
	 *        	The classifier to be copied.
	 * @param duplicate
	 * 			indicate whether the new classifier and the old one are identical.
	 */
	public XClassifier(XClassifier clOld, boolean duplicate) {
		condition = new String(clOld.condition);
		action = clOld.action;
		this.prediction = clOld.prediction;
		this.predictionError = clOld.predictionError;
		this.actionSetSize = clOld.actionSetSize;
		this.timeStamp = clOld.timeStamp;
		if(duplicate){
			this.fitness = clOld.fitness;
			this.numerosity = clOld.numerosity;
			this.experience = clOld.experience;
			this.tempFitness = clOld.tempFitness;
		}
		else{
			// Here the fitness should be divided by the numerosity to get a accurate value for the new one!
			this.fitness = clOld.fitness / clOld.numerosity;
			this.numerosity = 1;
			this.experience = 0;
			this.tempFitness = 0.0;
		}

	}
	/**
	 * Add a specified value to the numerosity of the classifier.
	 * @param num
	 * 		The value to be added (it can be negative).
	 */
	public void addNumerosity(int num) {
		numerosity += num;
	}
	/**
	 * Apply mutation to the classifier. Mutate its condition and/or action parts.
	 * @param numberOfActions
	 * 		the number of possible actions
	 * @return
	 * 		true if at least one bit or the classifier (condition or action part) was mutated.
	 */
	public boolean applyMutation(int numberOfActions) {
		boolean changed = mutateCondition();
		if (mutateAction(numberOfActions))
			changed = true;
		return changed;
	}
	/**
	 * Initialize the fields of a new classifier.
	 * @see Constants#predictionIni
	 * @see Constants#predictionErrorIni
	 * @see Constants#fitnessIni
	 * @param setSize
	 *            The size of the set the classifier is created in.
	 * @param time
	 *            The actual number of instances the XCS learned from so far.
	 */
	public void classifierSetVariables(double setSize, int time) {
		this.prediction = Constants.predictionIni;
		this.predictionError = Constants.predictionErrorIni;
		this.fitness = Constants.fitnessIni;

		this.numerosity = 1;
		this.experience = 0;
		this.tempFitness = 0.0;
		this.actionSetSize = setSize;
		this.timeStamp = time;
	}
	/**
	 * Create the classifier's condition part according to a specified string and some model parameter.
	 * @see XCSConstants#P_dontcare
	 * @param cond
	 * 		the specified condition string
	 */
	protected void createMatchingCondition(String cond) {
		int condLength = cond.length();
		char condArray[] = new char[condLength];

		for (int i = 0; i < condLength; i++)
			if (Constants.drand() < Constants.P_dontcare)
				condArray[i] = Constants.dontCare;
			else
				condArray[i] = cond.charAt(i);
		condition = new String(condArray);
	}
	/**
	 * Create a random action.
	 * @param numberOfActions
	 *      the total number of actions to chose from.
	 */
	protected void createRandomAction(int numberOfActions) {
		action = (int) (Constants.drand() * numberOfActions);
	}
	/**
	 * Create a condition randomly considering the constant
	 * <code>P_dontcare</code>.
	 * @see XCSConstants#P_dontcare
	 */
	protected void createRandomCondition(int condLength) {
		char condArray[] = new char[condLength];
		for (int i = 0; i < condLength; i++)
			if (Constants.drand() < Constants.P_dontcare)
				condArray[i] = Constants.dontCare;
			else if (Constants.drand() < 0.5)
				condArray[i] = '0';
			else
				condArray[i] = '1';
		condition = new String(condArray);
	}

	/**
	 * Return whether the classifier has a specific combination of condition and action.
	 * @param cond
	 * 		the condition string
	 * @param act
	 * 		the action integer
	 * @see #match(String)
	 * 		This method differs from the match method. The match method is used to create the match set and allows for "#",
	 * 		whereas this method is used to update specific classifiers in the action or population sets.
	 */
	public boolean equal(String cond, int act) {
		if (cond.equals(this.condition))
			if (act == this.action)
				return true;
		return false;
	}

	/**
	 * Return the accuracy of the classifier. The accuracy is determined from
	 * the prediction error of the classifier using Wilson's power function as
	 * published in 'Get Real! XCS with continuous-valued inputs' (1999)
	 * 
	 * @see XCSConstants#epsilon_0
	 * @see XCSConstants#alpha
	 * @see XCSConstants#nu
	 */
	public double getAccuracy() {
		double accuracy;

		if (predictionError <= Constants.epsilon_0)
			accuracy = 1.;
		else
			accuracy = Constants.alpha
					* Math.pow(predictionError / Constants.epsilon_0,
							-Constants.nu);
		return accuracy;
	}

	public int getAction() {
		return action;
	}

	public double getActionSetSize() {
		return actionSetSize;
	}

	public String getCondition() {
		return condition;
	}

	/**
	 * Return the deletion vote for the classifier.
	 * @see Constants#delta
	 * @see Constants#theta_del
	 * @param meanFitness
	 *            The mean fitness in the population set.
	 */
	public double getDelProp(double meanFitness) {
		if (fitness / numerosity >= Constants.delta * meanFitness
				|| experience < Constants.theta_del)
			return actionSetSize * numerosity;
		return actionSetSize * numerosity * meanFitness / (fitness / numerosity);
	}

	public int getExperience() {
		return experience;
	}

	public double getFitness() {
		return fitness;
	}

	public int getNumerosity() {
		return numerosity;
	}

	public double getPrediction() {
		return prediction;
	}

	public double getPredictionError() {
		return predictionError;
	}

	public double getTempFitness() {
		return tempFitness;
	}

	public int getTimeStamp() {
		return timeStamp;
	}

	/**
	 * Increase the <code>Experience</code> of the classifier by one.
	 */
	public void increaseExperience() {
		experience++;
	}

	/**
	 * Return whether this classifier is indeed more general than (not equally general as) the parameter classifier.
	 * @param 
	 *        The supposedly more specific classifier.
	 */
	public boolean isMoreGeneral(XClassifier cl) {
		boolean ret = false;
		for (int i = 0; i < condition.length(); i++)
			if (condition.charAt(i) != Constants.dontCare
					&& condition.charAt(i) != cl.condition.charAt(i))
				return false;
			else if (condition.charAt(i) != cl.condition.charAt(i))
				ret = true;
		return ret;
	}

	/**
	 * Return whether the classifier is a possible subsumer. To be a subsumer, the classifier must satisfy two conditions:
	 * (a) it has a sufficient experience; (b) its prediction error is sufficiently low.
	 * 
	 * @see Constants#theta_sub
	 * @see Constants#epsilon_0
	 */
	public boolean isSubsumer() {
		if (experience > Constants.theta_sub
				&& predictionError < Constants.epsilon_0)
			return true;
		return false;
	}

	/**
	 * Return whether the condition part of the classifier matches the given string.
	 * It is considered a match if the corresponding characters are the same or either one is a "#". 
	 * @param state
	 *         the given string
	 */
	public boolean match(String state) {
		if(condition.length()!= state.length())
			return false;
		for (int i = 0; i < state.length(); i++)
			if (condition.charAt(i) != Constants.dontCare
				&& condition.charAt(i) != state.charAt(i)
					&& state.charAt(i) != Constants.dontCare)
					return false;
		return true;
	}

	/**
	 * Mutate the action part of the classifier.
	 * @see Constants#pM
	 * @param numberOfActions
	 * 		The number of possible actions.
	 * @return
	 * 		true if the current action has changed to another possible action.
	 */
	protected boolean mutateAction(int numberOfActions) {
		boolean changed = false;
		if (Constants.drand() < Constants.pM) {
			int act = 0;
			do
				act = (int) (Constants.drand() * numberOfActions);
			while (act == action);
			action = act;
			changed = true;
		}
		return changed;
	}

	/**
	 * Mutate the condition part of the classifier. This mutation is a niche mutation.
	 * It only changes the extent of generality.
	 * @see Constants#pM
	 * @return
	 * 		true if at least one bit has been changed.
	 */
	protected boolean mutateCondition() {
		boolean changed = false;
		int condLength = condition.length();
		char[] cond = condition.toCharArray();
		for (int i = 0; i < condLength; i++)
			if (Constants.drand() < Constants.pM) {
				changed = true;
				if (cond[i] == Constants.dontCare) {
					if (Constants.drand() < 0.5)
						cond[i] = 0;
					else
						cond[i] = 1;
				} else
					cond[i] = Constants.dontCare;
			}
		condition = new String(cond);
		return changed;
	}

	/**
	 * Print the classifier to the print writer (a file).
	 * @param pW
	 *            The writer to which the classifier is written.
	 * @throws Exception
	 */
	public void printXClassifier(BufferedWriter writer) throws Exception {
		writer.write(condition
				+ "-"
				+ String.format("%3s", Integer.toBinaryString(action)).replace(
						" ", "0") + " " + (float) prediction + " "
				+ (float) predictionError + " " + (float) fitness + " "
				+ numerosity + " " + experience + " " + (float) actionSetSize
				+ " " + timeStamp + "\n");
	}

	public void setAction(int newAction) {
		action = newAction;
	}

	public void setActionSetSize(double newActionSetSize) {
		actionSetSize = newActionSetSize;
	}

	public void setCondition(String newCondition) {
		condition = newCondition;
	}

	public void setExperience(int newExperience) {
		experience = newExperience;
	}

	public void setFitness(double fit) {
		fitness = fit;
	}

	public void setPrediction(double pre) {
		prediction = pre;
	}

	public void setPredictionError(double preE) {
		predictionError = preE;
	}

	public void setTempFitness(double value) {
		tempFitness = value;
	}

	public void setTimeStamp(int ts) {
		timeStamp = ts;
	}

	/**
	 * Return whether the focal classifier subsumes the parameter classifier.
	 */
	public boolean subsumes(XClassifier cl) {
		if (cl.action == this.action)
			if (isSubsumer())
				if (isMoreGeneral(cl))
					return true;
		return false;
	}

	/**
	 * Apply two-point crossover on the focal classifier and the parameter classifier 
	 * @see Constants#pX
	 * @param cl
	 * 		The other classifier for the crossover application.
	 * @return
	 * 		the other classifier if crossover has happened; null otherwise.
	 */
	protected XClassifier twoPointCrossover(XClassifier cl) {
		boolean changed = false;
		if (Constants.drand() < Constants.pX) {
			int length = condition.length();
			int sep1 = (int) (Constants.drand() * length);
			int sep2 = (int) (Constants.drand() * length) + 1;
			if (sep1 > sep2) {
				// switch sep1 and sep2
				int help = sep1;
				sep1 = sep2;
				sep2 = help;
			} else if (sep1 == sep2)
				sep2++;
			char[] cond1 = condition.toCharArray();
			char[] cond2 = cl.condition.toCharArray();
			for (int i = sep1; i < sep2; i++)
				if (cond1[i] != cond2[i]) {
					changed = true;
					char help = cond1[i];
					cond1[i] = cond2[i];
					cond2[i] = help;
				}
			if (changed) {
				condition = new String(cond1);
				cl.condition = new String(cond2);
				return cl;
			}
			else return null;
		}
		return null;
	}

	/**
	 * Update the action set size.
	 * @see Constants#beta
	 * @param numeriositySum
	 *      The number of micro-classifiers in the population
	 */
	public double updateActionSetSize(double numerositySum) {
		if (experience < 1. / Constants.beta)
			actionSetSize = (actionSetSize * (experience - 1) + numerositySum)
					/ experience;
		else
			actionSetSize += Constants.beta * (numerositySum - actionSetSize);
		return actionSetSize * numerosity;
	}

	/**
	 * Update the fitness of the classifier according to the relative accuracy.
	 * @see XCSConstants#beta
	 * @param accSum
	 *            The sum of all the accuracies in the action set
	 * @param accuracy
	 *            The accuracy of the classifier.
	 */
	public double updateFitness(double accSum, double accuracy) {
		fitness += Constants.beta * (accuracy * numerosity / accSum - fitness);
		return fitness;// fitness already considers numerosity
	}

	/**
	 * Update the prediction of the classifier according to total reward.
	 * @see XCSConstants#beta
	 * @param P
	 *      total reward per problem.
	 */
	public double updatePrediction(double P) {
		if (experience < 1. / Constants.beta)
			prediction = (prediction * (experience - 1.) + P) / experience;
		else
			prediction += Constants.beta * (P - prediction);
		return prediction * numerosity;
	}

	/**
	 * Updates th prediction error of the classifier according to total reward.
	 * @see XCSConstants#beta
	 * @param P
	 *      total reward per problem.
	 */
	public double updatePreError(double P) {
		if (experience < 1. / Constants.beta)
			predictionError = (predictionError * (experience - 1.) + Math.abs(P
					- prediction))
					/ experience;
		else
			predictionError += Constants.beta
					* (Math.abs(P - prediction) - predictionError);
		return predictionError * numerosity;
	}
}
