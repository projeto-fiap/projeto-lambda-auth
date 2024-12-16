package auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class App implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String API_GATEWAY_BACKEND_URL = "https://acmulpj854.execute-api.us-east-2.amazonaws.com/prod/api/v1/person/cpf";

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

        try {
            // O evento do Custom Authorizer geralmente traz informações do método invocado em "methodArn" e headers em "headers"
            Map<String, String> headers = (Map<String, String>) input.get("headers");
            if (headers == null) {
                headers = new HashMap<>();
            }

            String authHeader = headers.get("Authorization");

            // Se não houver cabeçalho Authorization, permitir acesso anônimo ou negar - aqui podemos negar.
            if (authHeader == null || authHeader.isEmpty()) {
                context.getLogger().log("Acesso anônimo detectado. Negando acesso.");
                return generatePolicy("user", "Deny", getMethodArn(input));
            }

            // Validar cabeçalho Authorization
            if (!authHeader.startsWith("Basic ")) {
                context.getLogger().log("Authorization inválido.");
                return generatePolicy("user", "Deny", getMethodArn(input));
            }

            // Consultar backend para validação
            boolean isValid = consultaBackend(authHeader, context);

            if (isValid) {
                // Retorna política Allow
                context.getLogger().log("Credenciais válidas. Permitindo acesso.");
                return generatePolicy("user", "Allow", getMethodArn(input));
            } else {
                // Retorna política Deny
                context.getLogger().log("Credenciais inválidas. Negando acesso.");
                return generatePolicy("user", "Deny", getMethodArn(input));
            }

        } catch (Exception e) {
            context.getLogger().log("Erro inesperado: " + e.getMessage());
            // Em caso de erro, nega o acesso.
            return generatePolicy("user", "Deny", getMethodArn(input));
        }
    }

    private boolean consultaBackend(String authHeader, Context context) {
        try {
            String credentials = new String(Base64.getDecoder().decode(authHeader.replace("Basic ", "")));
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                context.getLogger().log("Formato de credenciais inválido.");
                return false;
            }

            String cpf = parts[0];
            String senha = parts[1];

            URL url = new URL(API_GATEWAY_BACKEND_URL + "?cpf=" + cpf);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", authHeader);

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

                Map<String, Object> responseBody = objectMapper.readValue(response.toString(), Map.class);

                // Validar CPF e senha retornados
                String returnedCpf = (String) ((Map<String, Object>) ((List<Object>) responseBody.get("document")).get(0)).get("value");
                String returnedPassword = (String) responseBody.get("password");

                return cpf.equals(returnedCpf) && senha.equals(returnedPassword);
            }
        } catch (Exception e) {
            context.getLogger().log("Erro ao consultar backend: " + e.getMessage());
        }
        return false;
    }

    private String getMethodArn(Map<String, Object> input) {
        // methodArn normalmente vem no input do authorizer.
        // Exemplo: "arn:aws:execute-api:us-east-2:123456789012:abcdefghij/dev/GET/resource"
        return (String) input.get("methodArn");
    }

    private Map<String, Object> generatePolicy(String principalId, String effect, String resource) {
        Map<String, Object> policyDocument = new HashMap<>();
        policyDocument.put("Version", "2012-10-17");

        Map<String, Object> statement = new HashMap<>();
        statement.put("Action", "execute-api:Invoke");
        statement.put("Effect", effect);
        statement.put("Resource", resource);

        policyDocument.put("Statement", Collections.singletonList(statement));

        Map<String, Object> authResponse = new HashMap<>();
        authResponse.put("principalId", principalId);
        authResponse.put("policyDocument", policyDocument);

        return authResponse;
    }
}
