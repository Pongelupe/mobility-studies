package pbhimporter.components;

import java.lang.reflect.Field;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import net.postgis.jdbc.geometry.GeometryBuilder;

@NoArgsConstructor
public class PBHPostgisFieldResolver {
	
	@SneakyThrows
	public String resolveSQL(Field f, Object o) {
		
		var name = Optional.ofNullable(f.getAnnotation(JsonProperty.class))
			.map(JsonProperty::value)
			.orElse(f.getName());
		
		var type = getType(f, o);
		
		return name.concat(type);
	}

	@SneakyThrows
	private String getType(Field f, Object o) {
		var type = "int PRIMARY KEY";
		
		var fieldType = f.getType();
		
		if (!fieldType.isPrimitive()) {
			if (f.getName().equalsIgnoreCase("GEOMETRIA")) {
				type = GeometryBuilder.geomFromString(f.get(o).toString(), false).getTypeString();
			} else {
				//STRING
				type = "varchar (70)";
			}
		}
		
		return " ".concat(type);
	}

}
