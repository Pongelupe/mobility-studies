package pbhimporter.model.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PontoOnibus {

	@JsonProperty("_id")
	private int id;
	
	@JsonProperty("ID_PONTO_ONIBUS_LINHA")
	private String pontoLinhaOnibus;

	@JsonProperty("COD_LINHA")
	private String codLinha;
	
	@JsonProperty("NOME_LINHA")
	private String nomeLinha;
	
	@JsonProperty("NOME_SUB_LINHA")
	private String nomeSubLinha;

	@JsonProperty("ORIGEM")
	private String origem;

	@JsonProperty("IDENTIFICADOR_PONTO_ONIBUS")
	private String identificadorPontoOnibus;
	
	@JsonProperty("GEOMETRIA")
	private String geometria;
	
}
