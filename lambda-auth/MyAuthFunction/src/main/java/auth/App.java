package auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.*;

public class App implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    // Use variáveis de ambiente (ou Secrets Manager) em produção
    private static final String SECRET_KEY = System.getenv("JWT_SECRET") != null
            ? System.getenv("JWT_SECRET")
            : "chave-secreta-em-desenvolvimento";
    private static final String ISSUER = "fiap-tech-project";

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Map<String, String> headers = (Map<String, String>) input.get("headers");
            if (headers == null) {
                headers = new HashMap<>();
            }

            String authHeader = headers.get("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                context.getLogger().log("Token JWT não encontrado ou mal formatado.");
                return generatePolicy("user", "Deny", getMethodArn(input));
            }

            // Remove prefixo "Bearer "
            String token = authHeader.substring(7).trim();

            // Valida token
            if (isValidToken(token, context)) {
                context.getLogger().log("Token JWT válido. Acesso permitido.");
                return generatePolicy("user", "Allow", getMethodArn(input));
            } else {
                context.getLogger().log("Token JWT inválido. Acesso negado.");
                return generatePolicy("user", "Deny", getMethodArn(input));
            }

        } catch (Exception e) {
            context.getLogger().log("Erro no Authorizer: " + e.getMessage());
            return generatePolicy("user", "Deny", getMethodArn(input));
        }
    }

    private boolean isValidToken(String token, Context context) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
            DecodedJWT jwt = verifier.verify(token);

            // Se chegou aqui, token é válido.
            context.getLogger().log("JWT subject: " + jwt.getSubject()); // Exemplo de log
            return true;
        } catch (Exception ex) {
            context.getLogger().log("Falha na validação do JWT: " + ex.getMessage());
            return false;
        }
    }

    private String getMethodArn(Map<String, Object> input) {
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
