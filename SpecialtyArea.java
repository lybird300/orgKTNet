package orgKTNet;


/**
 * An instance of this class handles knowledge accumulation and decay
 * related to a specific specialty area of the host agent
 * @author Yuan Lin
 *
 */
public class SpecialtyArea implements Comparable<SpecialtyArea> {
	/**
	 * the host agent's current knowledge level in this area
	 */
	public double currentLevel;
	/**
	 * the knowledge level when the in-progress learning first starts
	 */
	public double initial_learn;
	/**
	 * the knowledge level when the in-progress forgetting first starts
	 */
	public double initial_forget;
	/**
	 * The meaning of different values of state:
	 * 		2: the knowledge is task-required, but did not increase at this time step
	 * 		1: the knowledge is task-required and its level increases because of self-learning or obtain knowledge from others
	 * 		0: the knowledge is not task-required, its level remains unchanged at this time step because the agent transfers it to others
	 * 		-1: the knowledge is not task-required, its level decreases because it is not used at this time step
	 */
	public int state;
	/**
	 * The accumulated number of learning periods as the host agent participates in knowledge transfer (as source or recipient) or self-learning.
	 */
	protected int learnDuration;
	/**
	 * The accumulated number of forgetting periods as the agent does not participates in any knowledge transfer (as source or recipient) or self-learning.
	 */
	protected int forgetDuration;
	public SpecialtyArea(double initialLevel, int state){
		currentLevel = initialLevel;
		initial_learn = 0.0;
		initial_forget = 0.0;
		//every time when a new area is established, its initial state is "unused" (-1)
		this.state = state;
		learnDuration = 0;
		forgetDuration = 0;
	}
	/**
	 * Return the state of this specialty area
	 * @return
	 * 		2: the knowledge is task-required, but did not increase at this time step
	 * 		1: the knowledge is task-required and its level increases because of self-learning or obtain knowledge from others
	 * 		0: the knowledge is not task-required, its level remains unchanged at this time step because the agent transfers it to others
	 * 		-1: the knowledge is not task-required, its level decreases because it is not used at this time step
	 */
	public int getState(){
		return state;
	}
	/**
	 * The host agent alternates among the state of knowledge accumulation, maintenance, and decay
	 * @param newState
	 */
	public void setState(int newState){
		if(this.state != newState){
			this.state = newState;
			if(this.state == 1)
				this.setInitialLearn();
			if(this.state == -1)
				this.setInitialForget();
		}
	}
	/**
	 * Calculate the current level of knowledge as a result of learning
	 * @param rateOfChange
	 * 		the rate of knowledge gain
	 * @return
	 * 		the updated knowledge level
	 */
	public double addKnow(int maxLevel, double rateOfChange){
		if(state == 1){
			learnDuration++;
			if (Constants.switch_KADF == true)
				currentLevel = maxLevel - (maxLevel-initial_learn) * Math.exp(-1*rateOfChange*learnDuration);
			else
				currentLevel = maxLevel - (maxLevel-initial_learn) * Math.pow(learnDuration, -1*rateOfChange);
		}
		if(maxLevel - currentLevel < Constants.smallValue) currentLevel = maxLevel;
		return currentLevel;
	}
	/**
	 * Calculate the current level of knowledge as a result of forgetting
	 * @param rateOfChange
	 * 		the rate of knowledge decay (same as the rate of self learning)
	 * @return
	 * 		the updated knowledge level
	 */
	public double decayKnow (double rateOfChange){
		if(state == -1){
			forgetDuration++;
			if (Constants.switch_KADF == true)
				currentLevel = initial_forget * Math.exp(-1*rateOfChange*forgetDuration);
			else
				currentLevel = initial_forget * Math.pow(forgetDuration, -1*rateOfChange);
		}
		if(currentLevel < Constants.smallValue) currentLevel = 0.0;
		return currentLevel;
	}
	/**
	 * Set up the initial knowledge level when a period of learning starts.
	 * The value is the current knowledge level as a result of a period of continuous forgetting.
	 */
	public void setInitialLearn(){
		initial_learn = currentLevel;
		learnDuration = 0;
	}
	/**
	 * Set up the initial knowledge level when a period of forgetting starts.
	 * The value is the current knowledge level as a result of a period of continuous learning.
	 */
	public void setInitialForget(){
		initial_forget = currentLevel;
		forgetDuration = 0;
	}
	
	@Override
	public int compareTo(SpecialtyArea otherArea) {
		return (currentLevel < otherArea.currentLevel ? -1 : (currentLevel == otherArea.currentLevel ? 0 : 1));
	}
}