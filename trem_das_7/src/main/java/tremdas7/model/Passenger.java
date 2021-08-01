package tremdas7.model;

import java.util.List;

import lombok.Data;

@Data
public class Passenger {

	private String name;
	
	private String location;
	
	private List<String> modals;
	
	private String description;
	
}
