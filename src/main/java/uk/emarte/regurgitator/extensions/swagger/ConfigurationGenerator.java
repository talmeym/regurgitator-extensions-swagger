package uk.emarte.regurgitator.extensions.swagger;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class ConfigurationGenerator {
    public static void main(String[] args) {
        SwaggerParseResult result = new OpenAPIParser().readLocation("test.yaml", null, null);

        OpenAPI openAPI = result.getOpenAPI();

        Paths paths = openAPI.getPaths();

        for(String path: paths.keySet()) {
            PathItem pathItem = paths.get(path);

            Operation get = pathItem.getGet();

            if(get != null) {
                ApiResponses responses = get.getResponses();

                for(String code: responses.keySet()) {
                    System.out.println(get.getDescription());
                    System.out.println("get " + path + " return " + code + " " + responses.get(code).getDescription());
                }
            }
        }
    }
}
