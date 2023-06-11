package generator;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import generator.algorithms.EntryMerger;
import generator.algorithms.ExpectedTimeGenerator;
import generator.algorithms.TripMissingEntriesGenerator;
import generator.algorithms.TripRouteAssociator;
import generator.algorithms.TripsExtractor;
import generator.configuration.PostgisConfig;
import generator.models.A;
import generator.models.B;
import generator.models.EstruturaHumberto;
import generator.models.Route;
import generator.models.Viagem;
import generator.services.GTFSService;
import generator.services.QueryExecutor;
import generator.services.RTService;
import generator.utils.DateUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App7 {
	
	public record RouteTripRecord(Route r, List<Viagem> t) {}
	
	@SneakyThrows
	public static void main(String[] args) {
		var config = new PostgisConfig("jdbc:postgresql://localhost:15432/bh", "bh", "bh");
		var queryExecutor = new QueryExecutor(config.getConn());
		
		var tripExtractor = new TripsExtractor();
		var tripMissingEntriesGenerator = new TripMissingEntriesGenerator(new EntryMerger());
		var tripRouteAssociator = new TripRouteAssociator(tripMissingEntriesGenerator);
		var expectedTimeGenerator = new ExpectedTimeGenerator();
		
		var gtfsService = new GTFSService(queryExecutor);
		var rtService = new RTService(queryExecutor);
		
		var mapper = new ObjectMapper();
		
		var headsign = "9202";
		
		List<Route> expectedTimeTable = gtfsService.getExpectedTimeTable(headsign);
		
		mapper.writeValue(new File("./9202_ex.json"), expectedTimeTable);
		
		var routes = gtfsService.getRoutesHeadsign(headsign);
		var stopsIntervals = routes.keySet()
			.parallelStream()
			.collect(Collectors.toMap(Function.identity(), 
					gtfsService::getStopPointsInterval));
		
		var collect = List.of("2022-12-14"
//				,"2022-12-15", "2022-12-16", "2022-12-19", "2022-12-20"
				)
			.parallelStream()
			.collect(Collectors.toMap(d -> d, date -> {
				var entriesbyBusLine = rtService.getEntriesbyBusLine2(headsign, date);
				
				var t = entriesbyBusLine
						.values()
						.stream()
						.parallel()
						.map(tripExtractor::extract)
						.sequential()
						.flatMap(List<Viagem>::stream)
						.sorted((o1, o2) -> o1.getHorarioPartida().compareTo(o2.getHorarioPartida()))
						.map(trip -> tripRouteAssociator.associate(trip, routes.get(trip.getIdLinha())))
						.filter(trip -> !trip.getPartida().equals(trip.getChegada()))
						.filter(Viagem::isValid)
						.toList()
						;
				
				Function<LocalDateTime, List<Viagem>> filterTrips = d -> t
						.stream()
						.filter(ta -> {
							var w = ta.getHorarioPartida()
							.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
							.toLocalTime();
							
							long between = ChronoUnit.MINUTES.between(d.toLocalTime(), w);
							
							return Math.abs(between) <= 5;
						})
						.toList();
				
				log.info("t [{}] no dia {}", t.size(), date);
				
				
				return expectedTimeTable
					.stream()
					.map(route -> new RouteTripRecord(route, 
							new ArrayList<>(filterTrips.apply(route.getRouteDepartureTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()))))
					.map(o -> {
						o.t.removeIf(tc -> (tc.getDistanciaPercorrida() / o.r.getLength()) < 0.5d);
						return o;
					})
					.map(r -> expectedTimeGenerator.generate(r, stopsIntervals))
					.toList()
				;
			})
			);
		
		var fd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		var d = new EstruturaHumberto("9202");
		
		collect.forEach((k,v) -> {
			
			var lA = new ArrayList<A>();
			
			for (int i = 0; i < v.size(); i++) {
				var routeTrip = v.get(i);
				
				var route = routeTrip.r();
				var a = new A(i + 1, route);
				
				var b = routeTrip.t()
						.stream()
						.findFirst()
						.map(Viagem::getPontos)
						.orElse(List.of())
						.stream()
						.map(p -> B.builder()
								.stop(p)
								.timestampExpected(p.getTimestampExpected())
								.timestampReal(p.getArrivalTime())
								.idVehicle(p.getRegistros().get(0).getNumeroOrdemVeiculo())
								.build())
						.toList();
				a.setTrip(b);
				lA.add(a);
			}
			
			d.getTripsPerWeekday()
				.put(LocalDate.parse(k, fd).getDayOfWeek().name(), lA);
			
			try {
				mapper.writeValue(new File("./9202.json"), d);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
		});
		
		config.close();
	}

}
