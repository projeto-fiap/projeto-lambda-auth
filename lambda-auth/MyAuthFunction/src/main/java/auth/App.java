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

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);

        try {
            // TODO: Integração com API Gateway
            // Quando o API Gateway estiver configurado, ele enviará as requisições para esta função.
            // Você pode adicionar lógica específica aqui para lidar com headers, autenticação ou outros
            // dados fornecidos pelo Gateway.

            // Parse o JSON recebido
            Map<String, String> body = objectMapper.readValue(input.getBody(), Map.class);

            // Extrair CPF
            String cpf = body.get("cpf");
            if (cpf == null || cpf.isEmpty()) {
                context.getLogger().log("Operação continuada sem autenticação.");
                return response
                        .withStatusCode(200)
                        .withBody("{\"authorized\": false, \"message\": \"Operação continuada sem autenticação.\"}");
            }

            // Autenticação
            boolean isAuthorized = verificaCPF(cpf);
            String token = generateToken(cpf);
            String message = isAuthorized
                    ? "Cliente autenticado com sucesso"
                    : "CPF não encontrado, cliente registrado";

            return response
                    .withStatusCode(200)
                    .withBody(String.format("{\"authorized\": %b, \"message\": \"%s\", \"token\": \"%s\"}",
                            isAuthorized, message, token));

        } catch (Exception e) {
            context.getLogger().log("Erro inesperado: " + e.getMessage());
            return response.withStatusCode(500).withBody("{\"message\": \"Erro interno no servidor\"}");
        }
    }

    private boolean verificaCPF(String cpf) {
        // Verificar se o CPF tem 11 dígitos
        if (cpf.length() != 11 || !cpf.matches("\\d+")) {
            throw new IllegalArgumentException("CPF inválido");
        }

        // Simular consulta ao banco de dados
        if ("12345678901".equals(cpf)) {
            return true;
        } else {
            // Simular registro de cliente
            registraNovoCliente(cpf);
            return false;
        }
    }

    private void registraNovoCliente(String cpf) {
        // Simulação: adicione lógica para salvar em um banco de dados
        System.out.println("Registrando novo cliente com CPF: " + cpf);
    }

    private String generateToken(String cpf) {
        Algorithm algorithm = Algorithm.HMAC256("your-secret-key");
        return JWT.create()
                .withClaim("cpf", cpf)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(algorithm);
    }
}
