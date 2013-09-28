/**
 * 
 */
package ltg.hg;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import ltg.commons.SimpleRESTClient;
import ltg.commons.ltg_handler.LTGEvent;
import ltg.commons.ltg_handler.LTGEventHandler;
import ltg.commons.ltg_handler.LTGEventListener;
import ltg.hg.model.HungerGamesModel;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;

/**
 * @author gugo
 *
 */
public class HungerGamesMasterBot implements Observer {
	// Software components
	private SimpleRESTClient src = new SimpleRESTClient();
	private LTGEventHandler eh = null;
	private DB db = null;

	// Model data
	private String run_id = null;
	private HungerGamesModel hg = null;


	public HungerGamesMasterBot(String usernameAndPass, String groupChatID, String mongoDBId, String run_id) {

		// ---------------------------------------
		// Init event handler and connect to Mongo
		// ---------------------------------------
		eh =  new LTGEventHandler(usernameAndPass+"@ltg.evl.uic.edu", usernameAndPass, groupChatID+"@conference.ltg.evl.uic.edu");
		try {
			db = new MongoClient("localhost").getDB(mongoDBId);
		} catch (UnknownHostException e) {
			System.err.println("Impossible to connect to MongoDB, terminating...");
			System.exit(0);
		}


		// ------------------------------------------------------
		// Fetch the roster, the configuration and init the model
		// ------------------------------------------------------
		this.run_id = run_id;
		initializeModelAndState();


		// ----------------------------
		//Register XMPP event listeners
		// ----------------------------
		eh.registerHandler("rfid_update", new LTGEventListener() {
			public void processEvent(LTGEvent e) {
				hg.updateTagLocation(
						e.getPayload().get("id").textValue(), 
						e.getPayload().get("departure").textValue(), 
						e.getPayload().get("arrival").textValue()
						);
			}
		});

		eh.registerHandler("start_bout", new LTGEventListener() {
			public void processEvent(LTGEvent e) {
				hg.setCurrentState("foraging");
				saveState();
			}
		});

		eh.registerHandler("stop_bout", new LTGEventListener() {
			public void processEvent(LTGEvent e) {
				hg.setCurrentState("completed");
				saveState();
			}
		});
		
		
		eh.registerHandler("reset_bout", new LTGEventListener() {
			public void processEvent(LTGEvent e) {
				resetHGModel();
				hg.setFullState(
						e.getPayload().get("habitat_configuration_id").textValue(),
						e.getPayload().get("bout_id").textValue(),
						"ready"
						);
				saveState();
			}
		});


		// -------------------------
		// Start XMPP event listener
		// -------------------------
		eh.runSynchronously();
	}
	
	
	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		//saveStatsInDB(hg.getStats());
		System.out.println("Updating summative stats " + hg.getCurrentBoutId() + " " + new Random());
		// Send out kill messages
	}


	private void initializeModelAndState() {
		resetHGModel();
		loadState();
	}


	private void resetHGModel() {
		if (hg!=null) {
			hg.clean();
			hg.deleteObservers();
		}
		ArrayNode roster = null;
		BasicDBObject patchesConfiguration = null;
		try {
			roster = (ArrayNode) src.get("http://ltg.evl.uic.edu:9000/runs/"+run_id).get("data").get("roster");
			patchesConfiguration = (BasicDBObject) db.getCollection("configuration").findOne(new BasicDBObject("run_id", run_id));
			if (roster.size()==0 || patchesConfiguration==null)
				throw new IOException();
		} catch (Exception e) {
			System.err.println("Impossible to fetch roster and/or configuration, terminating...");
			System.exit(0);
		}
		hg = new HungerGamesModel(roster, patchesConfiguration);
		hg.addObserver(this);
	}

	private void loadState() {
		BasicDBObject state = (BasicDBObject) db.getCollection("state").findOne(new BasicDBObject("run_id", run_id)).get("state");
		hg.setFullState(
				state.getString("current_habitat_configuration"), 
				state.getString("current_bout_id"), 
				state.getString("current_state")
				);
	}

	private void saveState() {
		BasicDBObject tmp_state = new BasicDBObject()
		.append("current_habitat_configuration", hg.getCurrentHabitatConfiguration())
		.append("current_bout_id", hg.getCurrentBoutId())
		.append("current_state", hg.getCurrentState());
		db.getCollection("state").update(new BasicDBObject("run_id", run_id), 
				new BasicDBObject("run_id", run_id).append("state", tmp_state) );
	}

	private void saveStatsInDB(Object updateStats) {
		BasicDBObject bout_stats = new BasicDBObject("bout_length", 423);
		
		BasicDBList user_stats = new BasicDBList();
		BasicDBObject us = new BasicDBObject()
		.append("name", "JUR")
		.append("total_calories", 1234.4)
		.append("avg_richness", 2.0)
		.append("avg_competition", 3.5)
		.append("total_moves", 11)
		.append("yield", 2.3)
		.append("avg_risk", 3.4);
		user_stats.add(us);
		
		BasicDBObject stats = new BasicDBObject()
		.append("run_id", run_id)
		.append("habitat_configuration", hg.getCurrentHabitatConfiguration())
		.append("bout_id", hg.getCurrentBoutId())
		.append("bout_stats", bout_stats)
		.append("user_stats", user_stats);
		
		//Store in MongoDB
		BasicDBObject query = new BasicDBObject()
				.append("run_id", run_id)
				.append("habitat_configuration", hg.getCurrentHabitatConfiguration())
				.append("bout_id", hg.getCurrentBoutId());
		db.getCollection("statistics").update( query, stats );
	}




	/**
	 * MAIN Parses CLI arguments and launches an instance of the master bot
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 4 || 
				args[0]==null || args[0].isEmpty() || 
				args[1]==null || args[1].isEmpty() || 
				args[2]==null || args[2].isEmpty() ||
				args[3]==null || args[3].isEmpty()) {
			System.out.println("Need to specify the username/password (eg. hg-bots#master), "
					+ "chatroom ID (eg. hg-test), "
					+ "MongoDB (e.g. hunger-games-fall-13) "
					+ "and run_id (e.g period-1). Terminating...");
			System.exit(0);
		}
		new HungerGamesMasterBot(args[0], args[1], args[2], args[3]);      
	}



















	// ------------------
	// Old code
	// ------------------

	//	private void startAgent() {
	//		// Register event listener
	//		xmpp.registerEventListener(this);
	//		// Start killing thread
	//		while (!Thread.currentThread().isInterrupted()) {
	//			if (collectData) {
	//				sendKills(fg.selectVictims());
	//				sendAlives(fg.updateVictims());
	//			}
	//			try {
	//				Thread.sleep(1000);
	//			} catch (InterruptedException e) {
	//				System.err.println("We've been interrupted");
	//			}
	//		}
	//		xmpp.disconnect();
	//	}


	//	public void processMessage(Message m) {
	//		JsonObject json = null;
	//		JsonElement jsone = null;
	//		try {
	//			jsone = parser.parse(m.getBody());
	//			if (!jsone.isObject()) {
	//				// The element is not an object... bad...
	//				return;
	//			}
	//			json = jsone.asObject();
	//			// Pick the right JSON handler based on the event type
	//			if (isPatchInit(json)) { 
	//				sendPatchInitInfo(json.getString("origin"));
	//			} else if (isLifeDisplayInit(json)) {
	//				sendLifeDisplayInitInfo(json);
	//			} else if (isTeacherDisplayInit(json)) {
	//				sendTeacherDisplayInitInfo(json);
	//			} else if (isPenaltyBoxInit(json)) {
	//				sendPenaltyBoxInitInfo(json);

	//			} else if (isScoreUpdate(json)) {
	//				updateScore(json);
	//			} else if (isLocationUpdate(json)) {
	//				updateLocation(json);
	//			} else if (isGameReset(json)) {
	//				resetGame();
	//			} else if (isGameStop(json)) {
	//				stopGame();
	//			}
	//		} catch (JsonParseException e) {
	//			// Not JSON... skip
	//			//log.info("Not JSON: " + m.getBody());
	//		}
	//	}
	//
	//
	//	private void updateLocation(JsonObject json) {
	//		if (!collectData)
	//			return;
	//		String dest = json.getString("destination");
	//		for (JsonElement a : json.getArray("payload", "arrivals")) {
	//			fg.updateTagLocation(a.asPrimitive().asString(), dest, 1);
	//		}
	//		for (JsonElement a : json.getArray("payload", "departures")) {
	//			fg.updateTagLocation(a.asPrimitive().asString(), dest, -1);
	//		}
	//	}
	//
	//
	//	private boolean isLocationUpdate(JsonObject json) {
	//		if (json.getString("event")!= null && 
	//				json.getString("event").equals("rfid_update") && 
	//				json.getString("destination")!= null && 
	//				json.getObject("payload") != null)
	//			return true;
	//		return false;
	//	}
	//
	//
	//	private void sendPatchInitInfo(String origin) {
	//		if (fg.getPatch(origin)==null) {
	//			System.out.println("This patch doesn't exist!");
	//			return;
	//		}
	//		JsonObject response = new JsonObject();
	//		// Add all the tags
	//		JsonArray tags = new JsonArray();
	//		Collection<RFIDTag> tc = fg.getAllTags();
	//		for (RFIDTag t: tc) {
	//			JsonObject o = new JsonObject();
	//			o.put("tag", t.id);
	//			o.put("cluster", t.cluster);
	//			o.put("color", t.color);
	//			tags.add(o);
	//		}
	//		// Compose the payload
	//		JsonObject payload = new JsonObject();
	//		payload.put("feed-ratio", fg.getPatch(origin).feedRatio);
	//		payload.put("tags", tags);
	//		// Compose the event
	//		response.put("event", "patch_init_data");
	//		response.put("destination", origin); 
	//		response.put("payload", payload);
	//		xmpp.sendMessage(response.toString());
	//	}
	//	
	//	
	//	private void sendLifeDisplayInitInfo(JsonObject json) {
	//		JsonObject response = new JsonObject();
	//		// Add all the patches
	//		JsonArray patches = new JsonArray();
	//		Collection<FoodPatch> fc = fg.getAllPatches();
	//		for (FoodPatch fp : fc) {
	//			JsonObject o = new JsonObject();
	//			o.put("patch", fp.jid);
	//			o.put("feed-ratio", fp.feedRatio);
	//			patches.add(o);
	//		}
	//		// Add all the tags
	//		JsonArray tags = new JsonArray();
	//		Collection<RFIDTag> tc = fg.getAllTags();
	//		for (RFIDTag t: tc) {
	//			JsonObject o = new JsonObject();
	//			o.put("tag", t.id);
	//			o.put("cluster", t.cluster);
	//			o.put("color", t.color);
	//			tags.add(o);
	//		}
	//		// Compose the payload
	//		JsonObject payload = new JsonObject();
	//		payload.put("patches", patches);
	//		payload.put("tags", tags);
	//		// Compose the event
	//		response.put("event", "life_display_init_data"); 
	//		response.put("payload", payload);
	//		xmpp.sendMessage(response.toString());
	//	}
	//	
	//	
	//	private void sendTeacherDisplayInitInfo(JsonObject json) {
	//		JsonObject response = new JsonObject();
	//		// Add all the patches
	//		JsonArray patches = new JsonArray();
	//		Collection<FoodPatch> fc = fg.getAllPatches();
	//		for (FoodPatch fp : fc) {
	//			JsonObject o = new JsonObject();
	//			o.put("patch", fp.jid);
	//			o.put("feed-ratio", fp.feedRatio);
	//			patches.add(o);
	//		}
	//		// Add all the tags
	//		JsonArray tags = new JsonArray();
	//		Collection<RFIDTag> tc = fg.getAllTags();
	//		for (RFIDTag t: tc) {
	//			JsonObject o = new JsonObject();
	//			o.put("tag", t.id);
	//			o.put("cluster", t.cluster);
	//			o.put("color", t.color);
	//			tags.add(o);
	//		}
	//		// Compose the payload
	//		JsonObject payload = new JsonObject();
	//		payload.put("patches", patches);
	//		payload.put("tags", tags);
	//		// Compose the event
	//		response.put("event", "teacher_display_init_data"); 
	//		response.put("payload", payload);
	//		xmpp.sendMessage(response.toString());
	//	}
	//	
	//	
	//	private void sendPenaltyBoxInitInfo(JsonObject json) {
	//		JsonObject response = new JsonObject();
	//		// Add all the tags
	//		JsonArray tags = new JsonArray();
	//		Collection<RFIDTag> tc = fg.getAllTags();
	//		for (RFIDTag t: tc) {
	//			JsonObject o = new JsonObject();
	//			o.put("tag", t.id);
	//			o.put("cluster", t.cluster);
	//			o.put("color", t.color);
	//			tags.add(o);
	//		}
	//		// Compose the payload
	//		JsonObject payload = new JsonObject();
	//		payload.put("penalty", fg.penaltySec);
	//		payload.put("tags", tags);
	//		// Compose the event
	//		response.put("event", "penalty_box_init_data"); 
	//		response.put("payload", payload);
	//		xmpp.sendMessage(response.toString());
	//	}
	//	
	//	
	//	private void sendKills(List<RFIDTag> victims) {
	//		JsonObject response = null;
	//		for (RFIDTag v: victims) {
	//			response = new JsonObject();
	//			// Compose the payload
	//			JsonObject payload = new JsonObject();
	//			payload.put("id", v.id);
	//			// Compose the event
	//			response.put("event", "kill_bunny");
	//			response.put("destination", v.currentLocation);
	//			response.put("payload", payload);
	//			xmpp.sendMessage(response.toString());
	//		}
	//	}
	//	
	//	
	//	private void sendAlives(List<RFIDTag> alives) {
	//		JsonObject response = null;
	//		for (RFIDTag v: alives) {
	//			response = new JsonObject();
	//			// Compose the payload
	//			JsonObject payload = new JsonObject();
	//			payload.put("id", v.id);
	//			// Compose the event
	//			response.put("event", "bunny_alive");
	//			response.put("payload", payload);
	//			xmpp.sendMessage(response.toString());
	//		}
	//	}
	//	
	//		
	//	private void updateScore(JsonObject json) {
	//		fg.addPoints(json.getString("payload", "tag"), json.getInt("payload", "score"));
	//	}
	//	
	//	
	//	private void resetGame() {
	//		fg.resetGame();
	//		collectData = true;
	//	}
	//	
	//	
	//	private void initGame() {
	//		fg.resetGame();
	//	}
	//
	//	
	//	
	//	private void stopGame() {
	//		collectData = false;
	//	}
	//
	//
	//	private boolean isPatchInit(JsonObject json) {
	//		if (json.getString("event")!= null && 
	//				json.getString("event").equals("patch_init") && 
	//				json.getString("origin")!= null && 
	//				json.getObject("payload")!= null)
	//			return true;
	//		return false;
	//	}
	//	
	//	
	//	private boolean isLifeDisplayInit(JsonObject json) {
	//		if (json.getString("event")!= null && 
	//				json.getString("event").equals("life_display_init"))
	//			return true;
	//		return false;
	//	}
	//	
	//	
	//	private boolean isTeacherDisplayInit(JsonObject json) {
	//		if (json.getString("event")!= null && 
	//				json.getString("event").equals("teacher_display_init"))
	//			return true;
	//		return false;
	//	}
	//	
	//	
	//	private boolean isPenaltyBoxInit(JsonObject json) {
	//		if (json.getString("event")!= null && 
	//				json.getString("event").equals("penalty_box_init"))
	//			return true;
	//		return false;
	//	}
	//
	//	
	//	private boolean isScoreUpdate(JsonObject json) {
	//		if (json.getString("event")!= null && 
	//				json.getString("event").equals("score_update") && 
	//				json.getString("origin")!= null && 
	//				json.getString("payload", "tag")!= null &&
	//				json.getString("payload", "score")!= null)
	//			return true;
	//		return false;
	//	}
	//	
	//	private boolean isGameReset(JsonObject json) {
	//		if (json.getString("event")!= null && 
	//				json.getString("event").equals("game_reset"))
	//			return true;
	//		return false;
	//	}
	//	
	//	private boolean isGameStop(JsonObject json) {
	//		if (json.getString("event")!= null && 
	//				json.getString("event").equals("game_stop"))
	//			return true;
	//		return false;
	//	}

}
