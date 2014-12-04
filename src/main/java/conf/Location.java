package conf;

import java.awt.Point;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Transient;

@Embedded
public class Location {
	private String id;
	private String name;
	private Point ne;
	private Point sw;

	@Transient
	public int numberOfCoveringClients;

	public Location(String id, String name) {
		this.id = id;
		this.name = name;
		sw = new Point();
		ne = new Point();
	}

	public String getID() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Point getNorthEastPoint() {
		return ne;
	}

	public int getSize() {
		int v = (int) Math.abs(ne.getX() - sw.getX());
		int h = (int) Math.abs(ne.getY() - sw.getY());
		return v * h;
	}

	public Point getSowthWestPoint() {
		return sw;
	}

	public boolean isCovered(double lng, double lat) {
		return lat >= this.sw.getY() && lng >= this.sw.getX()
				&& lat <= this.ne.getY() && lng <= this.ne.getX();
	}

	public void setNorthEastPoint(double lng, double lat) {
		ne.setLocation(lng, lat);
	}

	public void setSowthWestPoint(double lng, double lat) {
		sw.setLocation(lng, lat);
	}
}