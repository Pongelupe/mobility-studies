package generator.models;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.postgis.jdbc.geometry.Point;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PontoRota {

	private int sequenciaPonto;
	
	private String idPonto;
	
	private Point coord;
	
	@Builder.Default
	private List<RegistroViagem> registros =  new ArrayList<>();
	
	
	public PontoRota(PontoRota ponto) {
		this.sequenciaPonto = ponto.getSequenciaPonto();
		this.idPonto = ponto.getIdPonto();
		this.coord = ponto.getCoord();
		this.registros = new ArrayList<>(ponto.getRegistros());
	}
	
}
