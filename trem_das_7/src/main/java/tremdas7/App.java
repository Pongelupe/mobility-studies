package tremdas7;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tremdas7.model.Location;
import tremdas7.model.Passenger;
import tremdas7.model.Ticket;
import tremdas7.model.Trem7;

@Slf4j
public class App {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
	private static ObjectMapper mapper = new ObjectMapper(); 
	
	public static void main(String... args) throws Exception {
		mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule()
				.addDeserializer(LocalDateTime.class, 
						new LocalDateTimeDeserializer(DATE_TIME_FORMATTER)));
		
		var trem7 = mapper.readValue(App.class.getClassLoader().getResourceAsStream("data.json"), Trem7.class);
		var target = trem7.getTarget();
		
		var key = args[0]; // the first argument MUST be the Google Maps API key
		var geoApiContext = new GeoApiContext.Builder()
				.apiKey(key)
				.build();
		
		log.info("Creating a new Trem das 7. Everyone abord!");
		
		trem7.getPassengers()
			.stream()
			.forEach(p -> {
				log.info("{} is buying a ticket", p.getName());
				var directions = getDirections(p, target, geoApiContext);
				var t = getTicket(directions, target.getTime());
				p.setTicket(t);
				
				log.info("{}'s ticket:\n\n"
						+ "Orign: {}\n"
						+ "Destination: {}\n"
						+ "Fare: R$ {}\n"
						+ "Mode: {}\n"
						+ "DepartureTime: {}\n"
						+ "Arrival time: {}\n"
						+ "Travel expected distance: {}\n"
						+ "Travel expected duration: {}", p.getName(), t.getStartAddress(), 
						t.getEndAddress(), t.getFare(), p.getModals().name(),
						DATE_TIME_FORMATTER.format(t.getDepartureTime()),
						DATE_TIME_FORMATTER.format(t.getArrivalTime()),
						t.getExpectedDistanceTravel(), t.getExpectedTimeTravel()
						);
			});
	}
	
	@SneakyThrows
	private static DirectionsResult getDirections(Passenger p, Location target, GeoApiContext geoApiContext) {
		return new DirectionsApiRequest(geoApiContext)
				.destination(target.getWhere())
				.origin(p.getLocation())
				.mode(p.getModals())
				.arrivalTime(target.getTime().atZone(ZoneId.systemDefault()).toInstant())
				.await();
	}
	
	private static Ticket getTicket(DirectionsResult directions, LocalDateTime arrivalTime) {
		var route = directions.routes[0];
		var leg = route.legs[0];
		
		return Ticket.builder()
				.departureTime(Optional.ofNullable(leg.departureTime)
						.map(ZonedDateTime::toLocalDateTime)
						.orElseGet(() -> arrivalTime.minus(leg.duration.inSeconds, ChronoUnit.SECONDS)))
				.arrivalTime(arrivalTime)
				.expectedTimeTravel(leg.duration.humanReadable)
				.expectedDistanceTravel(leg.distance.humanReadable)
				.fare(Optional.ofNullable(route.fare)
						.map(f -> f.value)
						.orElse(BigDecimal.ZERO))
				.startAddress(leg.startAddress)
				.endAddress(leg.endAddress)
				.build();
	}

}
