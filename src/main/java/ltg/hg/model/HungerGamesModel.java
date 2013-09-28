package ltg.hg.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class HungerGamesModel {

	// Model
	private Map<String, RFIDTag> tags = null;
	private Map<String, FoodPatch> patches = null;
	// State
	private String current_habitat_configuration = null;
	private String current_bout_id = null;
	private String current_state = null;
	// Updater thread (component)
	private static final int UPDATE_CYCLE_IN_SECONDS = 1;
	private final HGModelUpdated modelUpdater = new HGModelUpdated("HGModelUpdater");


	public HungerGamesModel(ArrayNode roster, BasicDBObject patchesConfiguration) {
		resetGame(roster, patchesConfiguration);
		modelUpdater.start();
	}

	private synchronized void resetGame(ArrayNode roster, BasicDBObject patchesConfiguration) {
		// Initialized tags and patches with JSON coming from DB
		tags = new HashMap<String, RFIDTag>();
		patches = new HashMap<String, FoodPatch>();
		for (JsonNode tag: roster)
			tags.put(tag.get("_id").textValue(), new RFIDTag(
					tag.get("_id").textValue(), 
					tag.get("rfid_tag").textValue(), 
					tag.get("color").textValue(), 
					tag.get("color_label").textValue()
					));		
		for (Object patch: (BasicDBList) patchesConfiguration.get("patches")) 
			patches.put(((BasicDBObject) patch).getString("patch_id"), new FoodPatch(
					((BasicDBObject) patch).getString("patch_id"), 
					((BasicDBObject) patch).getString("quality"), 
					((BasicDBObject) patch).getInt("quality_per_second"), 
					((BasicDBObject) patch).getString("risk_label"), 
					((BasicDBObject) patch).getDouble("risk_percent_per_second")
					));
	}


	////////////////////
	// Updater thread //
	////////////////////

	private final class HGModelUpdated extends Thread {
		/**
		 * Private constructor: only the outer class can instantiate 
		 * the updater.
		 * @param thread name
		 */
		private HGModelUpdated(String id) {
			super(id);
		}

		/**
		 * Updates cumulative stats while foraging
		 */
		public void run() {
			while(Thread.currentThread().isInterrupted()) {
				if (current_state=="foraging")
					updateAggregateStatistics();
				try {
					sleep(UPDATE_CYCLE_IN_SECONDS*1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}

	}
	
	
	private synchronized BasicDBObject updateAggregateStatistics() {
		// TODO Auto-generated method stub
		System.out.println("Updating summative stats " + new Random());
		return null;
	}


	//////////////
	// Handlers //
	//////////////

	/**
	 * This function updates the tags location and updates all the parameters
	 * 
	 * @param tag 
	 * @param departure
	 * @param arrival
	 */
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
		tags.get(tag).setCurrentLocation(patch);
	}

	private synchronized void resetTagLocation(String tag) {
		tags.get(tag).setCurrentLocation(null);
	}	

	
	/////////////////////
	// Utility methods //
	/////////////////////

	public synchronized double getTagCurrentYield(String tag) {
		return patches.get(tags.get(tag).getCurrentLocation()).getCurrentYield();
	}


	////////////////////////
	// Getters and setters /
	////////////////////////

	public synchronized Map<String, RFIDTag> getTags() {
		return tags;
	}

	public synchronized Map<String, FoodPatch> getPatches() {
		return patches;
	}

	public synchronized String getCurrent_habitat_configuration() {
		return current_habitat_configuration;
	}

	public synchronized String getCurrent_bout_id() {
		return current_bout_id;
	}

	public synchronized String getCurrent_state() {
		return current_state;
	}
	
	public synchronized void setFullState(String habitat_configuration, String bout_id, String state) {
		setCurrent_habitat_configuration(habitat_configuration);
		setCurrent_bout_id(bout_id);
		setCurrent_state(state);
	}

	public synchronized void setTags(Map<String, RFIDTag> tags) {
		this.tags = tags;
	}

	public synchronized void setPatches(Map<String, FoodPatch> patches) {
		this.patches = patches;
	}

	public synchronized void setCurrent_habitat_configuration(
			String current_habitat_configuration) {
		this.current_habitat_configuration = current_habitat_configuration;
	}

	public synchronized void setCurrent_bout_id(String current_bout_id) {
		this.current_bout_id = current_bout_id;
	}

	public synchronized void setCurrent_state(String current_state) {
		this.current_state = current_state;
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
