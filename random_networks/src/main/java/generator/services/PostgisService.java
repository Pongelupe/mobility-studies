package generator.services;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import generator.models.RowMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class PostgisService {

	private final Connection conn;
	
	@SneakyThrows
	public <T> List<T> queryAll(String query, RowMapper<T> mapper) {
		var result = new ArrayList<T>();
		var stmt = conn.createStatement();
		
		var rs = stmt.executeQuery(query);
		
		while(rs.next()) {
			result.add(mapper.mapRow(rs));
		}
		
		stmt.close();
		return result;
	}
	
}
