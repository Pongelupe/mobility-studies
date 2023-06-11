package generator.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StopPointsInterval {

	private int stopSequence1;

	private int stopSequence2;
	
	private double length;
	
}
