package pbhimporter.model;

import lombok.Data;

@Data
public class BasePbhResponse<T> {

	private String help;
	
	private boolean success;
	
	private BaseResult<T> result;
	
}
