package auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SECRET_KEY = "your-secret-key"; // Substitua por uma chave segura

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);

        try {
            // Parse o JSON recebido
            Map<String, String> body = objectMapper.readValue(input.getBody(), Map.class);

            // Extrair CPF
            String cpf = body.get("cpf");
            if (cpf == null || cpf.isEmpty()) {
                context.getLogger().log("CPF não informado.");
                return response
                        .withStatusCode(400)
                        .withBody("{\"authorized\": false, \"message\": \"CPF não informado.\"}");
            }

            // Validação do CPF
            if (!isValidCPF(cpf)) {
                return response
                        .withStatusCode(400)
                        .withBody("{\"authorized\": false, \"message\": \"CPF inválido.\"}");
            }

            // Consulta ao banco de dados
            boolean isAuthorized = consultaBanco(cpf);
            if (!isAuthorized) {
                return response
                        .withStatusCode(404)
                        .withBody("{\"authorized\": false, \"message\": \"CPF não encontrado. Cadastro necessário.\"}");
            }

            // Geração do Token JWT
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

    private boolean consultaBanco(String cpf) {
        // TODO: Substitua por lógica real de consulta a um banco de dados
        // Simulação: Retornar true se o CPF for "12345678901"
        return "12345678901".equals(cpf); // Cliente autenticado
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
