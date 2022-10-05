package pbhimporter.services;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import pbhimporter.components.PBHPostgisFieldResolver;
import pbhimporter.model.BasePbhResponse;
import pbhimporter.model.BaseResult;

@RequiredArgsConstructor
public class PostgisService {
	
	private final Connection conn;
	
	private final PBHPostgisFieldResolver pbhPostgisFieldResolver = new PBHPostgisFieldResolver();

	
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
		var records = (List<T>) response.getResult().getRecords();
		
		records
			.forEach(r -> addInsertToBatch(s, r, clazz));
		
		var result = s.executeBatch();
		
		s.close();
		
		return result.length;
	}
	
	@SneakyThrows
	private <T> void addInsertToBatch(Statement s, T r, Class<T> clazz) {
		var insertQuery = getSQLInsert(r, clazz);
		s.addBatch(insertQuery);
	}

	private <T> String getSQLInsert(T record, Class<T> clazz) {
		var sb = new StringBuilder("INSERT INTO ");
		sb.append(clazz.getSimpleName());
		
		var fields = getClazzAccesibleFields(clazz);
		var values = pbhPostgisFieldResolver.resolveFieldsWithoutTypeSQL(fields)
			.stream()
			.collect(Collectors.joining(", ", " (", " ) VALUES "));
		sb.append(values);
		
		sb.append(getRow(record, fields));
		return sb.toString();
	}

	private <T> String getRow(T obj, List<Field> fields) {
		return fields
				.stream()
				.map(f -> Optional.ofNullable(getFieldValue(obj, f))
						.map(value -> "'"
								+ value.toString().replace("'","") + "'")
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
