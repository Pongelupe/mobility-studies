package generator.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class EstruturaHumberto {

	private final String headsign;
	
	@Builder.Default
	private Map<String, List<A>> tripsPerWeekday = new HashMap<>();
	
}
