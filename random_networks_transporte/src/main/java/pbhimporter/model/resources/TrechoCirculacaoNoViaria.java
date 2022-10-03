package pbhimporter.model.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TrechoCirculacaoNoViaria {

	@JsonProperty("_id")
	private String id;

	@JsonProperty("ID_NTCV")
	private String idNTCV;

	@JsonProperty("GEOMETRIA")
	private String geometria;
	
}
