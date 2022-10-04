package pbhimporter.services;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.postgis.jdbc.geometry.Point;
import pbhimporter.components.PBHPostgisFieldResolver;
import pbhimporter.model.BasePbhResponse;
import pbhimporter.model.BaseResult;

@RequiredArgsConstructor
public class PostgisService {

	private final Connection conn;
	
	private final PBHPostgisFieldResolver pbhPostgisFieldResolver = new PBHPostgisFieldResolver();

	
	@SneakyThrows
	public Point pointFromEWKT(String ewkt) {
		return new Point(ewkt);
	}
	
	@SneakyThrows
	public <T> String createDataset(BasePbhResponse<? extends BaseResult<?>> response, Class<T> clazz) {
		var s = conn.createStatement();
		
		@SuppressWarnings("unchecked")
		T obj = (T) response.getResult().getRecords().get(0);
		
		var sqlCreateTable = getSQLCreateTable(clazz, obj);
		
		System.out.println("criando a table " + clazz.getSimpleName());
		System.out.println(sqlCreateTable);
		
		s.execute(sqlCreateTable);
		
		s.close();
		
		return sqlCreateTable;
	}
	
	@SneakyThrows
	public <T> long insertRecords(BasePbhResponse<? extends BaseResult<?>> response, Class<T> clazz) {
		var s = conn.createStatement();
		
		@SuppressWarnings("unchecked")
		var insertQuery = getSQLInsert((List<T>) response.getResult().getRecords(), clazz);
		
		s.addBatch(insertQuery);
		
		var result = s.executeBatch();
		
		s.close();
		
		return result.length;
	}
	

	private <T> String getSQLInsert(List<T> records, Class<T> clazz) {
		var sb = new StringBuilder("INSERT INTO ");
		sb.append(clazz.getSimpleName());
		
		var fields = getClazzAccesibleFields(clazz);
		var values = pbhPostgisFieldResolver.resolveFieldsWithoutTypeSQL(fields)
			.stream()
			.collect(Collectors.joining(", ", " (", " ) VALUES "));
		sb.append(values);
		
		var rows = records
			.stream()
			.map(obj -> getRow(obj, fields))
			.collect(Collectors.joining(", ", " (", " )"));
		
		sb.append(rows);
		return sb.toString();
	}

	private <T> String getRow(T obj, List<Field> fields) {
		return fields
				.stream()
				.map(f -> Optional.ofNullable(getFieldValue(obj, f)).map(Object::toString)
						.orElse("null"))
				.collect(Collectors.joining(", ", " (", " )"));
	}

	@SneakyThrows
	private Object getFieldValue(Object obj, Field f) {
		return f.get(obj);
	}

	private String getSQLCreateTable(Class<?> clazz, Object o) {
		var sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
		sb.append(clazz.getSimpleName());
		sb.append(" ( ");
		
		var fields = getClazzAccesibleFields(clazz);
			
		var tableBody = fields
			.stream()
			.map(field -> pbhPostgisFieldResolver.resolveFieldTypeSQL(field, o))
			.collect(Collectors.joining(", "));
		
		sb.append(tableBody);
		
		sb.append(" )");
		
		return sb.toString();
	}

	private List<Field> getClazzAccesibleFields(Class<?> clazz) {
		return Stream.of(clazz.getDeclaredFields())
			.map(this::makeFieldAccessible)
			.collect(Collectors.toList());
	}
	
	@SneakyThrows
	private Field makeFieldAccessible(Field f) {
		f.setAccessible(true);
		return f;
	}
	
	
}
