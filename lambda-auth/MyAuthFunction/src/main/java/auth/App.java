package auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String API_GATEWAY_BACKEND_URL = "https://ro4ghw5iqe.execute-api.us-east-2.amazonaws.com/prod/api/v1/person/cpf";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);

        try {
            // Obter o cabeçalho Authorization
            String authHeader = input.getHeaders().get("Authorization");

            // Verificar se o cabeçalho Authorization está presente
            if (authHeader == null || authHeader.isEmpty()) {
                // Permitir acesso anônimo
                context.getLogger().log("Acesso anônimo permitido.");
                return response
                        .withStatusCode(200)
                        .withBody("{\"authorized\": true, \"message\": \"Acesso anônimo permitido.\"}");
            }

            // Validar cabeçalho Authorization
            if (!authHeader.startsWith("Basic ")) {
                return response
                        .withStatusCode(401)
                        .withBody("{\"authorized\": false, \"message\": \"Cabeçalho 'Authorization' inválido.\"}");
            }

            // Consultar o backend para validar as credenciais
            boolean isValid = consultaBackend(authHeader, context);

            if (!isValid) {
                return response
                        .withStatusCode(401)
                        .withBody("{\"authorized\": false, \"message\": \"CPF ou senha inválidos.\"}");
            }

            // Retornar sucesso
            return response
                    .withStatusCode(200)
                    .withBody("{\"authorized\": true, \"message\": \"Cliente autenticado com sucesso.\"}");

        } catch (Exception e) {
            context.getLogger().log("Erro inesperado: " + e.getMessage());
            return response
                    .withStatusCode(500)
                    .withBody("{\"message\": \"Erro interno no servidor.\"}");
        }
    }

    private boolean consultaBackend(String authHeader, Context context) {
        try {
            // Decodificar o cabeçalho Base64
            String credentials = new String(Base64.getDecoder().decode(authHeader.replace("Basic ", "")));
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                context.getLogger().log("Formato de credenciais inválido.");
                return false;
            }

            String cpf = parts[0];
            String senha = parts[1];

            // Construir URL com o CPF
            URL url = new URL(API_GATEWAY_BACKEND_URL + "?cpf=" + cpf);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");

            // Adicionar cabeçalho de autenticação Basic
            connection.setRequestProperty("Authorization", authHeader);

            // Ler código de resposta
            int responseCode = connection.getResponseCode();
            context.getLogger().log("Backend response code: " + responseCode);

            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                // Parsear JSON retornado pelo backend
                Map<String, Object> responseBody = objectMapper.readValue(response.toString(), Map.class);

                // Validar CPF e senha retornados
                String returnedCpf = (String) ((Map<String, Object>) ((java.util.List<Object>) responseBody.get("document")).get(0)).get("value");
                String returnedPassword = (String) responseBody.get("password");

                // Retornar verdadeiro se CPF e senha forem válidos
                return cpf.equals(returnedCpf) && senha.equals(returnedPassword);
            }
        } catch (Exception e) {
            context.getLogger().log("Erro ao consultar backend: " + e.getMessage());
        }
        return false;
    }
}
