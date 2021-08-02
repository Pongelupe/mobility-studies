package tremdas7.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

	private LocalDateTime departureTime;
	
	private LocalDateTime arrivalTime;
	
	private String expectedTimeTravel;
	
	private String expectedDistanceTravel;
	
	private BigDecimal fare;
	
	private String startAddress;

	private String endAddress;
	
}
