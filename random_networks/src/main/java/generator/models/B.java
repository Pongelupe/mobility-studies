package generator.models;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class B {

	private PontoRota stop;
	
	@JsonFormat(pattern = "HH:mm:ss", timezone = "America/Sao_Paulo")
	private Date timestampExpected;
	
	@JsonFormat(pattern = "HH:mm:ss", timezone = "America/Sao_Paulo")
	private Date timestampReal;
	
	
	public Long getDiffTimestamp() {
		Function<Date, LocalTime> date2localtime = d -> d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
				.toLocalTime();
		BiFunction<Date, Date, Long> calcDiff = (ex, re) -> ChronoUnit.MINUTES.between(
				date2localtime.apply(ex), date2localtime.apply(re));
		
		var dates = List.of(timestampExpected, timestampReal)
				.stream()
				.filter(Objects::nonNull)
				.toList();
		return dates.size() != 2 ? null : calcDiff.apply(timestampReal, timestampExpected);
	}
	
	
	public BusStatusAtStop getStatus() {
		return BusStatusAtStop.ofDiffTimestamp(getDiffTimestamp());
	}
	
}
