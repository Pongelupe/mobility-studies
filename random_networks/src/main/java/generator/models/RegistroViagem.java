package generator.models;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.postgis.jdbc.geometry.Point;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegistroViagem {

	private Date dataHora;
	
	private int distanciaPercorrida;
	
	private Point coord;
	
	private int numeroOrdemVeiculo;
	
	private int velocidadeInstantanea;

	
}
