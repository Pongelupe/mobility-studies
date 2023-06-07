package generator;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import generator.algorithms.EntryMerger;
import generator.algorithms.TripMissingEntriesGenerator;
import generator.algorithms.TripRouteAssociator;
import generator.algorithms.TripsExtractor;
import generator.configuration.PostgisConfig;
import generator.models.Route;
import generator.models.Viagem;
import generator.services.GTFSService;
import generator.services.QueryExecutor;
import generator.services.RTService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App6 {
	
	private record RouteTripRecord(Route r, List<Viagem> t) {}
	
	@SneakyThrows
	public static void main(String[] args) {
		var config = new PostgisConfig("jdbc:postgresql://localhost:15432/bh", "bh", "bh");
		var queryExecutor = new QueryExecutor(config.getConn());
		
		var tripExtractor = new TripsExtractor();
		var tripMissingEntriesGenerator = new TripMissingEntriesGenerator(new EntryMerger());
		var tripRouteAssociator = new TripRouteAssociator(tripMissingEntriesGenerator);
		
		var gtfsService = new GTFSService(queryExecutor);
		var rtService = new RTService(queryExecutor);
		
		Function<Date, LocalTime> date2localtime = d -> d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
				.toLocalTime();
		
		var headsign = "9202";
		
		List<Route> expectedTimeTable = gtfsService.getExpectedTimeTable(headsign);
		
		var routes = gtfsService.getRoutesHeadsign(headsign);
		
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
						.filter(Viagem::isViagemCompleta)
						.sorted((o1, o2) -> o1.getHorarioPartida().compareTo(o2.getHorarioPartida()))
						.map(trip -> tripRouteAssociator.associate(trip, routes.get(trip.getIdLinha())))
						.filter(trip -> !trip.getChegada().equals(trip.getPartida()))
//						.filter(Viagem::isAnyNotCalculated)
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
					.toList()
				;
			})
			);
		
		DecimalFormat df = new DecimalFormat("##.##%");
		DoubleFunction<String> formatPG = d -> df.format(d / expectedTimeTable.size());
		
		collect.forEach((k,v) -> {
			ToDoubleFunction<Predicate<RouteTripRecord>> count = p -> v
					.stream()
					.filter(p)
					.count()
					;
			
			double viagensRealizadas = count.applyAsDouble(e -> !e.t.isEmpty());
			
			log.info("dia {}", k);
			log.info("Viagens esperadas: {}", expectedTimeTable.size());
			log.info("Viagens realizadas: {}", viagensRealizadas);
			log.info("Viagens realizadas/esperadas: {}", formatPG.apply(viagensRealizadas));
			log.info("****");
			
			v.forEach(routeTrip -> {
				var route = routeTrip.r();
				var trip = routeTrip.t();
				
				LocalTime horarioPrevistoPartida = date2localtime.apply(route.getRouteDepartureTime());
				LocalTime horarioPrevistoChegada = date2localtime.apply(route.getRouteArrivalTime());
				
				log.info("distancia prevista: {}", route.getLength());
				log.info("horario previsto: partida {} => chegada {}",
						horarioPrevistoPartida,
						horarioPrevistoChegada);
				trip.forEach(t -> {
					log.info("distancia real: {}", t.getDistanciaPercorrida());
					log.info("distancia real/prevista: {}", df.format(t.getDistanciaPercorrida() / route.getLength()));
					
					LocalTime horarioRealPartida = date2localtime.apply(t.getHorarioPartida());
					LocalTime horarioRealChegada = date2localtime.apply(t.getHorarioChegada());
					
					long diffPartida = ChronoUnit.MINUTES.between(horarioPrevistoPartida, horarioRealPartida);
					long diffChegada = ChronoUnit.MINUTES.between(horarioPrevistoChegada, horarioRealChegada);
					log.info("horario real: partida {} => chegada {} ", 
							horarioRealPartida,
							horarioRealChegada);
					log.info("Diferença partida: {} minutos", diffPartida);
					log.info("Diferença chegada: {} minutos", diffChegada);
					log.info("Diferença total: {} minutos", diffPartida + diffChegada);
				});
				log.info("-----");
				
			});
		});
		
		config.close();
	}

}
