package generator;

import java.io.File;
import java.util.function.BinaryOperator;

import com.fasterxml.jackson.databind.ObjectMapper;

import generator.configuration.PostgisConfig;
import generator.services.QueryExecutor;
import lombok.SneakyThrows;

public class ExportStopsCoords {

	private static final String QUERY_STOPS = "select s.stop_id, st_x(stop_loc::geometry), st_y(stop_loc::geometry) from stops s";
	
	private record StopIdCoord(String stopId, String coord) {}
	
	@SneakyThrows
	public static void main(String[] args) {
		var config = new PostgisConfig("jdbc:postgresql://localhost:5432/pondionstracker", "pondionstracker", "pondionstracker");
		var queryExecutor = new QueryExecutor(config.getConn());
		
		var mapper = new ObjectMapper();
		
		BinaryOperator<String> buildCoords = (x, y) -> x + ", " + y;
		
		var stops = queryExecutor
				.queryAll(QUERY_STOPS, rs -> new StopIdCoord(rs.getString(1), 
						buildCoords.apply(rs.getString(3), rs.getString(2))));
		
		System.out.println(stops.size());
		
		mapper.writeValue(new File("stops_with_coord.json"), stops);
		
		config.close();
	}
	
}
