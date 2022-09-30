package pbhimporter.resources;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pbhimporter.model.BasePbhResponse;
import pbhimporter.model.resources.PontoOnibus;
import pbhimporter.model.resources.TrechoCirculacaoViaria;

@Getter
@RequiredArgsConstructor
public enum ResourcesPBH {

	PONTO_ONIBUS("81b01e71-b237-467e-b607-f75073b412d4", new TypeReference<BasePbhResponse<PontoOnibus>>() {}),
	TRECHO_CIRCULACAO_VIARIA("1525da2a-5539-4a1d-8f69-5be2daf297c1", new TypeReference<BasePbhResponse<TrechoCirculacaoViaria>>() {});

	@Getter
	private final String resourceId;
	
	@SuppressWarnings("rawtypes")
	private final TypeReference responseTypeReference;

}