package tremdas7.model;

import com.google.maps.model.TravelMode;

import lombok.Data;

@Data
public class Passenger {

	private String name;
	
	private String location;
	
	private TravelMode modals;
	
	private String description;
	
	private Ticket ticket;
	
}
