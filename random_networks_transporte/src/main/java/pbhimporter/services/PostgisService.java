package pbhimporter.services;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.postgis.jdbc.geometry.Point;
import pbhimporter.components.PBHPostgisFieldResolver;
import pbhimporter.model.BasePbhResponse;

@RequiredArgsConstructor
public class PostgisService {

	private final Connection conn;
	
	private final PBHPostgisFieldResolver pbhPostgisFieldResolver = new PBHPostgisFieldResolver();

	
	@SneakyThrows
	public Point pointFromEWKT(String ewkt) {
		return new Point(ewkt);
	}
	
	@SneakyThrows
	public <T> long createDataset(BasePbhResponse<T> response, Class<T> clazz) {
		var s = conn.createStatement();
		
		T obj = response.getResult().getRecords().get(0);
		
		var sqlCreateTable = getSQLCreateTable(clazz, obj);
		
		System.out.println("criando a table " + clazz.getSimpleName());
		
		s.execute(sqlCreateTable);
		
		s.close();
		
		return 0l;
	}
	

	private String getSQLCreateTable(Class<?> clazz, Object o) {
		var sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
		sb.append(clazz.getSimpleName());
		sb.append(" ( ");
		
		var fields = Stream.of(clazz.getDeclaredFields())
			.map(this::makeFieldAccessible)
			.collect(Collectors.toList());
			
		var tableBody = fields
			.stream()
			.map(field -> pbhPostgisFieldResolver.resolveSQL(field, o))
			.collect(Collectors.joining(", "));
		
		sb.append(tableBody);
		
		sb.append(" )");
		
		return sb.toString();
	}
	
	@SneakyThrows
	private Field makeFieldAccessible(Field f) {
		f.setAccessible(true);
		return f;
	}
	
	
}
