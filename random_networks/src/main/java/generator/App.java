package generator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

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
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.TrackSegment;
import io.jenetics.jpx.WayPoint;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
	
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
		
		final Function<RegistroViagem, WayPoint> registro2Waypoint = p ->
		WayPoint.builder()
			.lat(p.getCoord().getX())
			.lon(p.getCoord().getY())
			.build();
		
		var routesRrecords = Stream.of("9202")
		.parallel()
		.map(headsign -> new RouteRecord(headsign, 
				gtfsService.getRouteFromBusHeadsign(headsign),
				gtfsService.getRouteLength(headsign)))
		.toList();
		
		List.of("2022-12-14")
			.parallelStream()
			.forEach(date -> {
				log.info("{} entries", date);
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
							.toList()
							;
					t.forEach(trip -> tripRouteAssociator.associate(trip, entry.route()));
					
					return new LineTripRecord(entry.headsign(), t);
				})
				.forEach(lineTripRecord -> {
					var builder = GPX.builder();
					var trip = lineTripRecord.trips.get(1);
					
					var a = trip.getPontos().stream().map(PontoRota::getRegistros)
						.flatMap(List<RegistroViagem>::stream)
						.map(registro2Waypoint::apply)
						.toList();
					a.forEach(builder::addWayPoint);


					GPX gpx = builder.build();

					try {
						GPX.write(gpx, Paths.get("./pontos_%s_%s.gpx".formatted(lineTripRecord.line, date)));
						
						var builder2 = GPX.builder();
						var b = trip.getRegistros().stream()
								.map(registro2Waypoint::apply).toList();
						b.forEach(builder2::addWayPoint);
						GPX gpx2 = builder2.build();
						GPX.write(gpx2, Paths.get("./registros_%s_%s.gpx".formatted(lineTripRecord.line, date)));
					} catch (IOException e) {
						e.printStackTrace();
					}
						});
				}
			);
		
		
		config.close();
	}

}
