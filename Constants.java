package orgKTNet;

import java.io.Serializable;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

/**
 * This class provides all relevant parameters for the model.
 * 
 * @author Yuan Lin
 * @version XCSJava 1.0
 * @since JDK1.1
 */

public class Constants implements Serializable {

	private static final long serialVersionUID = 1L;
	//***************************************************
	//A set of switches that control model versions
	/**
	 * Control whether the knowledge-gain rate in self-learning is
	 * (a)predefined and identical for all agents (value = true), or
	 * (b)following a normal distribution among agents (value = false)
	 */
	public static boolean switch_SGR = true;
	/**
	 * Control whether the knowledge-gain rate in knowledge transfer is
	 * (a)predefined and a fixed number of times more than that in self-learning (value = true), or
	 * (b)increases with the knowledge levels of both parties (value = false)
	 */
	public static boolean switch_KGR = true;
	/**
	 * Control whether the knowledge accumulation and decay functions are
	 * exponential functions (value = true) or power functions (value=false)
	 */
	public static boolean switch_KADF = true;
	/**
	 * Control whether the source and the recipient's knowledge levels counts -- influencing the recipient's knowledge-gain rate
	 */
	public static boolean switch_KLC = false;
	/**
	 * Control whether a low-performing agent will take a bonding/bridging preference that is
	 * (a) the majority preference of all its close contacts (value = true), or
	 * (b) the preference of its best-performing contacts (value = false) (if there are multiple "best" ones, randomly pick one)
	 */
	public static boolean switch_MOB = true;
	/**
	 * Control how individual agents decide knowledge sources. They can (a) randomly select (value = -1),
	 * (b) always use bonding social capital (value = 0), (c) always use bridging social capital (value = 1),
	 * (d) make decisions based on their XCSs (value = 2), or (e) choose from the expert index (value = -2)
	 * When comparing the results of different source selection methods, note that the length of the source list varies.
	 */
	public static int switch_DKS = 2;
	/**
	 * Control whether the initial state is identical. Value = true for identical initial state.
	 * The initial network structure is either generated using Repast generators (when value = false)
	 * or read from the same Pajek file (value = true). So are the initial task and expertise set-up.
	 */
	public static boolean switch_IIS = false;
	/**
	 * Control whether the recipient's judgment on the source's network position has some estimation error (value = true). 
	 */
	public static boolean switch_EST = true;
	//***************************************************
	//Model parameters (manipulative); each of these parameters should have a correspondent in parameters.xml file
	/**
	 * Indicate how much a tie will be strengthened if the payoff of transfer learning is positive
	 */
	public static double tieIncrease = 1.;
	/**
	 * Indicate how many times tie strength is reduced (due to 0 payoff) than when it is increased (a tie gets increased by 1 each time when payoff > 0)
	 * If negOverPos = 2, it means whenever payoff =0, tie strength is reduced by 2*Constants.tieIncrease  
	 */
	public static int negOverPos = 1;
	/**
	 * indicate the minimal final strength of a decayed tie
	 */
	public static double tieDecayMin = 0.1;
	/**
	 * This parameter predefines the probability of error when the recipient estimates a potential source's network position and their relationship.
	 */
	public static double estErrProb = 0.01;
	/**
	 * The predefined density of initial random networks
	 */
	public static double randomNetworkDensity = 0.8;
	/**
	 * The predefined average degree of initial small-world networks
	 * Must be an even number.
	 */
	public static int SWAvgDeg = 2;
	/**
	 * The predefined rewiring probability of initial small-world networks
	 * Also need to change the value in parameters.xml
	 */
	public static double SWRewirePr = 0.2;
	/**
	 * The threshold for judging common-contact relationship
	 */
	public static double commonContactThresh = 0.67;
	/**
	 * The utility threshold for adapting social capital preference
	 */	
	public static double adaptUtilThresh;
	/**
	 * The number-of-problem threshold for adapting social capital preference
	 */
	public static int adaptTimeThresh;
	/**
	 * A predefined value of every agent's knowledge-gain rate in self-learning,
	 * or the average value if the agents' knowledge-gain rates are not identify but follow a distribution.
	 * Also need to change the value in parameters.xml!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 */
	public static double selfGain;
	/**
	 * How many more times the rate of learning will be in knowledge transfer than in self-acquisition
	 */
	public static int KTOverSL = 100;
	/**
	 * The proportion of the maximally possible degree (netSize - 1) that the average
	 * degree must exceed (with other premises) to trigger weak-tie elimination.
	 * @see Environment#modifyTie(XClassifier, String)
	 */
	public static double denseNetThresh = 0.5;
	/**
	 * the ratio of knowledge decay rate vs. knowledge acquisition rate
	 */
	public static double forgetLearnRatio = 0.1;
	/**
	 * The number of experts recorded in the expert index for each area
	 */
	public static int numOfExperts = 3;
	/**
	 * The number of sources to send request messages to when the focal agent
	 * selects potential sources randomly
	 */
	public static int numOfRandSrc = 5; 
	/**
	 * The rate of environmental change, in terms of the percentage of prior-round tasks that the agent won't be assigned at the current round.
	 * In other words, here the "environment" means the local environment of individual agents. 
	 */
	public static double envChangeRate;
	/**
	 * The maximal number of micro-classifiers in the population.
	 */
	public static int maxPopSize = 50;
	/**
	 * The fall of rate in the fitness evaluation.
	 */
	public static double alpha = 0.1;

	/**
	 * The learning rate for updating fitness, prediction, prediction error, and
	 * action set size estimate in XCS's classifiers.
	 */
	public static double beta = 0.001;
	/**
	 * The fraction of the mean fitness of the population below which the
	 * fitness of a classifier may be considered in its vote for deletion.
	 */
	public static double delta = 0.1;

	/**
	 * The exponent in the power function for the fitness evaluation.
	 */
	public static double nu = 5.;

	/**
	 * The threshold for the GA application in an action set.
	 */
	public static double theta_GA = 3;

	/**
	 * The error threshold under which the accuracy of a classifier is set to one.
	 */
	public static double epsilon_0 = 10;

	/**
	 * The threshold over which the fitness of a classifier may be considered in its deletion probability.
	 */
	public static int theta_del = 5;

	/**
	 * The probability of applying crossover in an offspring classifier.
	 */
	public static double pX = 0.8;

	/**
	 * The probability of mutating one allele and the action in an offspring classifier.
	 */
	public static double pM = 0.05;

	/**
	 * The probability of using a don't care symbol (#) in an allele when covering.
	 */
	public static double P_dontcare = 0.5;

	/**
	 * The reduction of the prediction error when generating an offspring classifier.
	 */
	public static double predictionErrorReduction = 0.25;

	/**
	 * The reduction of the fitness when generating an offspring classifier.
	 */
	public static double fitnessReduction = 0.1;

	/**
	 * The experience of a classifier required to be a subsumer.
	 */
	public static int theta_sub = 5;

	/**
	 * Specifies if GA subsumption should be executed.
	 */
	public static boolean doGASubsumption = true;

	/**
	 * Specifies if action set subsumption should be executed.
	 */
	public static boolean doActionSetSubsumption = true;

	/**
	 * The initial prediction value when generating a new classifier (e.g in covering).
	 */
	public static double predictionIni = 10.0;

	/**
	 * The initial prediction error value when generating a new classifier (e.g in covering).
	 */
	public static double predictionErrorIni = 0.0;

	/**
	 * The initial fitness value when generating a new classifier (e.g in covering).
	 */
	public static double fitnessIni = 0.01;
	
	//***************************************************
	//Model parameters (non-manipulative)
	//TODO: this list has not been finished
	/**
	 * The don't care symbol (normally '#')
	 */
	public static char dontCare = '#';
	/**
	 * Once the knowledge level is smaller than this value, I treat it as 0
	 */
	public static double smallValue = 0.0001;
	/**
	 * The initialization of the pseudo random generator. Must be at lest one and smaller than _M (see below).
	 */
	protected static long seed = 1;

	/**
	 * Constant for the random number generator (modulus of PMMLCG = 2^31 -1).
	 */
	protected static long _M = 2147483647;

	/**
	 * Constant for the random number generator (default = 16807).
	 */
	protected static long _A = 16807;
	
	/**
	 * Constant for the random number generator (=_M/_A).
	 */
	protected static long _Q = _M / _A;
	
	/**
	 * Constant for the random number generator (=_M mod _A).
	 */
	protected static long _R = _M % _A;

	/**
	 * Return a random number between 0 and 1.
	 */
	public static double drand() {
		long hi = seed / _Q;
		long lo = seed % _Q;
		long test = _A * lo - _R * hi;

		if (test > 0)
			seed = test;
		else
			seed = test + _M;

		return (double) seed / _M;
	}

	/**
	 * Set a random seed in order to randomize the pseudo random generator.
	 */
	public static void setSeed(long s) {
		seed = s;
	}

	/**
	 * The default constructor.
	 */
	public Constants()
	{
		Parameters params = RunEnvironment.getInstance().getParameters();
		envChangeRate = (Double) params.getValue("envChangeRate");
		selfGain = (Double)params.getValue("selfGainRate");
		adaptUtilThresh = (Double)params.getValue("adaptUtilThresh");
		adaptTimeThresh = (Integer)params.getValue("adaptTimeThresh");
		SWRewirePr = (Double)params.getValue("networkRewiringProb");
	}
}