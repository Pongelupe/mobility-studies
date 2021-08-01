package tremdas7;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tremdas7.model.Location;
import tremdas7.model.Passenger;
import tremdas7.model.Trem7;

@Slf4j
public class App {

	private static ObjectMapper mapper = new ObjectMapper(); 
	
	public static void main(String... args) throws Exception {
		mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
		
		var trem7 = mapper.readValue(App.class.getClassLoader().getResourceAsStream("data.json"), Trem7.class);
		var target = trem7.getTarget();
		
		var key = args[0];
		var geoApiContext = new GeoApiContext.Builder()
				.apiKey(key)
				.build();
		
		log.info("Creating a new Trem das 7. Everyone abord!\nThe destination is {}", target.getWhere());
		
		trem7.getPassengers()
			.stream()
			.forEach(p -> {
				log.info("{} has joined", p.getName());
				DirectionsResult directionsResult = extracted(p, target, geoApiContext);
				System.out.println(directionsResult);
			});
		
		
	}
	
	@SneakyThrows
	private static DirectionsResult extracted(Passenger p, Location target, GeoApiContext geoApiContext) {
		return new DirectionsApiRequest(geoApiContext)
				.destination(target.getWhere())
				.origin(p.getLocation())
				.await();
	}

}
