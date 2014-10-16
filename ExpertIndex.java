package orgKTNet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import repast.simphony.context.Context;

/**
 * This class constructs and updates the organizational expert index, which is implemented as a map.
 * In every mapping, the key is a specialty area and the value is another map structure that stores agents and their expertise.
 * Look through all agents and index them based on their areas of expertise. This is done by the outer treemap structure named "expertIndex": key = a specific area of expert, value = an inner structure indicated by "areaIndex" 
 * The inner structure is a boundedSortedMap (see the java class in another file): key = levels of knowledge, value = id of a specific agent (instance of the orgMember class)
 * @see BoundedSortedMap
 * @author linly
 * @version OrgKTNet 1.0
 */
public class ExpertIndex extends TreeMap<Integer, BoundedSortedMap<Double, String>> {

	protected static final long serialVersionUID = 1L;

	public ExpertIndex() {
		super();
	}

	/**
	 * Add an agent to the expert index under each of its maximized specialty areas.
	 * The expert index remains the latest Top-5 experts for each area in terms of knowledge level.
	 * Note that an expert of an area may or may be achieve the maximal level of knowledge in that area.
	 * The put method of a Map data structure always replaces the old value of a key with a new value of the same key.
	 * @param expert
	 * 		the agent to be added
	 */
	public void addExpert(OrgMember expert) {
		BoundedSortedMap<Double, String> areaIndex = null;
		for (Map.Entry<Integer, SpecialtyArea> me : expert.myExpertise.entrySet()) {
			int area = me.getKey();
			double knowledgeLevel = me.getValue().currentLevel;
			areaIndex = this.get(area);
			if (areaIndex == null) {
				areaIndex = new BoundedSortedMap<Double, String>(Constants.numOfExperts);
				this.put(area, areaIndex);
			}
			areaIndex.put(knowledgeLevel, expert.getID());
		}
	}

	/**
	 * Initialize the expert index. This method is separated from the construction method,
	 * because it will be called at the completion of each problem. During the process of problem-solving,
	 * agents gain new knowledge while their unused knowledge decays at each step.
	 * @param context 
	 */
	public void initialize(Context<Object> context) {
		Iterable<Object> members = context.getObjects(OrgMember.class);
		//Iterable<OrgMember> members = RunState.getInstance().getMasterContext().getObjects(OrgMember.class);
		for (Object om : members)
			addExpert(((OrgMember) om));
	}

	/**
	 * This method searches the expert index for all experts in a specified area.
	 * @param area
	 * 		the specific area
	 * @return
	 * 		a list of the experts' IDs
	 */
	public ArrayList<String> searchExpert(int area) {
		BoundedSortedMap<Double, String> areaIndex = this.get(area);
		if(areaIndex == null) return null;
		ArrayList<String> sources = new ArrayList<String> ();
		for (Map.Entry<Double, String> entry : areaIndex.entrySet())
			sources.add(entry.getValue());
		return sources;
	}
}
