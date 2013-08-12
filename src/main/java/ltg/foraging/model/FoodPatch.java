package ltg.foraging.model;

import java.util.ArrayList;
import java.util.List;

public class FoodPatch {
	
	public String jid;
	public int feedRatio;
	public double killProb;
	public List<RFIDTag> kidsAtPatch = null;
	
	public FoodPatch(String jid, int feedRatio, double kb) {
		super();
		this.jid = jid;
		this.feedRatio = feedRatio;
		this.killProb = kb;
		this.kidsAtPatch = new ArrayList<RFIDTag>();
	}

}
