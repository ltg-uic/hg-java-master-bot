package ltg.hg.model;

public class RFIDTag {

	// Assigned attributes
	public final String id;
	public final String rfid_tag;
	public final String color;
	public final String color_label;
	
	// Physical attributes
	private String current_location_id = null;
	
	// Calculated model attributes
	private int current_rate = 0;
	private int current_calories = 0;
	private double average_richness = 0;
	private double average_competition = 0;
	private double total_moves = 0;
	private double yield = 0;
	private boolean is_alive = true;
	private int remaining_penalyt_time = -1;
	
	
	public RFIDTag(String id, String rfid_tag, String color, String color_label) {
		this.id = id;
		this.rfid_tag = rfid_tag;
		this.color = color;
		this.color_label = color_label;
	}


	public synchronized String getCurrent_location() {
		return current_location_id;
	}


	public synchronized int getCurrent_rate() {
		return current_rate;
	}


	public synchronized int getCurrentCalories() {
		return current_calories;
	}


	public synchronized boolean isIs_alive() {
		return is_alive;
	}


	public synchronized int getRemaining_penalyt_time() {
		return remaining_penalyt_time;
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


	public void setCurrentLocation(String patch) {
		this.current_location_id = patch;
	}
	
	public void resetCurrentLocation() {
		this.current_location_id = null;
	}
	
	

}
