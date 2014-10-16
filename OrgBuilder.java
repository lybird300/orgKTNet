package orgKTNet;

import java.util.ArrayList;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.graph.NetworkGenerator;
import repast.simphony.context.space.graph.RandomDensityGenerator;
import repast.simphony.context.space.graph.WattsBetaSmallWorldGenerator;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.util.SimUtilities;

/**
 * This class constructs the context that includes all agents, their social network, and all model parameters (manipulative or not).
 * It also schedules the actions of individual agents (i.e., the execution of their step function) and finally, outputs simulation data.
 * @author Yuan Lin
 * @version OrgKTNet 1.0
 */
public class OrgBuilder implements ContextBuilder<Object> {

	/**
	 *  The time each organizational problem takes (averaged by the number of active learners)
	 */
	protected double[] avgTimeCost = null;
	/**
	 *  The time each organizational problem takes (pure time)
	 */
	protected int[] totalTimeCost = null;
	/**
	 *  The organizational expert index
	 */ 
	protected ExpertIndex ei = null;

	/**
	 * This method coordinates agents by randomly executing their step functions and updating their information on the organizational social network (stored in some global variables).
	 * Using the annotation {@link ScheduledMethod} makes this method executed from the first simulation tick, and with specifying interval it is executed each tick afterwards.
	 * 
	 * When the organization solves the current problem, that is, all agents finish their current tasks (signal variable "totalLeft" = 0),
	 * the program moves on to the next problem (or end the simulation if no more problems to solve).
	 * Switching to a new problem requires some reconstruction: reassigning tasks, creating new messengers, resetting task environments, and updating the organizational expert index.
	 * However, this is the same group of agents. Their established social network remain unchanged. Their expertise is mostly kept except for a little decay of unused knowledge.
	 * 
	 * Upon solving a problem, the  agents will adapt their XCS. @see XCS#learnLessions
	 * 
	 * If an agent had a lower-than-average performance, its XCS adaptation starts with replacing some low-fit classifiers in its action set.
	 * Basically, the agent will replace those classifiers with some high-fit classifiers in the action sets of its close social contacts
	 * @see XClassifierSet#updateSet. After updating its action set, the agent then conducts regular XCS adaptation.
	 * 
	 * This method also output data for later analysis. It outputs network data in every step and performance and XCS data when a problem is solved.
	 * To ensure the correct value of "currentProblem," data outputting comes before preparing for the next problem.
	 */
	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public void activateAgents() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int currentProblem = (Integer) params.getValue("currentProblem");
		int conditionID = (Integer) params.getValue("conditionID");
		int numOfProblems = (Integer) params.getValue("numOfProblems");
		int totalLeft = (Integer) params.getValue("totalLeft");
		System.out.println("totalLeft =" + totalLeft + '\n');
		int orgSize = (Integer) params.getValue("orgSize");
		int orgArea = (Integer) params.getValue("orgArea"); 
		int memberArea = (Integer) params.getValue("memberArea");
		int prefBond = 0;
		int prefMoreConn = 0;
		int prefPreRespond = 0;
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> orgSocialNetwork = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		Iterable<Object> members = context.getObjects(OrgMember.class);
		ArrayList<OrgMember> memberList = new ArrayList<OrgMember>();
		for (Object member : members){
			memberList.add((OrgMember)member);
			if(((OrgMember)member).getSCPreference() == 0) prefBond++;
			if(((OrgMember)member).getNDPreference() == 0) prefMoreConn++;
			if(((OrgMember)member).getRBPreference() == 0) prefPreRespond++;
		}
		
		//social network and tie history are updated at each step
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		if(currentTick > 1)
			NetworkAnalysis.tieDecayRemove(orgSocialNetwork);
		
		try {
			String fileName = "output/net_P" + Integer.toString(currentProblem) + "_T" + Integer.toString(currentTick) + ".net";
			//DataIO.outputPajek(fileName, orgSize, orgSocialNetwork);
			//DataIO.outputExpertise("output/expertise" + Integer.toString(conditionID) + ".csv", orgArea, memberArea, memberList, currentTick);
			DataIO.outputTimeSeriesData(Integer.toString(conditionID), currentTick, (double)prefBond/orgSize, (double)prefMoreConn/orgSize, (double)prefPreRespond/orgSize, orgSocialNetwork, (double)(orgSize-totalLeft)/orgSize);
		} catch (Exception e) {
			System.out.println("File output error.");
			e.printStackTrace();
		}

		if (totalLeft == 0) {
			//calculate the average time cost of learning agents
			//excluding agents who are initially qualified in expertise
			int activeLearner = (Integer) RunEnvironment.getInstance().getParameters().getValue("activeLearner");
			//activeLearner ==0 means no knowledge transfer happens when the organization solves the current problem;
			//in other words, every agent is initially qualified for its task.
			totalTimeCost[currentProblem - 1] = currentTick - totalTimeCost[currentProblem - 1];
			if(activeLearner > 0){
				avgTimeCost[currentProblem - 1] = avgTimeCost[currentProblem - 1]/activeLearner;
				if(Constants.switch_DKS == 2){
					for (OrgMember member : memberList){
						Environment env = member.getTaskEnvironment();
						if (env.timeCost > avgTimeCost[currentProblem - 1]*Constants.adaptUtilThresh
							&& (env.timesOfBadPerform ++) > Constants.adaptTimeThresh){
							XCS myXCS = member.getXCS();
							if(myXCS.actionSet != null){//Sometimes an agent may finish learning at the first step by itself, in which case its action set is empty. 
								myXCS.actionSet.updateSet(env);
								env.timesOfBadPerform = 0;
								System.out.println("Member" + member.getID() + " has seeked for adaptation.");
							}
						}
					}
					//The next for statement should not be integrated into the previous one because it empties each agent's action set
					//concurrently, which the previous for statement intends to update.
					for (OrgMember member : memberList)
						if(member.getXCS().actionSet != null)
							member.getXCS().learnLessons();
				}
				try {
					//output the current problem#, the time spent on solving the problem, the preference ratio when members start to solve the problem (this
					//ratio remains the same during the problem-solving process), and the social network topology when the problem is solved.
					DataIO.outputPerform(Integer.toString(conditionID), currentProblem, totalTimeCost[currentProblem - 1], avgTimeCost[currentProblem - 1], (double)prefBond/(orgSize - prefBond), orgSocialNetwork);
				} catch (Exception e) {
					System.out.println("File output error.");
					e.printStackTrace();
				}
			}
			if (currentProblem == numOfProblems) {
				System.out.println("All problems have been solved.");
				RunEnvironment.getInstance().endRun();
			} else {
				params.setValue("currentProblem", currentProblem + 1);
				totalTimeCost[currentProblem] = currentTick; //set up the initial value
				params.setValue("totalLeft", orgSize);
				params.setValue("activeLearner", orgSize);
				int taskRange = (Integer) params.getValue("taskRange");
				for (OrgMember member : memberList) {
					member.idle = false;
					member.myMessenger.reset();
					if(!Constants.switch_IIS){
						member.assignTask(orgArea, taskRange, Constants.envChangeRate);
						//this is under the condition of "if totalLeft = 0", so technically should be output once per problem
						DataIO.outputTaskAssign("output/task" + Integer.toString(conditionID) + ".csv", taskRange, member, Integer.toString(currentProblem+1));
					}
				}
				//NetworkAnalysis.updateTieHistory();
				if(Constants.switch_DKS == -2) ei.initialize(context);
				if(Constants.switch_IIS)
					DataIO.inputTaskAssign("output/task1.csv", String.valueOf(currentProblem+1), context);
			}
		}
		
		SimUtilities.shuffle(memberList, RandomHelper.getUniform());
		params.setValue("avgDegreeCentrality", NetworkAnalysis.getAvgDC(orgSocialNetwork));
		params.setValue("avgTieStrength", NetworkAnalysis.getAvgTieStrength(orgSocialNetwork));
		params.setValue("networkDensity", NetworkAnalysis.getNetworkDensity(orgSocialNetwork));
		for (OrgMember member : memberList)
			member.step();
		//DataIO.outputNetworkPerStep(conditionID, orgSocialNetwork);
	}

	/**
	 * Construct the global context, set up global variables, and add agents to the context.
	 * initialize the tasks, expertise, and environments of individual agents. Build organizational expert index.
	 * Generate the initial topology of organizational social network.
	 * Repast S provides two network generators that create random and small-world networks separately.
	 */
	@Override
	public Context<Object> build(Context<Object> context) {
		context.setId("orgKTNet");
		if(Constants.switch_DKS == -2) ei = new ExpertIndex();
		ArrayList<OrgMember> memberList = new ArrayList<OrgMember>();
		//cs = new XClassifierSet();
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		/**
		 * regard the current model setting as one of the experimental conditions
		 */
		int conditionID = (Integer) params.getValue("conditionID");
		/**
		 * the number of organizational members; size of the social network
		 */
		int orgSize = (Integer) params.getValue("orgSize");
		/**
		 * the total number of different specialty areas
		 */
		int orgArea = (Integer) params.getValue("orgArea"); 
		/**
		 * the number of specialty areas each agent needs to fulfill a task. 
		*/
		int taskRange = (Integer) params.getValue("taskRange");
		/**
		 * the max level of knowledge (also the task-required level)
		 */
		int maxKnowLevel = (Integer) params.getValue("maxKnowLevel"); 
		/**
		 * The number of specialty areas each agent initially has.
		 * The value of this variable should be no smaller than taskRange
		 */
		int memberArea = (Integer) params.getValue("memberArea");
		
		new Constants();
		new DataIO();
		
		avgTimeCost = new double[(Integer) params.getValue("numOfProblems")];
		totalTimeCost = new int[(Integer) params.getValue("numOfProblems")];
		for (int j = 0; j<avgTimeCost.length; j++){
			avgTimeCost[j] = 0.;
			totalTimeCost[j] = 0;
		}
				
		for (int i = 0; i < orgSize; i++) {
			OrgMember om = new OrgMember(context, ei, i + 1, avgTimeCost);//the ID of members start from 1
			context.add(om);
			memberList.add(om);
		}
		if(Constants.switch_DKS == -2) ei.initialize(context);
		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>("socialNetwork", context, false);
		ContextJungNetwork<OrgMember> initialNet = null;
		if(!Constants.switch_IIS){
			if(orgSize >= 10){
				/**
				* WattsBetaSmallWorldGenerator is a graph generator that produces a small world network using the beta-model as proposed by Duncan Watts.
				* The basic ideas is to start with a one-dimensional ring lattice in which each vertex has k-neighbors
				* and then randomly rewire the edges, with probability beta, in such a way that a small world networks can be created
				* for certain values of beta and k that exhibit low characteristic path lengths and high clustering coefficient.
				*/
				NetworkGenerator<Object> smallWorldNetGenerator = 
						new WattsBetaSmallWorldGenerator<Object>(Constants.SWRewirePr, Constants.SWAvgDeg, true);
				netBuilder.setGenerator(smallWorldNetGenerator).buildNetwork();
			}
			else{
				NetworkGenerator<Object> randomNetGenerator = 
						new RandomDensityGenerator<Object>(Constants.randomNetworkDensity, false, true);
				netBuilder.setGenerator(randomNetGenerator).buildNetwork();
			}
			initialNet = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
			DataIO.outputPajek("output/net_initial.net", orgSize, initialNet);
			for(OrgMember member: memberList){
				member.initiateTask(orgArea, taskRange);
				member.initiateExpertise(orgArea, memberArea, maxKnowLevel);
				DataIO.outputTaskAssign("output/task" + Integer.toString(conditionID) + ".csv", taskRange, member, "1");
			}
			DataIO.outputExpertise("output/expertise" + Integer.toString(conditionID) + ".csv", orgArea, memberArea, memberList, 0);
		}
		else{
			netBuilder.buildNetwork();
			initialNet = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
			DataIO.inputPajek("output/net_initial.net", initialNet);
			DataIO.inputTaskAssign("output/task1.csv", "1", context);
			DataIO.inputExpertise("output/expertise1.csv", "0", memberArea, context);
			DataIO.outputExpertise("output/expertise" + Integer.toString(conditionID) + ".csv", orgArea, memberArea, memberList, 0);
		}
		
		new NetworkAnalysis(initialNet);
		
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters sParams = ScheduleParameters.createRepeating(1, 1, 1);
		schedule.schedule(sParams, this, "activateAgents");
				
		return context;
	}
}