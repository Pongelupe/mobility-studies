package generator.services;

import java.util.List;
import java.util.function.UnaryOperator;

import generator.models.PontoRota;
import lombok.RequiredArgsConstructor;
import net.postgis.jdbc.PGgeometry;
import net.postgis.jdbc.geometry.Point;

@RequiredArgsConstructor
public class GTFSService {

	private static final UnaryOperator<String> QUERY_ROUTES = """
			select st.stop_sequence, s.stop_id, ST_SetSRID(ST_MakePoint(ST_Y(s.the_geom),ST_X(s.the_geom)), 4326) from gtfs.stop_times st
			join gtfs.stops s on s.stop_id = st.stop_id
			where st.trip_id = '%s' order by 1
			"""::formatted;
	
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

	private final QueryExecutor queryExecutor;

	public List<PontoRota> getRouteFromTripId(String tripId) {
		return queryExecutor.queryAll(QUERY_ROUTES.apply(tripId), rs -> PontoRota
				.builder()
				.sequenciaPonto(rs.getInt(1))
				.idPonto(rs.getString(2))
				.coord((Point) ((PGgeometry) rs.getObject(3)).getGeometry())
				.build());
	}
	
	public List<PontoRota> getRouteFromBusHeadsign(String headsign) {
		return queryExecutor.queryAll(QUERY_ROUTES_HEADSIGN.apply(headsign), rs -> PontoRota
				.builder()
				.sequenciaPonto(rs.getInt(1))
				.idPonto(rs.getString(2))
				.coord((Point) ((PGgeometry) rs.getObject(3)).getGeometry())
				.build());
	}
	
	public Double getRouteLength(String headsign) {
		return queryExecutor.queryAll(QUERY_ROUTE_LENGTH.apply(headsign), rs -> rs.getDouble(1))
				.get(0);
	}

}
