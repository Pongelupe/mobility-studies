package generator.services;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import generator.models.PontoRota;
import generator.models.Route;
import lombok.RequiredArgsConstructor;
import net.postgis.jdbc.PGgeometry;
import net.postgis.jdbc.geometry.LineString;
import net.postgis.jdbc.geometry.Point;

@RequiredArgsConstructor
public class GTFSService {
	
	private record RouteRecord(Integer routeId, PontoRota route) {}

	private static final UnaryOperator<String> QUERY_ROUTES = """
			select st.stop_sequence, s.stop_id, ST_SetSRID(ST_MakePoint(ST_Y(s.the_geom),ST_X(s.the_geom)), 4326) from gtfs.stop_times st
			join gtfs.stops s on s.stop_id = st.stop_id
			where st.trip_id = '%s' order by 1
			"""::formatted;
	
	private static final UnaryOperator<String> QUERY_EXPECT_TIMETABLE = """
			with timetable as (
				select st.timepoint, r.route_id, r.route_short_name, r.route_long_name, 
				st.trip_id, st.stop_sequence, arrival_time, sg.length, sg.the_geom 
				from gtfs.stop_times st 
					join gtfs.trips p on p.trip_id = st.trip_id 
					join gtfs.shape_geoms sg on sg.shape_id = p.shape_id 
					join gtfs.routes r on r.route_id = p.route_id 
				where st.arrival_time is not null)
			select distinct t.route_id, t.route_short_name, t.route_long_name, 
			(select arrival_time from gtfs.stop_times t1 where t1.feed_index = 1 and t1.trip_id = t.trip_id and t1.stop_sequence = 1) as route_departure_time, 
			t.arrival_time as route_arrival_time, t.length, t.the_geom 
			from timetable t 
			where t.timepoint  = 0 and t.route_short_name = '%s' order by route_departure_time"""::formatted;

	private static final UnaryOperator<String> QUERY_ROUTES_HEADSIGN = """
			select st.stop_sequence, s.stop_id, ST_SetSRID(ST_MakePoint(ST_Y(s.the_geom),ST_X(s.the_geom)), 4326) from gtfs.stop_times st
			join gtfs.stops s on s.stop_id = st.stop_id
			join gtfs.trips t  on st.trip_id = t.trip_id
			where st.trip_id = (select trip_id from gtfs.trips t where route_id like '%s%%' limit 1)
			order by 1
			"""::formatted;

	private static final UnaryOperator<String> QUERY_ROUTE_LENGTH = """
			select sg.length from gtfs.trips t
			join gtfs.shape_geoms sg on sg.shape_id = t.shape_id
			where t.route_id like '%s%%' limit 1
			"""::formatted;

	private static final UnaryOperator<String> QUERY_ROUTE = """
			with linhas as (select replace(codigo_linha, '-', '  ') as route_id, id_linha 
			from bh.bh.linha_onibus lo where codigo_linha like '%s%%')
			select distinct id_linha,  st.stop_sequence, s.stop_id, ST_SetSRID(ST_MakePoint(ST_Y(s.the_geom),ST_X(s.the_geom)), 4326) from gtfs.stop_times st 
			join gtfs.stops s on s.stop_id = st.stop_id
			join gtfs.trips t  on st.trip_id = t.trip_id
			join linhas l on l.route_id = t.route_id
			order by 1, 2
			"""::formatted;

	private final QueryExecutor queryExecutor;
	
	public List<Route> getExpectedTimeTable(String headsign) {
		return queryExecutor.queryAll(QUERY_EXPECT_TIMETABLE.apply(headsign), rs -> Route.builder()
				.routeId(rs.getString(1))
				.routeDepartureTime(rs.getTimestamp(4))
				.routeArrivalTime(rs.getTimestamp(5))
				.length(rs.getDouble(6))
				.geom((LineString) ((PGgeometry) rs.getObject(7)).getGeometry())
				.build());
	}

	public Map<Integer, List<PontoRota>> getRoutesHeadsign(String headsign) {
		return queryExecutor.queryAll(QUERY_ROUTE.apply(headsign), rs -> new RouteRecord(rs.getInt(1), PontoRota.builder()
				.sequenciaPonto(rs.getInt(2))
				.idPonto(rs.getString(3))
				.coord((Point) ((PGgeometry) rs.getObject(4)).getGeometry()).build()))
				.stream()
				.collect(Collectors.groupingBy(RouteRecord::routeId))
				.entrySet()
				.stream()
				.collect(Collectors.toMap(Entry<Integer, List<RouteRecord>>::getKey,
						e -> e.getValue()
						.stream()
						.map(s -> s.route())
						.toList()));
	}

	public List<PontoRota> getRouteFromTripId(String tripId) {
		return queryExecutor.queryAll(QUERY_ROUTES.apply(tripId), rs -> PontoRota.builder().sequenciaPonto(rs.getInt(1))
				.idPonto(rs.getString(2)).coord((Point) ((PGgeometry) rs.getObject(3)).getGeometry()).build());
	}

	public List<PontoRota> getRouteFromBusHeadsign(String headsign) {
		return queryExecutor.queryAll(QUERY_ROUTES_HEADSIGN.apply(headsign),
				rs -> PontoRota.builder().sequenciaPonto(rs.getInt(1)).idPonto(rs.getString(2))
						.coord((Point) ((PGgeometry) rs.getObject(3)).getGeometry()).build());
	}

	public Double getRouteLength(String headsign) {
		return queryExecutor.queryAll(QUERY_ROUTE_LENGTH.apply(headsign), rs -> rs.getDouble(1)).get(0);
	}

}
