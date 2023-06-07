package generator.models;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class A {

	private final int sequenceTrip;
	
	private final Route route;
	
	@Builder.Default
	private List<B> trip = new ArrayList<>(); 
	
}
