package generator.models;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.postgis.jdbc.geometry.Point;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class RegistroViagem {

	private Date dataHora;
	
	private int distanciaPercorrida;
	
	@JsonIgnore
	private Point coord;
	
	private int numeroOrdemVeiculo;
	
	private int velocidadeInstantanea;

	private int idLinha;
	
	@JsonIgnore
	private int index;
	
}
