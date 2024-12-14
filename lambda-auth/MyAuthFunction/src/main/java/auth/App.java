package auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SECRET_KEY = System.getenv("SECRET_KEY");
    private static final String API_GATEWAY_BACKEND_URL = System.getenv("API_GATEWAY_BACKEND_URL");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);

        try {
            // Parse do corpo da requisição
            Map<String, String> body = objectMapper.readValue(input.getBody(), Map.class);

            // Extrair CPF e senha
            String cpf = body.get("cpf");
            String senha = body.get("senha");
            if (cpf == null || cpf.isEmpty() || senha == null || senha.isEmpty()) {
                context.getLogger().log("CPF ou senha não informados.");
                return response
                        .withStatusCode(400)
                        .withBody("{\"authorized\": false, \"message\": \"CPF ou senha não informados.\"}");
            }

            // Validação do formato do CPF
            if (!isValidCPF(cpf)) {
                return response
                        .withStatusCode(400)
                        .withBody("{\"authorized\": false, \"message\": \"CPF inválido.\"}");
            }

            // Consultar o backend via API Gateway
            boolean isAuthorized = consultaAPIGatewayBackend(cpf, senha, context);

            if (!isAuthorized) {
                return response
                        .withStatusCode(401)
                        .withBody("{\"authorized\": false, \"message\": \"Credenciais inválidas.\"}");
            }

            // Gerar Token JWT
            String token = generateToken(cpf);
            return response
                    .withStatusCode(200)
                    .withBody(String.format("{\"authorized\": true, \"message\": \"Cliente autenticado com sucesso.\", \"token\": \"%s\"}", token));

        } catch (Exception e) {
            context.getLogger().log("Erro inesperado: " + e.getMessage());
            return response
                    .withStatusCode(500)
                    .withBody("{\"message\": \"Erro interno no servidor.\"}");
        }
    }

    private boolean isValidCPF(String cpf) {
        return cpf.length() == 11 && cpf.matches("\\d+");
    }

    private boolean consultaAPIGatewayBackend(String cpf, String senha, Context context) {
        try {
            // Configurar URL e conexão
            URL url = new URL(API_GATEWAY_BACKEND_URL + "/api/v1/person/authenticate");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Construir o JSON para enviar ao backend
            Map<String, String> payload = new HashMap<>();
            payload.put("cpf", cpf);
            payload.put("senha", senha);

            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                writer.write(objectMapper.writeValueAsString(payload));
                writer.flush();
            }

            // Verificar a resposta do backend
            int responseCode = connection.getResponseCode();
            context.getLogger().log("API Gateway Backend response code: " + responseCode);

            // Credenciais válidas (200 OK)
            return responseCode == 200;

        } catch (Exception e) {
            context.getLogger().log("Erro ao consultar API Gateway Backend: " + e.getMessage());
            return false;
        }
    }

    private String generateToken(String cpf) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
        return JWT.create()
                .withClaim("cpf", cpf)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000)) // Token válido por 1 hora
                .sign(algorithm);
    }
}
