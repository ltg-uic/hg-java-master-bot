package ltg.hg.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class HungerGamesModel {

	private Map<String, RFIDTag> tags = null;
	private Map<String, FoodPatch> patches = null;
	
	private String current_configuration = null;
	private String current_bout = null;


	public HungerGamesModel(ArrayNode roster, BasicDBObject patchesConfiguration) {
		resetGame(roster, patchesConfiguration);
	}


	public synchronized void updateTagLocation(String tag, String departure, String arrival) {
		if (departure!=null) {
			resetTagLocation(tag);
			removeTagFromPatch(tag, departure);
		}
		if (arrival!=null) {
			setTagLocation(tag, arrival);
			addTagToPatch(tag, arrival);
		}
	}


	private synchronized void addTagToPatch(String tag, String patch) {
		patches.get(patch).addTagToPatch(tag);
	}
	
	
	private synchronized void removeTagFromPatch(String tag, String patch) {
		patches.get(patch).removeTagFromPatch(tag);
	}


	private synchronized void setTagLocation(String tag, String patch) {
		tags.get(tag).setCurrent_location(patch);
	}

	private synchronized void resetTagLocation(String tag) {
		tags.get(tag).setCurrent_location(null);
	}

	// TODO need to finish to implement this!!
	private synchronized void resetGame(ArrayNode roster, BasicDBObject patchesConfiguration) {
		tags = new HashMap<String, RFIDTag>();
		patches = new HashMap<String, FoodPatch>();
		for (JsonNode tag: roster)
			tags.put(tag.get("rfid_tag").textValue(), new RFIDTag(
					tag.get("_id").textValue(), 
					tag.get("rfid_tag").textValue(), 
					tag.get("color").textValue(), 
					tag.get("color_label").textValue()
					));		
		for (Object patch: (BasicDBList) patchesConfiguration.get("patches")) 
			patches.put(((BasicDBObject) patch).getString("patch_id"), new FoodPatch(
					((BasicDBObject) patch).getString("patch_id"), 
					((BasicDBObject) patch).getString("richness"), 
					((BasicDBObject) patch).getInt("richness_per_second"), 
					null, 	//((BasicDBObject) patch).getString("risk_label")	 
					0.0d	//((BasicDBObject) patch).getDouble("risk")
					));
	}
	
	
//	public FoodPatch getPatch(String patchId) {
//	return patches.get(patchId);
//}
//
//
//public Collection<FoodPatch> getAllPatches() {
//	return patches.values();
//}
//
//
//public Collection<RFIDTag> getAllTags() {
//	return tags.values();
//}
//
//
//public void addPoints(String tagId, int points) {
//	tags.get(tagId).score += points;
//	System.out.println("Added " + points + " calories to " + tagId);
//}
//
//
//public synchronized List<RFIDTag> selectVictims() {
//	List<RFIDTag> victims = new ArrayList<RFIDTag>();
//	for (FoodPatch p : patches.values()) {
//		for (RFIDTag tag: p.kidsAtPatch) {
//			if (tag.alive && Math.random()<p.killProb) {
//				tag.alive = false;
//				victims.add(tag);
//			}
//		}
//	}
//	return victims;
//}
//
//
//public synchronized List<RFIDTag> updateVictims() {
//	List<RFIDTag> alives = new ArrayList<RFIDTag>();
//	for (FoodPatch p : patches.values()) {
//		for (RFIDTag tag: p.kidsAtPatch) {
//			if (!tag.alive) {
//				tag.remainingPBTime--;
//				if (tag.remainingPBTime<=0 && tag.currentLocation.equals("fg-den")) {
//					tag.remainingPBTime = penaltySec;
//					tag.alive = true;
//					alives.add(tag);
//				}
//			}
//		}
//	}
//	return alives;
//}
//
//


//	public void printScores() {
//		System.out.println("===Scores===");
//		String tag_ids = "|";
//		for (RFIDTag t : tags.values()) {
//			tag_ids += (t.id + "\t|");
//		}
//		System.out.println(tag_ids);
//		String scores = "|";
//		for (RFIDTag t : tags.values()) {
//			scores += (t.score + "      \t|");
//		}
//		System.out.println(scores);
//	}

}
