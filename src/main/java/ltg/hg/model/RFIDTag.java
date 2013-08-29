package ltg.hg.model;

public class RFIDTag {

	public String id;
	public String cluster;
	public String color;
	public int score;
	public int rate;
	public String currentLocation;
	public boolean alive;
	public int remainingPBTime;
	
	
	public RFIDTag(String id, String cluster, String color, int penaltySec) {
		this.id = id;
		this.cluster = cluster;
		this.color = color;
		this.score = 0;
		this.rate = 0;
		this.alive = true;
		this.remainingPBTime = penaltySec;
		this.currentLocation = null;
	}
	
	
	public void setRate(int rate) {
		this.rate = rate;
	}

}
