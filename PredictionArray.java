package orgKTNet;

import java.io.Serializable;
import java.util.ArrayList;

import repast.simphony.random.RandomHelper;

/**
 * This class generates a prediction array of the provided set. The prediction
 * array is generated according to Wilson's Classifier Fitness Based on Accuracy
 * (Evolutionary Computation Journal, 1995). Moreover, this class provides the 
 * method to choose between two type of social capital.
 * @author Martin V. Butz
 * @version XCSJava 1.0
 */
public class PredictionArray implements Serializable {

	protected static final long serialVersionUID = 1L;

	/**
	 * The prediction array. 
	 */
	protected double[] pa;

	/**
	 * The fitness sum array. Its entries correspond to those in the prediction array.
	 */
	protected double[] nr;

	/**
	 * Constructs the prediction array according to the give classifier set and the possible number of actions.
	 * Whereas the value of each entry in the prediction array is the sum of corresponding action's accuracy-based fitness, 
	 * the value of corresponding entry in the fitness sum array is the sum of that action's fitness.
	 * @param set
	 *            The classifier set out of which a prediction array is formed (normally the match set).
	 * @param numberOfActions
	 *            The number of entries in the prediction array (should be set to the number of possible actions in the problem)
	 */
	public PredictionArray(XClassifierSet clSet, int numberOfActions) {
		pa = new double[numberOfActions];
		nr = new double[numberOfActions];

		for (int i = 0; i < numberOfActions; i++) {
			pa[i] = 0.;
			nr[i] = 0.;
		}

		ArrayList<XClassifier> set = clSet.getClassifierSet();
		for (XClassifier cl : set) {
			pa[cl.getAction()] += cl.getPrediction() * cl.getFitness();
			nr[cl.getAction()] += cl.getFitness();
		}
		for (int i = 0; i < numberOfActions; i++)
			if (nr[i] != 0)
				pa[i] /= nr[i];
			else
				pa[i] = 0;
	}

	/**
	 * Compare four types of social capital and nodal degree preferences, each corresponding to 2 types of actions (different reply based preference), and choose the one with higher pa value.
	 * Bonding social capital + avoid increasing degree (return 00 = 0) cover Actions 0(000), and 1(001)
	 * Bonding social capital + allow increasing degree (return 01 = 1) cover Actions 2(010), and 3(011)
	 * Bonding social capital + avoid increasing degree (return 10 = 2) cover Actions 4(100), and 5(101)
	 * Bridging social capital + allow increasing degree (return 11 = 3) cover Actions 6 (110) and 7(111)
	 * @see XClassifier#action
	 * @return
	 * 		the outperformed type of social capital and degree preferences that the agent should utilize
	 */
	public int bestPreferences() {
		int ret = RandomHelper.nextIntFromTo(0, 7);//to make sure that when every action has the same rewards, no one will tend to be more frequently picked
		for(int i=0; i<pa.length; i++){
		    if(pa[ret]<pa[i])
			ret=i;
		}
		return ret;
	}
}
