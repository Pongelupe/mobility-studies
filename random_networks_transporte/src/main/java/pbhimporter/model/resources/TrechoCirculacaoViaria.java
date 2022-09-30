package pbhimporter.model.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TrechoCirculacaoViaria {

	@JsonProperty("_id")
	private String id;

	@JsonProperty("ID_TCV")
	private String idTCV;

	@JsonProperty("TIPO_TRECHO_CIRCULACAO")
	private String tipoTrechoCirculacao;

	@JsonProperty("TIPO_LOGRADOURO")
	private String tipoLougradouro;
	
	@JsonProperty("LOGRADOURO")
	private String lougradouro;
	
	@JsonProperty("ID_NO_CIRC_INICIAL")
	private String idNoCircInicial;
	
	@JsonProperty("ID_NO_CIRC_FINAL")
	private String idNoCircFinal;

	@JsonProperty("GEOMETRIA")
	private String geometria;
	
}
