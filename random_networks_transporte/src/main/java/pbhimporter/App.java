package pbhimporter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import pbhimporter.configuration.PostgisConfig;
import pbhimporter.model.BasePbhResponse;
import pbhimporter.model.BaseResult;
import pbhimporter.resources.ResourcesPBH;
import pbhimporter.services.PBHService;
import pbhimporter.services.PostgisService;

public class App {

	@SneakyThrows
	public static void main(String... args) {
		var resourcePBH = ResourcesPBH.valueOf(args[0]);
		
		ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
         var pbhService = new PBHService(mapper);
         
         BasePbhResponse<? extends BaseResult<?>> response = pbhService.getResource(resourcePBH);
         
         System.out.println("registros recuperados da API " + response.getResult().getRecords().size());
         
         var config = new PostgisConfig("jdbc:postgresql://localhost:15432/bh", "bh", "bh");
         
         var postgisService = new PostgisService(config.getConn());
         
         postgisService.createDataset(response, resourcePBH.getClazz());
         var linesInserted = postgisService.insertRecords(response, resourcePBH.getClazz());
         
         System.out.println("resgistros inseridos " + linesInserted);
         System.out.println("finalizando...");
         
         config.close();
         
	}

}
