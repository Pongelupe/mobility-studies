package generator.models;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
	
	@JsonFormat(pattern = "HH:mm:ss", timezone = "America/Sao_Paulo")
	private Date routeDepartureTime;

	@JsonFormat(pattern = "HH:mm:ss", timezone = "America/Sao_Paulo")
	private Date routeArrivalTime;
	
	private double length;
	
	@JsonIgnore
	private LineString geom;
	
}
