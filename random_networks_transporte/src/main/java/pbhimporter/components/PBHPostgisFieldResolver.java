package pbhimporter.components;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor
public class PBHPostgisFieldResolver {
	
	
	public List<String> resolveFieldsWithoutTypeSQL(List<Field> fields) {
		return fields
				.stream()
				.map(this::getFieldName)
				.collect(Collectors.toList());
	}
	
	@SneakyThrows
	public String resolveFieldTypeSQL(Field f, Object o) {
		
		var name = getFieldName(f);
		
		var type = getType(f, o);
		
		return name.concat(type);
	}

	private String getFieldName(Field f) {
		return Optional.ofNullable(f.getAnnotation(JsonProperty.class))
			.map(JsonProperty::value)
			.orElse(f.getName());
	}

	@SneakyThrows
	private String getType(Field f, Object o) {
		var type = "int PRIMARY KEY";
		
		var fieldType = f.getType();
		
		if (!fieldType.isPrimitive()) {
			if (f.getName().equalsIgnoreCase("GEOMETRIA")) {
				type = "  geometry";
			} else {
				//STRING
				type = "varchar (70)";
			}
		}
		
		return " ".concat(type);
	}

}
