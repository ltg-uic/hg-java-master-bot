/**
 * 
 */
package ltg.foraging;

import java.util.Collection;
import java.util.List;

import ltg.commons.MessageListener;
import ltg.commons.SimpleXMPPClient;
import ltg.foraging.model.FoodPatch;
import ltg.foraging.model.ForagingGame;
import ltg.foraging.model.RFIDTag;

import org.jivesoftware.smack.packet.Message;

import com.github.jsonj.JsonArray;
import com.github.jsonj.JsonElement;
import com.github.jsonj.JsonObject;
import com.github.jsonj.exceptions.JsonParseException;
import com.github.jsonj.tools.JsonParser;

/**
 * @author tebemis
 *
 */
public class MasterAgent implements MessageListener {
	
	// Data collection flag
	private boolean collectData = false;
	// XMMP client
	private SimpleXMPPClient xmpp = null;
	// JSON parser
	private JsonParser parser = new JsonParser();
	
	// Foraging game instance
	private ForagingGame fg = new ForagingGame();
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MasterAgent ma = new MasterAgent();
		ma.initializeAgent();
		ma.startAgent();
	}
	

	private void initializeAgent() {
		initGame();
		xmpp = new SimpleXMPPClient("fg-master@ltg.evl.uic.edu", "fg-master", "fg-pilot-oct12@conference.ltg.evl.uic.edu");
		System.out.println("Connected to chatroom and waiting for commands");
	}
	
	
	private void startAgent() {
		// Register event listener
		xmpp.registerEventListener(this);
		// Start killing thread
		while (!Thread.currentThread().isInterrupted()) {
			if (collectData) {
				sendKills(fg.selectVictims());
				sendAlives(fg.updateVictims());
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.err.println("We've been interrupted");
			}
		}
		xmpp.disconnect();
	}


	public void processMessage(Message m) {
		JsonObject json = null;
		JsonElement jsone = null;
		try {
			jsone = parser.parse(m.getBody());
			if (!jsone.isObject()) {
				// The element is not an object... bad...
				return;
			}
			json = jsone.asObject();
			// Pick the right JSON handler based on the event type
			if (isPatchInit(json)) { 
				sendPatchInitInfo(json.getString("origin"));
			} else if (isLifeDisplayInit(json)) {
				sendLifeDisplayInitInfo(json);
			} else if (isTeacherDisplayInit(json)) {
				sendTeacherDisplayInitInfo(json);
			} else if (isPenaltyBoxInit(json)) {
				sendPenaltyBoxInitInfo(json);
			} else if (isScoreUpdate(json)) {
				updateScore(json);
			} else if (isLocationUpdate(json)) {
				updateLocation(json);
			} else if (isGameReset(json)) {
				resetGame();
			} else if (isGameStop(json)) {
				stopGame();
			}
		} catch (JsonParseException e) {
			// Not JSON... skip
			//log.info("Not JSON: " + m.getBody());
		}
	}


	private void updateLocation(JsonObject json) {
		if (!collectData)
			return;
		String dest = json.getString("destination");
		for (JsonElement a : json.getArray("payload", "arrivals")) {
			fg.updateTagLocation(a.asPrimitive().asString(), dest, 1);
		}
		for (JsonElement a : json.getArray("payload", "departures")) {
			fg.updateTagLocation(a.asPrimitive().asString(), dest, -1);
		}
	}


	private boolean isLocationUpdate(JsonObject json) {
		if (json.getString("event")!= null && 
				json.getString("event").equals("rfid_update") && 
				json.getString("destination")!= null && 
				json.getObject("payload") != null)
			return true;
		return false;
	}


	private void sendPatchInitInfo(String origin) {
		if (fg.getPatch(origin)==null) {
			System.out.println("This patch doesn't exist!");
			return;
		}
		JsonObject response = new JsonObject();
		// Add all the tags
		JsonArray tags = new JsonArray();
		Collection<RFIDTag> tc = fg.getAllTags();
		for (RFIDTag t: tc) {
			JsonObject o = new JsonObject();
			o.put("tag", t.id);
			o.put("cluster", t.cluster);
			o.put("color", t.color);
			tags.add(o);
		}
		// Compose the payload
		JsonObject payload = new JsonObject();
		payload.put("feed-ratio", fg.getPatch(origin).feedRatio);
		payload.put("tags", tags);
		// Compose the event
		response.put("event", "patch_init_data");
		response.put("destination", origin); 
		response.put("payload", payload);
		xmpp.sendMessage(response.toString());
	}
	
	
	private void sendLifeDisplayInitInfo(JsonObject json) {
		JsonObject response = new JsonObject();
		// Add all the patches
		JsonArray patches = new JsonArray();
		Collection<FoodPatch> fc = fg.getAllPatches();
		for (FoodPatch fp : fc) {
			JsonObject o = new JsonObject();
			o.put("patch", fp.jid);
			o.put("feed-ratio", fp.feedRatio);
			patches.add(o);
		}
		// Add all the tags
		JsonArray tags = new JsonArray();
		Collection<RFIDTag> tc = fg.getAllTags();
		for (RFIDTag t: tc) {
			JsonObject o = new JsonObject();
			o.put("tag", t.id);
			o.put("cluster", t.cluster);
			o.put("color", t.color);
			tags.add(o);
		}
		// Compose the payload
		JsonObject payload = new JsonObject();
		payload.put("patches", patches);
		payload.put("tags", tags);
		// Compose the event
		response.put("event", "life_display_init_data"); 
		response.put("payload", payload);
		xmpp.sendMessage(response.toString());
	}
	
	
	private void sendTeacherDisplayInitInfo(JsonObject json) {
		JsonObject response = new JsonObject();
		// Add all the patches
		JsonArray patches = new JsonArray();
		Collection<FoodPatch> fc = fg.getAllPatches();
		for (FoodPatch fp : fc) {
			JsonObject o = new JsonObject();
			o.put("patch", fp.jid);
			o.put("feed-ratio", fp.feedRatio);
			patches.add(o);
		}
		// Add all the tags
		JsonArray tags = new JsonArray();
		Collection<RFIDTag> tc = fg.getAllTags();
		for (RFIDTag t: tc) {
			JsonObject o = new JsonObject();
			o.put("tag", t.id);
			o.put("cluster", t.cluster);
			o.put("color", t.color);
			tags.add(o);
		}
		// Compose the payload
		JsonObject payload = new JsonObject();
		payload.put("patches", patches);
		payload.put("tags", tags);
		// Compose the event
		response.put("event", "teacher_display_init_data"); 
		response.put("payload", payload);
		xmpp.sendMessage(response.toString());
	}
	
	
	private void sendPenaltyBoxInitInfo(JsonObject json) {
		JsonObject response = new JsonObject();
		// Add all the tags
		JsonArray tags = new JsonArray();
		Collection<RFIDTag> tc = fg.getAllTags();
		for (RFIDTag t: tc) {
			JsonObject o = new JsonObject();
			o.put("tag", t.id);
			o.put("cluster", t.cluster);
			o.put("color", t.color);
			tags.add(o);
		}
		// Compose the payload
		JsonObject payload = new JsonObject();
		payload.put("penalty", fg.penaltySec);
		payload.put("tags", tags);
		// Compose the event
		response.put("event", "penalty_box_init_data"); 
		response.put("payload", payload);
		xmpp.sendMessage(response.toString());
	}
	
	
	private void sendKills(List<RFIDTag> victims) {
		JsonObject response = null;
		for (RFIDTag v: victims) {
			response = new JsonObject();
			// Compose the payload
			JsonObject payload = new JsonObject();
			payload.put("id", v.id);
			// Compose the event
			response.put("event", "kill_bunny");
			response.put("destination", v.currentLocation);
			response.put("payload", payload);
			xmpp.sendMessage(response.toString());
		}
	}
	
	
	private void sendAlives(List<RFIDTag> alives) {
		JsonObject response = null;
		for (RFIDTag v: alives) {
			response = new JsonObject();
			// Compose the payload
			JsonObject payload = new JsonObject();
			payload.put("id", v.id);
			// Compose the event
			response.put("event", "bunny_alive");
			response.put("payload", payload);
			xmpp.sendMessage(response.toString());
		}
	}
	
		
	private void updateScore(JsonObject json) {
		fg.addPoints(json.getString("payload", "tag"), json.getInt("payload", "score"));
	}
	
	
	private void resetGame() {
		fg.resetGame();
		collectData = true;
	}
	
	
	private void initGame() {
		fg.resetGame();
	}

	
	
	private void stopGame() {
		collectData = false;
	}


	private boolean isPatchInit(JsonObject json) {
		if (json.getString("event")!= null && 
				json.getString("event").equals("patch_init") && 
				json.getString("origin")!= null && 
				json.getObject("payload")!= null)
			return true;
		return false;
	}
	
	
	private boolean isLifeDisplayInit(JsonObject json) {
		if (json.getString("event")!= null && 
				json.getString("event").equals("life_display_init"))
			return true;
		return false;
	}
	
	
	private boolean isTeacherDisplayInit(JsonObject json) {
		if (json.getString("event")!= null && 
				json.getString("event").equals("teacher_display_init"))
			return true;
		return false;
	}
	
	
	private boolean isPenaltyBoxInit(JsonObject json) {
		if (json.getString("event")!= null && 
				json.getString("event").equals("penalty_box_init"))
			return true;
		return false;
	}

	
	private boolean isScoreUpdate(JsonObject json) {
		if (json.getString("event")!= null && 
				json.getString("event").equals("score_update") && 
				json.getString("origin")!= null && 
				json.getString("payload", "tag")!= null &&
				json.getString("payload", "score")!= null)
			return true;
		return false;
	}
	
	private boolean isGameReset(JsonObject json) {
		if (json.getString("event")!= null && 
				json.getString("event").equals("game_reset"))
			return true;
		return false;
	}
	
	private boolean isGameStop(JsonObject json) {
		if (json.getString("event")!= null && 
				json.getString("event").equals("game_stop"))
			return true;
		return false;
	}
}
