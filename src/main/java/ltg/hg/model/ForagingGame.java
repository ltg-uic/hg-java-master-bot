package ltg.hg.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForagingGame {
	
	private String filename = null;
	private Map<String, RFIDTag> tags = null;
	private Map<String, FoodPatch> patches = null;
	public int penaltySec = -1;
	
	public ForagingGame() {
	}
	
	
	public ForagingGame(String file) {
		this.filename = file; 
	}
	
	
	public FoodPatch getPatch(String patchId) {
		return patches.get(patchId);
	}
	
	
	public Collection<FoodPatch> getAllPatches() {
		return patches.values();
	}
	
	
	public Collection<RFIDTag> getAllTags() {
		return tags.values();
	}
	
	
	public void addPoints(String tagId, int points) {
		tags.get(tagId).score += points;
		System.out.println("Added " + points + " calories to " + tagId);
	}
	
	
	public synchronized List<RFIDTag> selectVictims() {
		List<RFIDTag> victims = new ArrayList<RFIDTag>();
		for (FoodPatch p : patches.values()) {
			for (RFIDTag tag: p.kidsAtPatch) {
				if (tag.alive && Math.random()<p.killProb) {
					tag.alive = false;
					victims.add(tag);
				}
			}
		}
		return victims;
	}
	
	
	public synchronized List<RFIDTag> updateVictims() {
		List<RFIDTag> alives = new ArrayList<RFIDTag>();
		for (FoodPatch p : patches.values()) {
			for (RFIDTag tag: p.kidsAtPatch) {
				if (!tag.alive) {
					tag.remainingPBTime--;
					if (tag.remainingPBTime<=0 && tag.currentLocation.equals("fg-den")) {
						tag.remainingPBTime = penaltySec;
						tag.alive = true;
						alives.add(tag);
					}
				}
			}
		}
		return alives;
	}
	
	
	public synchronized void updateTagLocation(String tag, String patch, int event) {
		if (event==1) {
			// Arrival
			patches.get(patch).kidsAtPatch.add(tags.get(tag));
			tags.get(tag).currentLocation = patch;
		} else {
			// Departure
			patches.get(patch).kidsAtPatch.remove(tags.get(tag));
			tags.get(tag).currentLocation = null;
		}
	}
	
	
	public synchronized void resetGame() {
		System.out.println("Resetting game...");
		tags = new HashMap<String, RFIDTag>();
		patches = new HashMap<String, FoodPatch>();
		if (filename!=null) {
			// Read everything from the file
		} else {
			// Hard-coded initialization. YIKES!!!
			penaltySec = 30;
			
			// Patches
			patches.put("fg-patch-1", new FoodPatch("fg-patch-1", 10, 1.0/80.0));
			patches.put("fg-patch-2", new FoodPatch("fg-patch-2", 10, 1.0/120.0));
			patches.put("fg-patch-3", new FoodPatch("fg-patch-3", 15, 1.0/80.0));
			patches.put("fg-patch-4", new FoodPatch("fg-patch-4", 15, 1.0/120.0));
			patches.put("fg-patch-5", new FoodPatch("fg-patch-5", 20, 1.0/80.0));
			patches.put("fg-patch-6", new FoodPatch("fg-patch-6", 20, 1.0/120.0));
			patches.put("fg-den", new FoodPatch("fg-den", 0, 0));
			
			// Cluster A
			tags.put("1623110", new RFIDTag("1623110", "a", "#7b2e1a", penaltySec)); //Brown
			tags.put("1623624", new RFIDTag("1623624", "a", "#cb5012", penaltySec)); //Orange3
			tags.put("1623305", new RFIDTag("1623305", "a", "#99896f", penaltySec)); //Gray-ish
			tags.put("1623728", new RFIDTag("1623728", "a", "#ffd1a7", penaltySec)); //Cream-ish 
			// Cluster B
			tags.put("1623302", new RFIDTag("1623302", "b", "#89369e", penaltySec)); //Purple
			tags.put("1623386", new RFIDTag("1623386", "b", "#edac52", penaltySec)); //Orangy
			tags.put("1623126", new RFIDTag("1623126", "b", "#c33d2f", penaltySec)); //Brown
			tags.put("1623972", new RFIDTag("1623972", "b", "#f6e6d9", penaltySec)); //Cream
			// Cluster C
			tags.put("1623683", new RFIDTag("1623683", "c", "#e38b31", penaltySec)); //Orange
			tags.put("1623663", new RFIDTag("1623663", "c", "#ffbeb4", penaltySec)); //Pink
			tags.put("1623641", new RFIDTag("1623641", "c", "#146d71", penaltySec)); //Blue
			tags.put("1623454", new RFIDTag("1623454", "c", "#89369e", penaltySec)); //Purple
			// Cluster D
			tags.put("1623257", new RFIDTag("1623257", "d", "#ffbeb4", penaltySec)); //Pink
			tags.put("1623667", new RFIDTag("1623667", "d", "#146d71", penaltySec)); //Blue
			tags.put("1623303", new RFIDTag("1623303", "d", "#db773c", penaltySec)); //Orange2
			tags.put("1623115", new RFIDTag("1623115", "d", "#edac52", penaltySec)); //Orangy
			tags.put("1623373", new RFIDTag("1623373", "d", "#99896f", penaltySec)); //Gray-ish
			
		}
	}


	public void printScores() {
		System.out.println("===Scores===");
		String tag_ids = "|";
		for (RFIDTag t : tags.values()) {
			tag_ids += (t.id + "\t|");
		}
		System.out.println(tag_ids);
		String scores = "|";
		for (RFIDTag t : tags.values()) {
			scores += (t.score + "      \t|");
		}
		System.out.println(scores);
	}

}
