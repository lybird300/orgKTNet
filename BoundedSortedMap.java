package orgKTNet;

import java.util.TreeMap;

/**
 * This class constructs a data structure used to store the value of each expert-index entry.
 * It is a TreeMap implementation that grows to a fixed size and then retains only a fixed number of 

the highest (largest) keys.
 * @author David Jurgens, Yuan Lin
 */
public class BoundedSortedMap<K, V> extends TreeMap<K, V> {

	private static final long serialVersionUID = 1L;
	/**
	 * The maximum number of mappings to retain.
	 */
	protected final int bound;

	/**
	 * Construct an instance that will only retain the specified number of the largest (highest) 

keys.
	 * In this model, the bound is predefined.
	 * @param bound
	 *            the number of mappings to retain
	 * @see Constants#numOfExperts
	 */
	public BoundedSortedMap(int bound){
		super();
		this.bound = bound;
	}

	/**
	 * The constructor is used when the map is part of an expert index: the key-value pair 

represents knowledgeLevel-agentID
	 * Add the key-value mapping to this map, and if the total number of mappings exceeds the 

bounds, remove the currently lowest element.
	 * @param key
	 * @param value
	 */
	public V put(K key, V value) {
		//"old" is the old value of the same key, which is replaced by the new value, i.e., the parameter
		V old = super.put(key, value);
		if (size() > bound)
			remove(firstKey());
		return old;
	}
	
	/**
	 * This constructor is used when the items of this map represents an individual agent's expertise:
	 * the key-value pair represents areaID-instance of specialty area.
	 * Add the key-value mapping to this map, and if the total number of mappings exceeds the bounds, remove the currently lowest element.
	 * @param key
	 * @param value
	 */
	public void put(K key, V value, boolean isExpert){
		if(isExpert) put(key, value);
		else{
			//first remove the lowest-value entry then add the new entry, since the new one may also have the lowest level -- 0
			//but the one removed should not be a specialty area needed for the current task 
			if (size()+ 1 > bound){
				K min_key = null;
				V min_value = null;
				Double min_level = Double.POSITIVE_INFINITY;
				for(java.util.Map.Entry<K, V> e: entrySet()){
					int state = ((SpecialtyArea) e.getValue()).getState();
					if(state == 2 || state == 1) continue;
				    if(min_level.compareTo(((SpecialtyArea)e.getValue()).currentLevel)>0){
				        min_key = e.getKey();
				        min_value = e.getValue();
				        min_level = ((SpecialtyArea)e.getValue()).currentLevel;
				    }
				}
				if(min_key != null)
					remove(min_key);
				else
					System.out.println("Why the key is null? BoudedSortedMap L84");
			}
			super.put(key, value);
		}
	}
	
}