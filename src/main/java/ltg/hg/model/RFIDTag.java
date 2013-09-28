package ltg.hg.model;

public class RFIDTag {

	// Assigned attributes
	public final String id;
	public final String rfid_tag;
	public final String color;
	public final String color_label;
	
	// Instantaneous attributes
	private String current_location_id = null;
	private boolean is_alive = true;
	private int remaining_penalyt_time = -1;
	
	// Aggregate attributes
	private int harvest = -1;
	private double avg_quality = -1.0d;
	private double average_competition = -1.0d;
	private double total_moves = -1.0d;
	private double arbitrage = -1.0d;
	private double average_risk = -1.0d;
	
	
	
	public RFIDTag(String id, String rfid_tag, String color, String color_label) {
		this.id = id;
		this.rfid_tag = rfid_tag;
		this.color = color;
		this.color_label = color_label;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RFIDTag 
				&& ((RFIDTag) obj).id.equals(id)
				&& ((RFIDTag) obj).rfid_tag.equals(rfid_tag)
			)
			return true;
		return false;
	}


	public synchronized String getCurrentLocation() {
		return current_location_id;
	}
	
	
	public synchronized void setCurrentLocation(String patch) {
		this.current_location_id = patch;
	}
	
	public synchronized void resetCurrentLocation() {
		this.current_location_id = null;
	}
	
	
	
	
	

}
