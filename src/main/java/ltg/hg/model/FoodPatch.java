package ltg.hg.model;

import java.util.ArrayList;
import java.util.List;

public class FoodPatch {
	
	// Assigned attributes
	public final String id;
	public final String richness_label;
	public final int richness_per_second;
	public final String risk_label;
	public final double risk;
	
	// Calculated attributes
	private List<String> tags_id_currently_at_patch = null;
	
	
	public FoodPatch(String id, String richness_label, int richness, String risk_label, double risk) {
		this.id = id;
		this.richness_label = richness_label;
		this.richness_per_second = richness;
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

}
