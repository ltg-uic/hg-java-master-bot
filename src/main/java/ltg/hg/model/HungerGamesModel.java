package ltg.hg.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class HungerGamesModel extends Observable {

	// Model
	private Map<String, RFIDTag> tags = null;
	private Map<String, FoodPatch> patches = null;
	private double predation_penalty_length_in_seconds = -1.0d;
	private double bout_length_in_seconds = 0.0d;
	// State
	private String current_habitat_configuration = null;
	private String current_bout_id = null;
	private String current_state = null;
	// Updater thread (component)
	private static final int UPDATE_CYCLE_IN_SECONDS = 1;
	private final HGModelUpdated modelUpdater = new HGModelUpdated("HGModelUpdater");


	/////////////////
	// Constructor //
	/////////////////

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
		predation_penalty_length_in_seconds = patchesConfiguration.getInt("predation_penalty_length_in_seconds");
		
	}


	////////////////////
	// Updater thread //
	////////////////////

	private final class HGModelUpdated extends Thread {
		// Private constructor: only the outer class can instantiate the updater 
		private HGModelUpdated(String id) {
			super(id);
		}

		// Updates the simulation and the stats while foraging 
		// and notifies the master agent
		public void run() {
			while(!modelUpdater.isInterrupted()) {
				if ( getCurrentState()!=null && getCurrentState().equals("foraging") ) {
					updateAggregateStatistics();
					if (getCurrentHabitatConfiguration()!= null && getCurrentHabitatConfiguration().equals("predation"))
						killTags();
					HungerGamesModel.this.setChanged();
					HungerGamesModel.this.notifyObservers();
				}
				try {
					sleep(UPDATE_CYCLE_IN_SECONDS*1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	private synchronized void updateAggregateStatistics() {
		bout_length_in_seconds += UPDATE_CYCLE_IN_SECONDS;
		for (String tag: tags.keySet()) {
			tags.get(tag).updateHarvest(getCurrentYieldForTag(tag) * UPDATE_CYCLE_IN_SECONDS);
			tags.get(tag).updateAverageQuality(bout_length_in_seconds, UPDATE_CYCLE_IN_SECONDS, getCurrentQualityForTag(tag));
			tags.get(tag).updateAverageCompetition(bout_length_in_seconds, UPDATE_CYCLE_IN_SECONDS, getCurrentCompetitionForTag(tag));
			//tags.get(tag).updateAverageRisk(bout_length_in_seconds, UPDATE_CYCLE_IN_SECONDS, getCurrentRiskForTag(tag));
		}	
	}
	
	private synchronized void killTags() {
		// TODO Auto-generated method stub
		//predation_penalty_length_in_seconds;
	}


	//////////////
	// Handlers //
	//////////////

	public synchronized void updateTagLocation(String tag, String departure, String arrival) {
		double my_current_yield = -1.0d;
		double my_displayed_future_yield = -1.0d;
		double my_actual_future_yield = -1.0d;
		if (departure!=null) {
			my_current_yield = getCurrentYieldForTag(tag);
			resetTagLocation(tag);
			removeTagFromPatch(tag, departure);
		}
		if (arrival!=null) {
			my_displayed_future_yield = patches.get(arrival).getCurrentYield();
			setTagLocation(tag, arrival);
			addTagToPatch(tag, arrival);
			my_actual_future_yield = getCurrentYieldForTag(tag);
		}
		if ( departure!=null && arrival!=null && getCurrentState()!=null && getCurrentState().equals("foraging") )
			increasePerMoveAggregateAttributes(tag, my_current_yield, my_displayed_future_yield, my_actual_future_yield);
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

	private synchronized void increasePerMoveAggregateAttributes(String tag, double my_current_yield, double my_displayed_future_yield, double my_actual_future_yield) {
		tags.get(tag).increaseTotalMovesCounter();	
		tags.get(tag).updateArbitrage(my_current_yield, my_displayed_future_yield, my_actual_future_yield);
	}

	public synchronized void clean() {
		modelUpdater.interrupt();
	}


	/////////////////////
	// Utility methods //
	/////////////////////

	public synchronized double getCurrentQualityForTag(String tag) {
		String currentPatch = tags.get(tag).getCurrentLocation();
		return currentPatch==null ? 0 : patches.get(currentPatch).getQuality();
	}

	public synchronized double getCurrentCompetitionForTag(String tag) {
		String currentPatch = tags.get(tag).getCurrentLocation();
		return currentPatch==null ? 0 : patches.get(currentPatch).getCurrentCompetition();
	}

	public synchronized double getCurrentYieldForTag(String tag) {
		String currentPatch = tags.get(tag).getCurrentLocation();
		return currentPatch==null ? 0 : patches.get(currentPatch).getCurrentYield();
	}

	public synchronized double getCurrentRiskForTag(String tag) {
		String currentPatch = tags.get(tag).getCurrentLocation();
		return currentPatch==null ? 0 : patches.get(currentPatch).getRisk();
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

	public synchronized String getCurrentHabitatConfiguration() {
		return current_habitat_configuration;
	}

	public synchronized String getCurrentBoutId() {
		return current_bout_id;
	}

	public synchronized String getCurrentState() {
		return current_state;
	}

	public synchronized void setFullState(String habitat_configuration, String bout_id, String state) {
		setCurrentHabitatConfiguration(habitat_configuration);
		setCurrentBoutId(bout_id);
		setCurrentState(state);
	}

	public synchronized void setCurrentHabitatConfiguration (
			String current_habitat_configuration) {
		this.current_habitat_configuration = current_habitat_configuration;
	}

	public synchronized void setCurrentBoutId(String current_bout_id) {
		this.current_bout_id = current_bout_id;
	}

	public synchronized void setCurrentState(String current_state) {
		this.current_state = current_state;
	}

	public synchronized BasicDBObject getStats() {
		BasicDBObject bout_stats = new BasicDBObject("bout_length", bout_length_in_seconds);

		BasicDBList user_stats = new BasicDBList();
		for (String tag: tags.keySet() ) {
			BasicDBObject user = new BasicDBObject("name", tag)
			.append("harvest", tags.get(tag).getHarvest())
			.append("avg_quality", tags.get(tag).getAvgQuality())
			.append("avg_competition", tags.get(tag).getAvgCompetition())
			.append("total_moves", tags.get(tag).getTotalMoves())
			.append("arbitrage", tags.get(tag).getArbitrage())
			.append("avg_risk", tags.get(tag).getAvgRisk());
			user_stats.add(user);
		}

		BasicDBObject stats = new BasicDBObject()
		.append("bout_stats", bout_stats)
		.append("user_stats", user_stats);

		return stats;
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
