package generator;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import generator.algorithms.EntryMerger;
import generator.algorithms.TripMissingEntriesGenerator;
import generator.algorithms.TripRouteAssociator;
import generator.algorithms.TripsExtractor;
import generator.configuration.PostgisConfig;
import generator.models.PontoRota;
import generator.models.RegistroViagem;
import generator.models.Viagem;
import generator.services.GTFSService;
import generator.services.QueryExecutor;
import generator.services.RTService;
import lombok.SneakyThrows;

public class App3 {
	
	record LineTripRecord(String line, List<Viagem> trips) {}

	record RouteRecord(String headsign, List<PontoRota> route, Double length) {}
	
	@SneakyThrows
	public static void main(String... args) {
		var config = new PostgisConfig("jdbc:postgresql://localhost:15432/bh", "bh", "bh");
		var queryExecutor = new QueryExecutor(config.getConn());
		
		var tripExtractor = new TripsExtractor();
		var tripMissingEntriesGenerator = new TripMissingEntriesGenerator(new EntryMerger());
		var tripRouteAssociator = new TripRouteAssociator(tripMissingEntriesGenerator);
		
		var gtfsService = new GTFSService(queryExecutor);
		var rtService = new RTService(queryExecutor);
		
		var routesRrecords = Stream.of("9202"
				, "9204", "8208", "8203", "4150", "2103", "9210", "2104", "2102", "2101"
				)
		.parallel()
		.map(headsign -> new RouteRecord(headsign, 
				gtfsService.getRouteFromBusHeadsign(headsign),
				gtfsService.getRouteLength(headsign)))
		.toList();
		
		Map<String, List<LineTripRecord>> tripsPerWeekdays = List.of("2022-12-14", "2022-12-15", "2022-12-16", "2022-12-19", "2022-12-20")
			.parallelStream()
			.collect(Collectors.toMap(d -> d, date ->
				routesRrecords
				.stream()
				.map(entry -> {
					var entriesbyBusLine = rtService.getEntriesbyBusLine(entry.headsign(), date);
					var t = entriesbyBusLine
							.entrySet()
							.stream()
							.parallel()
							.map(Entry<Integer, Map<Integer, List<RegistroViagem>>>::getValue)
							.map(e -> e.entrySet().stream().map(Entry<Integer, List<RegistroViagem>>::getValue).toList())
							.flatMap(List<List<RegistroViagem>>::stream)
							.map(tripExtractor::extract)
							.sequential()
							.flatMap(List<Viagem>::stream)
							.filter(Viagem::isViagemCompleta)
							.filter(v -> entry.length() * 0.7 <= v.getDistanciaPercorrida())
							.sorted((o1, o2) -> o1.getHorarioPartida().compareTo(o2.getHorarioPartida()))
							.map(trip -> tripRouteAssociator.associate(trip, entry.route()))
							.filter(Viagem::isAnyNotCalculated)
							.toList()
							;
					
					return new LineTripRecord(entry.headsign(), t);
				})
				.toList()));
		
		var mapper = new ObjectMapper();
		mapper.writeValue(new File("./tripsPerWeekdays.json"), tripsPerWeekdays);
			
		
		config.close();
	}

}
