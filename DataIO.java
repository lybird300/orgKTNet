package orgKTNet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.graph.RepastEdge;

public class DataIO {
	/**
	 * Output the generated organizational social network into a Pajek (.net) file
	 * Naming: net_P(id of current problem)_T(current step).net
	 * Notes: (a) Pajek uses "\r" instead of "\n" as the line breaker.
	 * (b) If the network is empty (with no ties), the corresponding Pajek file only has a "Vertices" section, no "Edges" section.
	 * (c) If the network only has one edge, output "Edge" instead of "Edges" to the .net files.
   	 * (d) If the network is non-directed, use "Edges"; otherwise, use "Arcs" instead. 
	 * @param currentProblem
	 * 		the ID of the problem under processing (start from 1)
	 * @param currentTick
	 *		current step (tick)
	 * @param orgSize
	 * 		the number of agents, i.e., nodes; that is, the size of the organizational network
	 * @throws IOException
	 */
	public static void outputPajek(String fileName, int orgSize, ContextJungNetwork<OrgMember> orgSocialNetwork){
		try{
			FileOutputStream output = new FileOutputStream(fileName, false);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
			writer.write("*Vertices " + orgSocialNetwork.size());
			writer.newLine();
			for (int i = 1; i <= orgSize; i++) {
				writer.write(i + " \"" + String.valueOf(i) + "\"");
				writer.newLine();
			}
			if (orgSocialNetwork.numEdges() == 0)
				System.out.println(fileName + " is an empty network.");
			else if (orgSocialNetwork.numEdges() == 1) {
				System.out.println(fileName + " only has one edge.");
				writer.write("*Edge");
			} else	
				writer.write("*Edges");
			writer.newLine();
			for (RepastEdge<OrgMember> e : orgSocialNetwork.getEdges()) {
				String n1_id = ((OrgMember) e.getSource()).getID();
				String n2_id = ((OrgMember) e.getTarget()).getID();
				writer.write(n1_id + " " + n2_id + " " + e.getWeight());
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
     * Create an undirected network out of a Pajek .net file.
     * Below is an example format for an undirected graph with edge weights and edges specified in non-list form: <br>
     * <pre>
     * *vertices <# of vertices> 
     * 1 "a" 
     * 2 "b" 
     * 3 "c" 
     * *edges 
     * 1 2 0.1 
     * 1 3 0.9 
     * 2 3 1.0 
     * </pre>
     * @param fileName
     * 		the Pajek file
	 * @param net
	 * 		the network to be specified
	 */
	public static void inputPajek(String fileName, ContextJungNetwork<OrgMember> net){
		try {
			FileInputStream input = new FileInputStream(fileName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			String currentLine = null;
			StringTokenizer tokenizer = null;
			currentLine = reader.readLine();
            tokenizer = new StringTokenizer(currentLine);
            if (!tokenizer.nextToken().toLowerCase().startsWith("*vertices"))
            	System.out.println("Pajek file parse error: '*vertices' not first token");
            int numVertices = Integer.parseInt(tokenizer.nextToken());
            if(numVertices != net.size())
            	System.out.println("Pajek file parse error: the number of vertices does not match");
            
            //scan the .net file until reach the line of "*edge(s)"
            do{
            	currentLine = reader.readLine().trim();
            } while (!currentLine.startsWith("*"));
            
			int currentStartId = -1;
            int currentEndId = -1;
            //the starting line in the following "while" statement is the line right below "edges"
            //since the previous "while" statement ends with reading the line with "edges" 
            while ((currentLine = reader.readLine()) != null){
                currentLine = currentLine.trim();
                if (currentLine.length() == 0) {
                    break;
                }     
                if (currentLine.startsWith("*" ))
                    continue;
                tokenizer = new StringTokenizer(currentLine);
                currentStartId = Integer.parseInt(tokenizer.nextToken());              
                currentEndId = Integer.parseInt(tokenizer.nextToken());
                double weight = Double.parseDouble(tokenizer.nextToken());
                if (currentStartId == currentEndId) {
                    System.out.println("Same source and target nodes");
                    break;
                }
                OrgMember start = null;
                OrgMember end = null;
                for(OrgMember om : net.getNodes()){
                	int id = Integer.parseInt(om.getID());
                	if(id == currentStartId) start = om;
                	if(id == currentEndId) end = om;
                	if(start!=null && end!=null) break;
                }
                if(!net.isAdjacent(start, end))
                	net.addEdge(start, end, weight);
                else
                	net.getEdge(start, end).setWeight(weight);
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Input predefined task assignments (a problem with certain ID) from an existing csv file.
	 * The file is created earlier via outputTaskAssign. Using the same task assignments
	 * is important for experiments 
	 * @param fileName
	 * @param problemID
	 * @param memberList 
	 */
	public static void inputTaskAssign(String fileName, String problemID, Context<Object> context){
		try {
			FileInputStream input = new FileInputStream(fileName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			String currentLine = null;
            
            //scan the .csv file until reach the first record with the specified problem ID
            do{
            	currentLine = reader.readLine().trim();
            } while (!currentLine.startsWith(problemID));
            
            HashMap<String, int[]> temp = new HashMap<String, int[]>();
            while (currentLine != null && currentLine.startsWith(problemID)){
            	String[] assemble = currentLine.split(",");
            	int[] tasks = new int[assemble.length - 2];
            	//assemble[0] is problem#, assemble[1] is member#, and the rest are task-required areas
            	for(int i = 0; i < tasks.length; i++)
            		tasks[i] = Integer.parseInt(assemble[i+2]);
            	if(temp.containsKey(assemble[1])) System.out.println("OM already exists. DataIOL168\n");
            	temp.put(assemble[1], tasks);
            	currentLine = reader.readLine();
            	if(currentLine!=null) currentLine = currentLine.trim(); 
            }

    		//Context<Object> context = RunState.getInstance().getMasterContext();
    		Iterator<Object> members = context.getObjects(OrgMember.class).iterator();
    		while(members.hasNext()){
    			OrgMember om = (OrgMember)members.next();
    			om.getTaskEnvironment().currentTask = temp.get(om.getID());
    			om.getTaskEnvironment().reset();
    			Parameters params = RunEnvironment.getInstance().getParameters();
    			int taskRange = (Integer) params.getValue("taskRange");
    			int conditionID = (Integer) params.getValue("conditionID");
    			outputTaskAssign("output/task" + Integer.toString(conditionID) + ".csv", taskRange, om, problemID);
    		}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Input the expertise of each organizational member at a specific tick from an existing .csv file.
	 * The file is created earlier via outputExpAssign. 
	 * Using identical initial condition is important for experiments.
	 * @param fileName
	 * @param tick
	 * @param memberArea
	 * 		the number of specialty areas each organizational member can have
	 * @param memberList 
	 */
	public static void inputExpertise(String fileName, String tick, int memberArea, Context<Object> context){
		try {
			FileInputStream input = new FileInputStream(fileName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			String currentLine = null;
            
            //scan the .csv file until reach the first record with the specified problem ID
            do{
            	currentLine = reader.readLine();
            } while (!currentLine.startsWith(tick));
            
            HashMap<String, BoundedSortedMap<Integer, SpecialtyArea>> temp = new HashMap<String, BoundedSortedMap<Integer, SpecialtyArea>>();
            while (currentLine != null && currentLine.startsWith(tick)){
            	String[] assemble = currentLine.split(",");
            	BoundedSortedMap<Integer, SpecialtyArea> areas = new BoundedSortedMap<Integer, SpecialtyArea>(memberArea);
            	//assemble[0] is tick#, assemble[1] is member#, and the rest are the member's specialty areas
            	for(int i = 0; i < assemble.length - 2; i++)
            		if(!assemble[i+2].equals("0"))
            			areas.put(i, new SpecialtyArea(Double.parseDouble(assemble[i+2]), -1));
            	if(temp.containsKey(assemble[1])) System.out.println("OM already exists. DataIOL168\n");
            	temp.put(assemble[1], areas);
            	currentLine = reader.readLine(); 
            }

    		Iterator<Object> members = context.getObjects(OrgMember.class).iterator();
    		while(members.hasNext()){
    			OrgMember om = (OrgMember)members.next();
    			om.myExpertise = temp.get(om.getID());
    			om.getTaskEnvironment().reset();
    		}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void outputTimeSeriesData(String conditionID, int currentTick, double scPerc, double ndPerc, double rbPerc, ContextJungNetwork<OrgMember> orgSocialNetwork, double percOfFinish){
		try{
			File tResult = new File("output/TimeSeries" + conditionID + ".csv");
			boolean newFile = tResult.createNewFile();
			FileOutputStream file = new FileOutputStream(tResult, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	
			if(newFile){//if the file does not exist
				writer.append("Tick");
			    writer.append(',');
			    //the percent of agents who prefer bonding structures
			    writer.append("BondPerc");
			    writer.append(',');
			    //the percent of agents who allow for more degree
			    writer.append("MoreDegPerc");
			    writer.append(',');
			    //the percent of agents who prefer to respond even if they have not acquired the knowledge yet
			    writer.append("PreRespPerc");
			    writer.append(',');
			    //the average tie strength of the current social network
			    writer.append("AvgTS");
			    writer.append(',');
			    //the average tie strength of the current social network
			    writer.append("NumOfTies");
			    writer.append(',');
			    //the percent of agents who have finished their tasks
			    writer.append("FiniPerc");
			    writer.newLine();
			}
			//int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			writer.append(Integer.toString(currentTick));
			writer.append(',');
			writer.append(Double.toString(scPerc));
			writer.append(',');
			writer.append(Double.toString(ndPerc));
			writer.append(',');
			writer.append(Double.toString(rbPerc));
			writer.append(',');
			writer.append(String.valueOf(NetworkAnalysis.getAvgTieStrength(orgSocialNetwork)));
			writer.append(',');
			writer.append(String.valueOf(orgSocialNetwork.numEdges()));
			writer.append(',');
			writer.append(String.valueOf(percOfFinish));
			writer.newLine();
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Output the data of individual performance into a .csv file
	 * The AvgLearning is the sum of initial knowledge gap divided by the time cost.
	 * @param currentProblem
	 * 		the ID of the problem under processing
	 * @param context 
	 * @throws Exception
	 */
	public static void outputPerform(String conditionID, int currentProblem, int totalTime, double avgTime, double scRatio, ContextJungNetwork<OrgMember> orgSocialNetwork){
		try{
			File pResult = new File("output/performance" + conditionID + ".csv");
			boolean newFile = pResult.createNewFile();
			FileOutputStream file = new FileOutputStream(pResult, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	
			if(newFile){//if the file does not exist
				//create the head row of the .csv file
			    writer.append("Problem");
			    writer.append(',');
			    writer.append("TotalTime");
			    writer.append(',');
			    writer.append("AvgTime");
			    writer.append(',');
			    writer.append("SCR");
			    writer.append(',');
			    writer.append("ACC");
			    writer.append(',');
			    writer.append("BC");
			    writer.append(',');
			    writer.append("HTP");
			    writer.newLine();
			}
			writer.append(Integer.toString(currentProblem));
			writer.append(',');
			//int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			writer.append(Integer.toString(totalTime));
			writer.append(',');
			writer.append(Double.toString(avgTime));
			writer.append(',');
			writer.append(Double.toString(scRatio));
			writer.append(',');
			writer.append(String.valueOf(NetworkAnalysis.getHybridTriadProportion(orgSocialNetwork)));
			writer.append(',');
			writer.append(String.valueOf(NetworkAnalysis.getAvgClusterCoeff(orgSocialNetwork)));
			writer.append(',');
			writer.append(String.valueOf(NetworkAnalysis.getBetweenCentralization(orgSocialNetwork)));
			writer.newLine();
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Output the network structural data at each step into a .csv file
	 * @throws Exception
	 */
	public static void outputNetworkPerStep(int conditionID, ContextJungNetwork<OrgMember> orgSocialNetwork){
		try{
			File pResult = new File("output/networkPerStep" + conditionID + ".csv");
			boolean newFile = pResult.createNewFile();
			FileOutputStream file = new FileOutputStream(pResult, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	
			if(newFile){//if the file does not exist
				//create the head row of the .csv file
			    writer.append("Tick");
			    writer.append(',');
			    writer.append("ACC");
			    writer.append(',');
			    writer.append("BC");
			    writer.append(',');
			    writer.append("HTP");
			    writer.newLine();
			}
			int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			writer.append(Integer.toString(currentTick));
			writer.append(',');
			writer.append(String.valueOf(NetworkAnalysis.getHybridTriadProportion(orgSocialNetwork)));
			writer.append(',');
			writer.append(String.valueOf(NetworkAnalysis.getAvgClusterCoeff(orgSocialNetwork)));
			writer.append(',');
			writer.append(String.valueOf(NetworkAnalysis.getBetweenCentralization(orgSocialNetwork)));
			writer.newLine();
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Output organizational members' task assignments regarding a certain problem
	 * @param fileName
	 * @param taskRange
	 * 		specify the number of tasks for each memeber
	 * @param om
	 * 		the organizational member whose task-required areas are output
	 * @param currentProblem
	 */
	public static void outputTaskAssign(String fileName, int taskRange, OrgMember om, String currentProblem){
		try{
			File assignment = new File(fileName);
			boolean newFile = assignment.createNewFile();
			FileOutputStream file = new FileOutputStream(assignment, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	

	
			if(newFile){//if the file does not exist
				//create the head row of the .csv file
			    writer.append("Problem");
			    writer.append(',');
			    writer.append("Member");
			    for(int i = 0; i < taskRange; i++){
			    	writer.append(',');
			    	writer.append("Required Area" + String.valueOf(i+1));
			    }
			    writer.newLine();
			}
			
			writer.append(currentProblem);
			writer.append(',');
			writer.append(om.getID());
			for(int j = 0; j < taskRange; j++){
				writer.append(',');
				writer.append(String.valueOf(om.taskEnv.currentTask[j]));
			}
			writer.newLine();
			
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Output each organizational member's expertise at a specific click
	 * @param fileName
	 * @param orgArea
	 * 		the total number of expertise areas in the organization
	 * @param memberArea
	 * 		the number of expertise areas of each member
	 * @param list
	 * 		the list of organizational members
	 * @param currentTick
	 */
	public static void outputExpertise(String fileName, int orgArea, int memberArea, ArrayList<OrgMember> list, int currentTick){
		try{
			File assignment = new File(fileName);
			boolean newFile = assignment.createNewFile();
			FileOutputStream file = new FileOutputStream(assignment, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	

	
			if(newFile){//if the file does not exist
				//create the head row of the .csv file
			    writer.append("Tick#");
			    writer.append(',');
			    writer.append("Member#");
			    for(int i = 0; i < orgArea; i++){
			    	writer.append(',');
			    	writer.append("Area" + String.valueOf(i));
			    }
			    writer.newLine();
			}
			for(OrgMember om : list){
				writer.append(Integer.toString(currentTick));
				writer.append(',');
				writer.append(om.getID());
				for(int j = 0; j < orgArea; j++){
					writer.append(',');
					writer.append(String.valueOf(om.getExpertise(j)));
				}
				writer.newLine();
			}
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Output an individual agent's strategy chain while it solves the current task.
	 * Since it is called as soon as an agent finishes its task. The strategy chains from different agents are listed in the order of task completion.
	 * @param fileName
	 * @param strategyChain
	 * 		the type is Linkedlist 
	 */
	public static void outputStrategyChain(String fileName, int currentProblem, OrgMember om) {
		try{
			File assignment = new File(fileName);
			boolean newFile = assignment.createNewFile();
			FileOutputStream file = new FileOutputStream(assignment, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	

	
			if(newFile){//if the file does not exist
				//create the head row of the .csv file
			    writer.append("Problem#");
			    writer.append(',');
			    writer.append("Member#");
			    writer.append(',');
			    writer.append("Strategy Chain");
			    writer.newLine();
			}
			
			writer.append(Integer.toString(currentProblem));
			writer.append(',');
			writer.append(om.getID());
			Iterator<Integer> it = om.strategyChain.iterator();
			while(it.hasNext()){
				writer.append(',');
				writer.append(String.valueOf(it.next()));
			}
			writer.newLine();			
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
}



