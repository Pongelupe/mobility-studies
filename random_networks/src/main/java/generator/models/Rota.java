package generator.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.postgis.jdbc.geometry.Point;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Rota {

	private String tripId;
	
	private int stopSequence;
	
	private String stopId;
	
	private Point coord;
	
}
