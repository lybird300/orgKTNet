package orgKTNet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.metrics.*;
import edu.uci.ics.jung.graph.Graph;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.graph.RepastEdge;

/**
 * This class provides some methods for analyzing the organizational social network.
 * It extends and utilizes JUNG class/functions, whose .jar files has already been included in the library of "Repast Simphony Development" (see left menu).
 * You may think the organizational social network should be an attribute of this class. However, this is a static class whereas the network keeps evolving.
 * Thus, we'd better input the latest network as a parameter when invoking any function inside this class.
 * @author Yuan Lin
 */
public class NetworkAnalysis extends Metrics {
	
	/**
	 *  a semi-matrix recording the historically accumulated strength of every possible network tie
	 */ 
	public static double[][] tieHistory = null;
	/**
	 *  a semi-matrix recording the time of use for every possible network tie
	 */ 
	public static int[][] tieUsePerProblem = null;
	
	public NetworkAnalysis(ContextJungNetwork<OrgMember> initialNet){
		int netSize = initialNet.size();
		//build a population * (population + 1) matrix
		if(tieHistory==null){
			tieHistory = new double [netSize][netSize];
			int agentA, agentB, u, v;
			for(RepastEdge<OrgMember> tie: initialNet.getEdges()){
				agentA = Integer.parseInt(tie.getSource().getID());
				agentB = Integer.parseInt(tie.getTarget().getID());
				u = (agentA < agentB)? (agentA - 1):(agentB - 1);
				v = (agentA < agentB)? (agentB - 1):(agentA - 1);
				tieHistory[u][v] = tie.getWeight();
			}
		}
		if(tieUsePerProblem==null){
			tieUsePerProblem = new int [netSize][netSize];
			for (int u = 0; u < netSize; u++)
				for (int v = u; v< netSize; v++){
					tieUsePerProblem[u][v]= 0;
				}
		}
	}
	/**
	 * Deal with sources whose reply messages the recipient never receives
	 */
	/*public static void updateTieHistory(){
	    for (int i = 0; i < tieHistory.length; i++)
	    	for (int j = i; j<= tieHistory[0].length; j++)
	    		if(tieUsePerProblem[i][j] > 0){
	    			updateTieStrength(i, j, -1*Constants.negOverPos*tieUsePerProblem[i][j], false);
	    			tieUsePerProblem[i][j] = 0;
	    		}
	}*/
	/**
	 * Calculates the average node degree of the network
	 * @param numOfNodes
	 * 		the size of the network
	 */
	public static double getAvgDC(ContextJungNetwork<OrgMember> orgSocialNetwork) {
		int sumDegree = 0;
		int numOfNodes = orgSocialNetwork.size();
		for (OrgMember node : orgSocialNetwork.getNodes())
			sumDegree += orgSocialNetwork.getDegree(node);
		return (double) sumDegree / (double) numOfNodes;
	}

	/**
	 * Calculate the average tie strength of the network
	 */
	public static double getAvgTieStrength(ContextJungNetwork<OrgMember> orgSocialNetwork) {
		int sumStrength = 0;
		for (RepastEdge<OrgMember> tie : orgSocialNetwork.getEdges())
			sumStrength += tie.getWeight();
		int numOfTies = orgSocialNetwork.numEdges();
		double avgTS = 0.;
		if(numOfTies > 0){
			avgTS = (double) sumStrength / (double) numOfTies;
			if(avgTS < Constants.smallValue) return 0.0;
			else return avgTS;
		}
		else return -1;
		
	}

	/**
	 * Calculate the density of the entire network
	 * @param orgSize
	 * 		the size of the network
	 */
	public static double getNetworkDensity(ContextJungNetwork<OrgMember> orgSocialNetwork) {
		int netSize = orgSocialNetwork.size();
		int maxNumOfTies = netSize * (netSize - 1) / 2;
		return (double) orgSocialNetwork.numEdges() / (double) maxNumOfTies;
	}
	
	/**
	 * Calculate the mean value of network nodes' clustering coefficient
	 * @param orgSocialNetwork
	 * @see clusteringCoefficients
	 */
	public static double getAvgClusterCoeff(ContextJungNetwork<OrgMember> orgSocialNetwork){
		Graph<OrgMember, RepastEdge<OrgMember>> g = orgSocialNetwork.getGraph();
		Map<OrgMember, Double> cc = clusteringCoefficients(g);
		double avgCC = 0.;
		for (Map.Entry<OrgMember, Double> single_cc : cc.entrySet())
			avgCC += single_cc.getValue();
		avgCC = avgCC/orgSocialNetwork.size();
		return avgCC;
	}

	/**
	 * Calculate the network's betweenness centralization based on Equation 5.13
	 * in Wasserman, S., & Faust, K. (1994). Social network analysis methods and applications. p.191  
	 * The betweenness centrality of each node is calculated using JUNG
	 * <a href="http://jung.sourceforge.net/doc/api/edu/uci/ics/jung/algorithms/importance/BetweennessCentrality.html>BetweennessCentrality class</a>
	 * @param orgSocialNetwork
	 * @return
	 */
	public static double getBetweenCentralization(ContextJungNetwork<OrgMember> orgSocialNetwork){
		Graph<OrgMember, RepastEdge<OrgMember>> g = orgSocialNetwork.getGraph();
        BetweennessCentrality<OrgMember, RepastEdge<OrgMember>> ranker = 
                new BetweennessCentrality<OrgMember, RepastEdge<OrgMember>>(g);
        ranker.setRemoveRankScoresOnFinalize(false); 
        ranker.evaluate();
        double maxBC = 0.;
        double sumBC = 0.;
        for (OrgMember om: orgSocialNetwork.getNodes())
        	if (ranker.getVertexRankScore(om) > maxBC )
        		maxBC = ranker.getVertexRankScore(om);
        for (OrgMember om: orgSocialNetwork.getNodes())
        	sumBC += maxBC - ranker.getVertexRankScore(om);
        int netSize = orgSocialNetwork.size();
        return 2*sumBC/(Math.pow((netSize - 1), 2.)*(netSize - 2));
	}
	
	/**
	 * TriadicCensus is a standard social network tool that counts, for each of the different possible configurations of three vertices,
	 * the number of times that that configuration occurs in the given graph. This model uses a modified triadic census to distinguish hybrid triads
	 * from other triads and the underlying social network is assumed to be undirected.
	 * This function returns the proportion of hybrid triad types in all triad types. The modifiedTriadCounts array stores 10 different triad types,
	 * named as (number of pairs that are strongly connected)(number of pairs that are weekly connected)(number of disconnected pairs).
	 * <table>
	 * <tr><th>Index</th><th>Configuration</th><th>Notes</th></tr>
	 * <tr><td>1</td><td>003</td><td>The empty triad</td></tr>
	 * <tr><td>2</td><td>012</td><td></td></tr>
	 * <tr><td>3</td><td>102</td><td></td></tr>
	 * <tr><td>4</td><td>021</td><td></td></tr>
	 * <tr><td>5</td><td>111</td><td></td></tr>
	 * <tr><td>6</td><td>030</td><td>Three weak ties, a type of hybrid configuration</td></tr>
	 * <tr><td>7</td><td>201</td><td>Two strong ties, another type of hybrid configuration</td></tr>
	 * <tr><td>8</td><td>120</td><td></td></tr>
	 * <tr><td>9</td><td>210</td><td></td></tr>
	 * <tr><td>10</td><td>300</td><td></td></tr>
	 * </table>
	 * This function and related sub-functions are modified from the JUNG TriadicCensus class(edu.uci.ics.jung.algorithms.metrics.TriadicCensus),
	 * which is based on <a href="http://vlado.fmf.uni-lj.si/pub/networks/doc/triads/triads.pdf">
	 * A subquadratic triad census algorithm for large sparse networks with small maximum degree</a>
	 * Vladimir Batagelj and Andrej Mrvar, University of Ljubljana. Published in Social Networks.
	 * @param orgSocialNetwork
	 * @return the proportion of hybrid triads in all triads
	 */
	public static double getHybridTriadProportion(ContextJungNetwork<OrgMember> orgSocialNetwork){
		double tieStrengthThresh = getAvgTieStrength(orgSocialNetwork);
		int netSize = orgSocialNetwork.size();
        int[] modifiedTriadCounts = new int[11];
		Iterable<OrgMember> nodes = orgSocialNetwork.getNodes();
		ArrayList<OrgMember> id = new ArrayList<OrgMember>();
		for (OrgMember node : nodes)
			id.add(node);
		for (int i_v = 0; i_v < netSize; i_v++) {
			OrgMember v = id.get(i_v);
			Set<OrgMember> v_neighbors = new HashSet<OrgMember>();
			for(OrgMember v_neighbor: orgSocialNetwork.getAdjacent(v))
				v_neighbors.add(v_neighbor);
			for(OrgMember u : v_neighbors) {
				int triType = -1;
				if (id.indexOf(u) <= i_v)
					continue;
				Set<OrgMember> u_neighbors = new HashSet<OrgMember>();
				for(OrgMember u_neighbor: orgSocialNetwork.getAdjacent(u))
					u_neighbors.add(u_neighbor);
				u_neighbors.addAll(v_neighbors);
				u_neighbors.remove(u);
				u_neighbors.remove(v);
				//v and u are neighbors.
				if (orgSocialNetwork.getEdge(u, v).getWeight() > tieStrengthThresh)
					triType = 3;
				if (orgSocialNetwork.getEdge(u, v).getWeight() <= tieStrengthThresh)
					triType = 2;
				modifiedTriadCounts[triType] += netSize - u_neighbors.size() - 2;
				for (OrgMember w : u_neighbors) {
					if (shouldCount(orgSocialNetwork, id, u, v, w)) {
						modifiedTriadCounts[triType(triCode(orgSocialNetwork, u, v, w, tieStrengthThresh))] ++;
					}
				}
			}
		}
		int sum = 0;
		for (int i = 2; i <= 10; i++) {
			sum += modifiedTriadCounts[i];
		}
		//the total number of triads is netSize!/(3!*(netSize - 3)!)
		int totalTriads = netSize * (netSize-1) * (netSize-2) / 6;
		modifiedTriadCounts[1] = totalTriads - sum;		
		return (modifiedTriadCounts[6] + modifiedTriadCounts[7])/(double)totalTriads;
	}
	
	/**
	 * Evaluate the relationship between nodes a and b
	 * @param net
	 * @param a
	 * @param b
	 * @param strengthThresh
	 * @return
	 * 		2: if nodes a and b are disconnected
	 * 		1: if nodes a and b are weakly connected
	 * 		0: if nodes a and b are strongly connected
	 */
	protected static int link(ContextJungNetwork<OrgMember> net, OrgMember a, OrgMember b, double avgTieStrength) {
		if(!net.isAdjacent(a, b))
			return 2;
		else if(net.getEdge(a, b).getWeight() <= avgTieStrength)
			return 1;
		else return 0;
	}
	
	/**
	 * Decide whether a triad containing node w should be counted.
	 * u and v are neighbors; u and w are neighbors; v and w may or may not be neighbors.
	 */
	protected static boolean shouldCount(ContextJungNetwork<OrgMember> net, ArrayList<OrgMember> id, OrgMember u, OrgMember v, OrgMember w) {
		int i_u = id.indexOf(u);
		int i_w = id.indexOf(w);
		if (i_u < i_w)
			return true;
		int i_v = id.indexOf(v);
		if ((i_v < i_w) && (i_w < i_u) && (!net.isAdjacent(w, v)))
			return true;
		return false;
	}

	/**
	 * This function implements the core idea of the aforementioned paper:
	 * each non-empty, non-dyadic(dyadic tirads has one and only one tie) triad type (i.e., with at least 2 ties) is represented by a binary code.
	 * This function is called by another function getHybridTriadProportion(), when u and w are connected (so are u and v).
	 * Thus, the last bit of the binary code indicates whether u and v are weakly (1) or strongly (0) connected.
	 * The second last bit indicates whether u and w are weakly (1) or strongly (0) connected.
	 * The first two bits together indicate whether v and w are weakly (01) or strongly (00) connected or unconnected (10). Specifically,
	 * <table>
	 * <tr><th>binary form</th><th>triCode</th><th>triType</th><th>index in the counts array</th></tr>
	 * <tr><td>00|0|0</td><td>0</td><td>300</td><td>10</td></tr>
	 * <tr><td>00|0|1</td><td>1</td><td>210</td><td>9</td></tr>
	 * <tr><td>00|1|0</td><td>2</td><td>210</td><td>9</td></tr>
	 * <tr><td>00|1|1</td><td>3</td><td>120</td><td>8</td></tr>
	 * <tr><td>01|0|0</td><td>4</td><td>210</td><td>9</td></tr>
	 * <tr><td>01|0|1</td><td>5</td><td>120</td><td>8</td></tr>
	 * <tr><td>01|1|0</td><td>6</td><td>120</td><td>8</td></tr>
	 * <tr><td>01|1|1</td><td>7</td><td>030</td><td>6</td></tr>
	 * <tr><td>10|0|0</td><td>8</td><td>201</td><td>7</td></tr>
	 * <tr><td>10|0|1</td><td>9</td><td>111</td><td>5</td></tr>
	 * <tr><td>10|1|0</td><td>10</td><td>111</td><td>5</td></tr>
	 * <tr><td>10|1|1</td><td>11</td><td>021</td><td>4</td></tr>
	 * </table>
	 * @return
	 * 		the decimal value of the binary code, an integer from [0, 11] 
	 */
	public static int triCode(ContextJungNetwork<OrgMember> net, OrgMember u, OrgMember v, OrgMember w, double strengthThresh) {
		int i = 0;
		i += link(net, u, v, strengthThresh);
		i += link(net, u, w, strengthThresh)*2;
		i += link(net, v, w, strengthThresh)*4;
		return i;
	}
	
	/**
	 * Use the triCode to locate the appropriate triType
	 * @param triCode
	 * @return
	 * 		the index of a specific triType in the modifiedTriadCounts array
	 */
	public static int triType(int triCode) {
		int[] codeToType = {10, 9, 9, 8, 9, 8, 8, 6, 7, 5, 5, 4};
		return codeToType[triCode];
	}

	/**
	 * This function and related sub-functions are modified from the JUNG StructuralHoles class(edu.uci.ics.jung.algorithms.metrics.StructuralHoles),
	 * which calculates some of the measures from Burt's text "Structural Holes: The Social Structure of Competition".
	 * <p>The original codes are donated by Jasper Voskuilen and Diederik van Liere of the Department of Information and Decision Sciences at Erasmus University,
	 * and are converted to jung2 by Tom Nelson.</p>
	 * 
     * Burt's constraint measure (equation 2.4, page 55 of Burt, 1992). Essentially a
     * measure of the extent to which <code>i</code> is invested in people who are invested in
     * other of <code>i</code>'s alters (neighbors). The "constraint" is characterized
     * by a lack of primary holes around each neighbor. Formally:
     * <pre>
     * constraint(i) = sum_{j in MP(i), j != i} localConstraint(i,j)
     * </pre>
     * where MP(i) is the set of i's neighbors(in a non-directed network context).
     * @return
	 * 		the constraint measure
     * @see #localConstraint(Object, Object)
     */
    public static double constraint(ContextJungNetwork<OrgMember> net, OrgMember i) {
        double result = 0;
        for(OrgMember j : net.getAdjacent(i))
        	result += localConstraint(net, i, j);
        return result;
    }
    /**
     * Return the local constraint on <code>i</code> from a lack of primary holes around its neighbor <code>j</code>.
     * Based on Burt's equation 2.4. Formally:
     * <pre>
     * localConstraint(i, j) = (w(i,j) + (sum_{q in N(i)} w(i,q) * w(q, j)))^2
     * </pre>
     * where 
     * <ul>
     * <li/><code>N(i) is the set of i's neighbors(in a non-directed network context)</code>
     * <li/><code>w(i,q) =</code> normalized tie weight of i and q
     * </ul>
     * @see #normalizedTieWeight(Object, Object)
     */
    public static double localConstraint(ContextJungNetwork<OrgMember> net, OrgMember i, OrgMember j) 
    {	
        double nmtw_ij = normalizedTieWeight(net, i, j);
        double inner_result = 0;
        for (OrgMember q : net.getAdjacent(i))
            inner_result += normalizedTieWeight(net, i, q) * normalizedTieWeight(net, q, j);
        return (nmtw_ij + inner_result) * (nmtw_ij + inner_result);
    }
    /**
     * Return the proportion of <code>i</code>'s network time and energy invested
     * in the relationship with <code>j</code>. Formally:
     * <pre>
     * normalizedMutualEdgeWeight(a,b) = mutual_weight(a,b) / (sum_c mutual_weight(a,c))
     * </pre>
     * Returns 0 if either numerator or denominator = 0, or if <code>v1 == v2</code>.
     */
    protected static double normalizedTieWeight(ContextJungNetwork<OrgMember> net, OrgMember v1, OrgMember v2)
    {
        if (v1.getID() == v2.getID() || !net.isAdjacent(v1, v2))
            return 0;
        double numerator = net.getEdge(v1, v2).getWeight();
        if (numerator == 0)
            return 0;
        double denominator = 0;
        for (OrgMember g : net.getAdjacent(v1))
            denominator += net.getEdge(v1, g).getWeight();
        if (denominator == 0)
            return 0; 
        return numerator / denominator;
    }
	/**
	 * Update a specific element of the tie strength history matrix.
	 * Note: agent ID starts from "1"
	 * @param agentB
	 * @param agentA
	 * @param delta
	 * 		In case of tie strength reduction, delta is a negative value; otherwise, it is a positive value.
	 * @param decay
	 * 		If tie strength is updated to reflect the effect of tie decay (decay = true) or not (decay = false)
	 */
	/**
	 * @param agentA
	 * @param agentB
	 * @param delta
	 * @param decay
	 * @return
	 */
	public static double updateTieStrength(int agentA, int agentB, double delta, boolean decay) {
		//row is always <= col; the purpose is to use only semi-matrix
		if(agentA == agentB){
			System.out.println("NetworkAnalysis.javaL387: Wrong!\n");
			return 0.;
		}
		int row = (agentA < agentB)? (agentA - 1):(agentB - 1);
		int col = (agentA < agentB)? (agentB - 1):(agentA - 1);
		double oldRecord = tieHistory[row][col];
		double newRecord = oldRecord + delta;
		tieHistory[row][col] = newRecord;
		if(!decay) tieUsePerProblem[row][col]--;//because whenever a request was sent out, this value gets ++;
		else{
			if(oldRecord <= Constants.tieDecayMin) tieHistory[row][col] = oldRecord;
			else if(newRecord < Constants.tieDecayMin) tieHistory[row][col] = Constants.tieDecayMin;
		}
		return tieHistory[row][col];
	}
	/**
	 * Every time when a request message is sent out, the value of the matrix element adds 1, indicating that the corresponding tie is used once.
	 * It is possible that a tie is used multiple times while the agent solves a task.
	 * It is also possible that this tie is just created
	 * @param agentA
	 * @param agentB
	 */
	public static void updateTieUse(String agentA, String agentB) {
		if(agentA.equals(agentB)){
			System.out.println("NetworkAnalysis.javaL387: Wrong!\n");
			return;
		}
		int row = (Integer.parseInt(agentA) < Integer.parseInt(agentB))? (Integer.parseInt(agentA) - 1):(Integer.parseInt(agentB) - 1);
		int col = (Integer.parseInt(agentA) < Integer.parseInt(agentB))? (Integer.parseInt(agentB) - 1):(Integer.parseInt(agentA) - 1);
		tieUsePerProblem[row][col]++;
	}
	
	/**
	 * All network ties (social relations) will be subject to strength decay.
	 * The tie decay rate is independent of any interactions but inversely proportional to tie strength
	 * Ties whose strength has fallen below the minimal value will be deleted
	 */
	public static void tieDecayRemove(ContextJungNetwork<OrgMember> net){
		ArrayList<RepastEdge<OrgMember>> tieCollection = new ArrayList<RepastEdge<OrgMember>>();
		double oldWeight = 0.;
		double newWeight = 0.;
		for(RepastEdge<OrgMember> currentTie : net.getEdges()){
			oldWeight = currentTie.getWeight();
			//System.out.println("OrgBuilder.java L285. OldWeight = " + oldWeight + "\n");
			if(oldWeight <= Constants.tieDecayMin)
				tieCollection.add(currentTie);
			else{
				newWeight = oldWeight*(1-Constants.tieDecayMin/oldWeight);
				//newWeight = oldWeight*(1-1/Math.log1p(Math.exp(1.0) + oldWeight));//must ensure that newWeight is between 0 and oldWeight, because the minimal strength for an active tie is Constants.tieDecayMin = 0.1
				//System.out.println("OrgBuilder.java L286. NewWeight = " + newWeight + "\n");
				updateTieStrength(Integer.parseInt(currentTie.getSource().getID()),
					Integer.parseInt(currentTie.getTarget().getID()), newWeight - oldWeight, true);	
				if(newWeight <= Constants.tieDecayMin)
					tieCollection.add(currentTie);
				else currentTie.setWeight(newWeight);
			}
		}
		for(RepastEdge<OrgMember> tie: tieCollection)
			net.removeEdge(tie);
	}
}
