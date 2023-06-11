package generator.algorithms;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import generator.App7.RouteTripRecord;
import generator.models.StopPointsInterval;
import generator.utils.DateUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExpectedTimeGenerator {
	
	
	public RouteTripRecord generate(RouteTripRecord r, Map<Integer, List<StopPointsInterval>> stopsIntervals) {
		r.t().stream().findFirst()
			.ifPresent(trip -> {
				var route = r.r();
				var departure = DateUtils.date2localtime(route.getRouteDepartureTime());
				var arrivalTime = DateUtils.date2localtime(route.getRouteArrivalTime());
				
				var routeDuration = ChronoUnit.MINUTES.between(departure, arrivalTime);
				var avgSpeed = (route.getLength() / 1000d) / (routeDuration / 60d);
				
				var intervals = stopsIntervals.get(trip.getIdLinha())
						.stream()
						.collect(Collectors.toMap(StopPointsInterval::getStopSequence2, Function.identity()));
				
				trip.getPontos().get(0).setTimestampExpected(DateUtils.dateFromLocalTime(trip.getHorarioPartida(), departure));
				Optional.ofNullable(trip.getHorarioChegada())
					.ifPresent(d -> trip.getPontos().get(trip.getPontos().size() - 1)
							.setTimestampExpected(DateUtils.dateFromLocalTime(d, arrivalTime)));
				
				var expectedTime = departure;
				
				for (int i = 1; i < trip.getPontos().size() - 1; i++) {
					var ponto = trip.getPontos().get(i);
					
					if (intervals.containsKey(ponto.getSequenciaPonto())) {
						var distanceTillThisStop = intervals.get(ponto.getSequenciaPonto())
							.getLength();
						
						long expectedTimeDiff = BigDecimal.valueOf(((distanceTillThisStop * 100 / avgSpeed) * 60 * 60))
								.setScale(2, RoundingMode.UP)
							.longValue();
						expectedTime = expectedTime.plusSeconds(expectedTimeDiff);
						ponto.setTimestampExpected(DateUtils.dateFromLocalTime(trip.getHorarioPartida(), expectedTime));
					}
					
				}
				
			});
		
		
		return r;
	}

}
