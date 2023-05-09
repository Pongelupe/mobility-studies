package generator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongBiFunction;
import java.util.stream.Collectors;

import generator.configuration.PostgisConfig;
import generator.models.PontoRota;
import generator.models.RegistroViagem;
import generator.models.Rota;
import generator.models.Viagem;
import generator.services.PostgisService;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Route;
import io.jenetics.jpx.WayPoint;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.postgis.jdbc.PGgeometry;
import net.postgis.jdbc.geometry.Point;

@Slf4j
public class App3 {
	
	private static final double DISTANCE_THRESHOLD = 0.0005d; // 50m
	

	@SneakyThrows
	public static void main(String... args) {
		var config = new PostgisConfig("jdbc:postgresql://localhost:15432/bh", "bh", "bh");
		var postgisService = new PostgisService(config.getConn());
		
		var queryRota = """
				with routes as(
select distinct  t.route_id  from gtfs.stop_times st 
join gtfs.trips t on t.trip_id = st.trip_id 
where st.stop_id = '00102112100323')
select st.trip_id, st.stop_sequence, s.stop_id, s.the_geom from gtfs.stop_times st
				join gtfs.stops s on s.stop_id = st.stop_id 
				where st.trip_id in (
select (select trip_id from gtfs.trips t where t.route_id = r.route_id limit 1) as a from routes r)
order by 1,2
				""";
		postgisService.queryAll(queryRota, rs ->
					Rota.builder()
					.tripId(rs.getString(1))
					.stopSequence(rs.getInt(2))
					.stopId(rs.getString(3))
					.coord((Point) ((PGgeometry) rs.getObject(4)).getGeometry())
					.build()
				)
				.stream()
				.collect(Collectors.groupingBy(Rota::getTripId))
				.entrySet()
				.stream()
				.collect(Collectors.toMap(Entry<String, List<Rota>>::getKey, e -> Route.of(e
						.getValue()
						.stream()
						.map(p -> WayPoint.builder()
								.lat(p.getCoord().getY())
								.lon(p.getCoord().getX())
								.build())
						.toList())))
				.forEach((k, v) -> {
					var gpx = GPX.builder()
							.addRoute(v)
							.build();
					try {
						GPX.write(gpx, Paths.get("./rotas_ponto_00102112100323/" + k.replace(" ", "_") + ".gpx"));
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				});
		
		
		config.close();
	}
	
}
