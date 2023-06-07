package generator.models;

import java.util.Optional;

public enum BusStatusAtStop {

	DELAYED, ON_TIME, AHEAD_OF_SCHEDULE, UNKOWN;
	
	public static BusStatusAtStop ofDiffTimestamp(Long diff) {
		return Optional.ofNullable(diff)
				.map(d -> {
					BusStatusAtStop v;
					if (d.intValue() == 0 ) {
						v = ON_TIME;
					} else if (d.intValue() > 0) {
						v = AHEAD_OF_SCHEDULE;
					} else {
						v = DELAYED;
					}
					
					return v;
				})
				.orElse(UNKOWN)
				;
	}
	
}
