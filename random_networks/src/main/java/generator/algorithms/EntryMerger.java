package generator.algorithms;

import java.util.Date;
import java.util.Optional;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongBiFunction;

import generator.models.RegistroViagem;
import lombok.RequiredArgsConstructor;
import net.postgis.jdbc.geometry.Point;

@RequiredArgsConstructor
public class EntryMerger {

	public RegistroViagem merge(RegistroViagem registroAtual, RegistroViagem registroSeguinte) {
		
		IntBinaryOperator avg = (o1, o2) -> (o1 + o2) / 2;
		
		ToDoubleFunction<RegistroViagem> getXCoord = o1 -> o1.getCoord().getX();
		ToDoubleFunction<RegistroViagem> getYCoord = o1 -> o1.getCoord().getY();
		DoubleBinaryOperator avgDouble = (o1, o2) -> (o1 + o2) / 2;
		ToLongBiFunction<Date, Date> avgDate = (o1, o2) -> (o1.getTime() + o2.getTime()) / 2;
		
		return Optional.ofNullable(registroSeguinte)
				.map(r -> {
					var point = new Point(avgDouble.applyAsDouble(getXCoord.applyAsDouble(registroAtual), getXCoord.applyAsDouble(registroSeguinte)), 
							avgDouble.applyAsDouble(getYCoord.applyAsDouble(registroAtual), getYCoord.applyAsDouble(registroSeguinte)),
							r.getCoord().getZ());
					point.dimension = r.getCoord().dimension;
					return RegistroViagem
							.builder()
							.distanciaPercorrida(avg.applyAsInt(registroAtual.getDistanciaPercorrida(), 
									registroSeguinte.getDistanciaPercorrida()))
							.dataHora(new Date(avgDate.applyAsLong(registroAtual.getDataHora(), registroSeguinte.getDataHora())))
							.coord(point)
							.numeroOrdemVeiculo(r.getNumeroOrdemVeiculo())
							.velocidadeInstantanea(avg.applyAsInt(registroAtual.getVelocidadeInstantanea(), 
									registroSeguinte.getVelocidadeInstantanea()))
							.idLinha(r.getIdLinha())
							.index(-1)
						.build();
				})
				.orElse(registroAtual);
	}
	
}
