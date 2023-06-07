package generator.models;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.postgis.jdbc.geometry.LineString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Route {

	private String routeId;
	
	private Date routeDepartureTime;

	private Date routeArrivalTime;
	
	private double length;
	
	private LineString geom;
	
}
