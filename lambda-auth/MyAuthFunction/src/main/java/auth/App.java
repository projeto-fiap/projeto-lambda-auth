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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SECRET_KEY = System.getenv("SECRET_KEY");
    private static final String API_GATEWAY_BACKEND_URL = "https://nch98tb5bg.execute-api.us-east-1.amazonaws.com/prod/api/v1/person/cpf";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);

        try {
            // Parse o JSON recebido
            Map<String, String> body = objectMapper.readValue(input.getBody(), Map.class);
            String cpf = body.get("cpf");
            String senha = body.get("senha");

            // Caso CPF e senha não sejam fornecidos, gerar um token anônimo
            if ((cpf == null || cpf.isEmpty()) && (senha == null || senha.isEmpty())) {
                context.getLogger().log("Acesso anônimo permitido.");
                String token = generateAnonymousToken();
                return response
                        .withStatusCode(200)
                        .withBody(String.format("{\"authorized\": true, \"message\": \"Acesso anônimo permitido.\", \"token\": \"%s\"}", token));
            }

            // Caso CPF ou senha estejam ausentes, retornar erro
            if (cpf == null || cpf.isEmpty() || senha == null || senha.isEmpty()) {
                context.getLogger().log("CPF ou senha não informados.");
                return response
                        .withStatusCode(400)
                        .withBody("{\"authorized\": false, \"message\": \"CPF ou senha não informados.\"}");
            }

            // Consultar o backend para validar as credenciais
            boolean isValid = consultaBackend(cpf, senha, context);

            if (!isValid) {
                return response
                        .withStatusCode(401)
                        .withBody("{\"authorized\": false, \"message\": \"CPF ou senha inválidos.\"}");
            }

            // Geração do Token JWT após autenticação bem-sucedida
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

    private boolean consultaBackend(String cpf, String senha, Context context) {
        try {
            // Construir URL com o CPF
            URL url = new URL(API_GATEWAY_BACKEND_URL + "?cpf=" + cpf);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");

            // Adicionar cabeçalho de autenticação Basic
            String auth = Base64.getEncoder().encodeToString((cpf + ":" + senha).getBytes());
            connection.setRequestProperty("Authorization", "Basic " + auth);

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
                String returnedCpf = (String) ((Map<String, Object>) ((List<Object>) responseBody.get("document")).get(0)).get("value");
                String returnedPassword = (String) responseBody.get("password");

                // Retornar verdadeiro se CPF e senha forem válidos
                return cpf.equals(returnedCpf) && senha.equals(returnedPassword);
            }
        } catch (Exception e) {
            context.getLogger().log("Erro ao consultar backend: " + e.getMessage());
        }
        return false;
    }


    private String generateToken(String cpf) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
        return JWT.create()
                .withClaim("cpf", cpf)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(algorithm);
    }

    private String generateAnonymousToken() {
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
        return JWT.create()
                .withClaim("role", "anonymous")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(algorithm);
    }
}
