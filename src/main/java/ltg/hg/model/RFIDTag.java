package ltg.hg.model;

public class RFIDTag {

	// Assigned attributes
	public final String id;
	public final String rfid_tag;
	public final String color;
	public final String color_label;
	
	// Calculated attrbutes
	private String current_location_id = null;
	private int current_rate = 0;
	private int current_score = 0;
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


	public synchronized int getCurrent_score() {
		return current_score;
	}


	public synchronized boolean isIs_alive() {
		return is_alive;
	}


	public synchronized int getRemaining_penalyt_time() {
		return remaining_penalyt_time;
	}


	public synchronized void setCurrent_location(String current_location) {
		this.current_location_id = current_location;
	}


	public synchronized void setCurrent_rate(int current_rate) {
		this.current_rate = current_rate;
	}


	public synchronized void setCurrent_score(int current_score) {
		this.current_score = current_score;
	}


	public synchronized void setIs_alive(boolean is_alive) {
		this.is_alive = is_alive;
	}


	public synchronized void setRemaining_penalyt_time(int remaining_penalyt_time) {
		this.remaining_penalyt_time = remaining_penalyt_time;
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
	
	

}
