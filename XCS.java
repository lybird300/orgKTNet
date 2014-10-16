package orgKTNet;

import java.io.Serializable;
import java.util.Iterator;

import repast.simphony.engine.environment.RunEnvironment;

/**
 * An instance of this class is an individual agent's XCS. 
 * This class provides methods for the main learning cycles in XCS.
 * Unlike regular use of XCS, the integration of XCS here is not to find an "optimal" (in terms of
 * effectiveness and efficiency) solution to organizational knowledge transfer, but to simulate
 * the individual and social learning processes of human beings especially schema-based mechanisms.
 * 
 * @author Yuan Lin
 * @version orgKTNet 1.0
 */
public class XCS implements Serializable {

	protected static final long serialVersionUID = 1L;

	/**
	 * the three major classifier sets of a XCS.
	 */
	public XClassifierSet population;
	public XClassifierSet matchSet;
	public XClassifierSet actionSet;

	/**
	 * Constructs the XCS
	 */
	public XCS(Environment e) {
		population = new XClassifierSet();
	}

	/**
	 * Add the given classifier to the action set, which needs to be created if has not.
	 * @param cl
	 * 		the given classifier
	 * @param specificSource
	 * 		
	 * @return
	 * 		the added classifier
	 */
	/**
	 * @param cl
	 * 
	 * @return
	 */
	public XClassifier addClassifierToAction(XClassifier cl) {
		if (actionSet == null)
			actionSet = new XClassifierSet(matchSet);
		int currentProblem = (Integer) RunEnvironment.getInstance().getParameters().getValue("currentProblem"); 
		cl.setActionSetSize(actionSet.getNumerositySum()+1);
		cl.setTimeStamp(currentProblem - 1);
		actionSet.addClassifier(cl);
		return cl;
	}

	/**
	 * Decide what type of social capital the agent should utilize based on the agent's network position.
	 * 
	 * Firstly, create a match set out of the population set. The classifiers picked out should have their condition parts match the given string.
	 * The given string represents the first 4 bits of a classifier's condition part.
	 * The complete condition part has 7 bits, of which the first 4 bits represent the focal agent's network position
	 * and the last 3 bits represent a potential knowledge source's network position.
	 * The last 3 bits are not considered at this point since potential sources haven't been selected yet.
	 * @see OrgMember#step(), the list of potential sources is collected after calling this method.
	 * 
	 * Secondly, create a prediction array based on the action parts of classifiers in the match set, 
	 * and decide which social capital the focal agent should prefer.
	 * 
	 * @param state
	 * 		a string representing the condition part of a classifier
	 * @param numberOfActions
	 * 		the number of actions in the XCS (4: 2 types of social capital choice * 2 levels of capacity choice)
	 * @return the type of social capital: bonding (0) or bridging (1)
	 */
	public int decidePreferences(String state, int numberOfActions) {
		int counter = (Integer) RunEnvironment.getInstance().getParameters().getValue("currentProblem") - 1;
		matchSet = new XClassifierSet(state, population, counter, numberOfActions);
		PredictionArray pa = new PredictionArray(matchSet, numberOfActions);
		/*for(int i = 0; i< pa.pa.length; i++)
			System.out.println("pa[" + i + "]=" + pa.pa[i]+"\n");*/
		return pa.bestPreferences(); 
	}

	/**
	 * This method copies classifiers accumulated in the action set during problem-solving process
	 * to the population set. Then it runs GA (in certain probability) on the action set
	 * and concurrently update the population set. Finally, empty both the match and the action sets.
	 */
	public void learnLessons() {
		Iterator<XClassifier> actionCls = actionSet.getClassifierSet().iterator();
		while(actionCls.hasNext()){
			XClassifier actionCl = actionCls.next();
			if(actionCl.getNumerosity()<=0)
				System.out.println("XCSL97 = " + actionCl.getNumerosity());
			population.addClassifier(actionCl);
		}
		int counter = (Integer) RunEnvironment.getInstance().getParameters().getValue("currentProblem");
		actionSet.runGA(counter, 8);
		actionSet.getClassifierSet().clear();
		actionSet = null;
		matchSet.getClassifierSet().clear();
		matchSet = null;
	}
}
