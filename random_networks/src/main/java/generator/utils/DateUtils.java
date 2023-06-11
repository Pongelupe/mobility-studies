package generator.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DateUtils {
	
	public LocalDateTime date2localdatetime(Date d) {return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();}
	
	public LocalDate date2localdate(Date d) {return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();}
	
	public LocalTime date2localtime(Date d) { return date2localdatetime(d)
			.toLocalTime();}

	public Date dateFromLocalTime(Date d, LocalTime e) {
		var dt = date2localdate(d);
		return Date.from(e.atDate(dt).atZone(ZoneId.systemDefault()).toInstant());
	}
	
}
