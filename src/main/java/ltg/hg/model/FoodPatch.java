package ltg.hg.model;

import java.util.ArrayList;
import java.util.List;

public class FoodPatch {
	
	// Assigned attributes
	private final double qulity_per_second;
	private final double risk;
	// unused so far
	protected final String id;
	protected final String quality_label;
	protected final String risk_label;
	
	// Instantaneous attributes
	private List<String> tags_id_currently_at_patch = null;
	
	
	public FoodPatch(String id, String richness_label, int richness, String risk_label, double risk) {
		this.id = id;
		this.quality_label = richness_label;
		this.qulity_per_second = (double) richness;
		this.risk_label = risk_label;
		this.risk = risk;
		this.tags_id_currently_at_patch = new ArrayList<String>();
	}


	public synchronized void addTagToPatch(String tag) {
		tags_id_currently_at_patch.add(tag);
	}
	
	
	public synchronized void removeTagFromPatch(String tag) {
		tags_id_currently_at_patch.remove(tag);
	}
	
	
	public synchronized double getQuality() {
		return qulity_per_second;
	}


	public synchronized double getCurrentCompetition() {
		return ((double) tags_id_currently_at_patch.size() );
	}
	
	public synchronized double getCurrentYield() {
		return qulity_per_second / ((double) tags_id_currently_at_patch.size() ); 
	}
	
	
	public synchronized double getRisk() {
		return risk;
	}

}
