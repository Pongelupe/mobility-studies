package pbhimporter.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BaseResult<T> {

	@JsonProperty("include_total")
	private boolean includeTotal;

	@JsonProperty("resource_id")
	private String resourceId;
	
	private List<ResultField> fields;

	@JsonProperty("records_format")
	private String recordsFormat;

	private List<T> records;
	
	@JsonProperty("_links")
	private ResultLinks links;
	
	private int total;

}
