package generator;

import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import generator.algorithms.EntryMerger;
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
		
		Function<Date, LocalDateTime> date2localdatetime = d -> d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
				;
		Function<Date, LocalTime> date2localtime = d -> date2localdatetime.apply(d)
				.toLocalTime();
		
		var mapper = new ObjectMapper();
		
		var headsign = "9202";
		
		List<Route> expectedTimeTable = gtfsService.getExpectedTimeTable(headsign);
		
		mapper.writeValue(new File("./9202_ex.json"), expectedTimeTable);
		
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
//						.filter(Viagem::isViagemCompleta)
						.sorted((o1, o2) -> o1.getHorarioPartida().compareTo(o2.getHorarioPartida()))
						.map(trip -> tripRouteAssociator.associate(trip, routes.get(trip.getIdLinha())))
						.filter(trip -> !trip.getPartida().equals(trip.getChegada()))
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
		
		var fd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		var d = new EstruturaHumberto("9202");
		
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
			
			var lA = new ArrayList<A>();
			
			for (int i = 0; i < v.size(); i++) {
				var routeTrip = v.get(i);
				
				var route = routeTrip.r();
				var a = new A(i + 1, route);
				
				var ii = new AtomicInteger(i);
				
				var b = routeTrip.t()
						.stream()
						.findFirst()
						.map(Viagem::getPontos)
						.orElse(List.of())
						.stream()
						.map(p -> B.builder()
								.stop(p)
								.timestampExpected(calculateTimestampExpected(route.getRouteDepartureTime(), 
										p.getSequenciaPonto(), ii.intValue(), v, date2localtime, date2localdatetime))
								.timestampReal(p.getArrivalTime())
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
			
//			v.forEach(routeTrip -> {
//				var route = routeTrip.r();
//				var trip = routeTrip.t();
//				
//				LocalTime horarioPrevistoPartida = date2localtime.apply(route.getRouteDepartureTime());
//				LocalTime horarioPrevistoChegada = date2localtime.apply(route.getRouteArrivalTime());
//				
//				log.info("distancia prevista: {}", route.getLength());
//				log.info("horario previsto: partida {} => chegada {}",
//						horarioPrevistoPartida,
//						horarioPrevistoChegada);
//				trip.forEach(t -> {
//					log.info("distancia real: {}", t.getDistanciaPercorrida());
//					log.info("distancia real/prevista: {}", df.format(t.getDistanciaPercorrida() / route.getLength()));
//					
//					LocalTime horarioRealPartida = t.getHorarioPartida() != null ? date2localtime.apply(t.getHorarioPartida()) : null;
//					LocalTime horarioRealChegada = t.getHorarioChegada() != null ? date2localtime.apply(t.getHorarioChegada()) : null;
//					
//					long diffPartida = horarioRealPartida != null ? ChronoUnit.MINUTES.between(horarioPrevistoPartida, horarioRealPartida) : 0;
//					long diffChegada = horarioRealChegada != null ? ChronoUnit.MINUTES.between(horarioPrevistoChegada, horarioRealChegada) : 0;
//					log.info("horario real: partida {} => chegada {} ", 
//							horarioRealPartida,
//							horarioRealChegada);
//					log.info("Diferença partida: {} minutos", diffPartida);
//					log.info("Diferença chegada: {} minutos", diffChegada);
//					log.info("Diferença total: {} minutos", diffPartida + diffChegada);
//				});
//				log.info("-----");
//				
//			});
			
		});
		
		config.close();
	}

	private static Date calculateTimestampExpected(Date routeDepartureTime, int sequenciaPonto, int i,
			List<RouteTripRecord> v, Function<Date, LocalTime> date2localtime, Function<Date, LocalDateTime> date2localdatetime) {

		var dtEx = date2localdatetime.apply(routeDepartureTime);
		var dtReal = date2localdatetime.apply(routeDepartureTime);
		var index = i - 1;
		long diff = 0 ;
		
		if (i == 0) {
			index = v.size() - 1;
			dtEx = date2localdatetime.apply(v.get(index).r().getRouteDepartureTime());
			dtReal = dtReal.plusDays(1);
		} else {
			dtReal = date2localdatetime.apply(v.get(index).r().getRouteDepartureTime());
		}
		
		if (sequenciaPonto > 1) {
			diff = ChronoUnit.MINUTES.between(dtEx, dtReal);
		}
		
		return Date.from(date2localdatetime.apply(routeDepartureTime)
				.plusMinutes(Math.abs(diff * (sequenciaPonto - 1)))
				.atZone(ZoneId.systemDefault())
				.toInstant());
	}

}
