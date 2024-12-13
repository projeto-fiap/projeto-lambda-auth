# Imagem base para Lambda com Java 17
FROM public.ecr.aws/lambda/java:17

# Copiar o JAR gerado pelo Maven para o container
COPY target/lambda-auth.jar ${LAMBDA_TASK_ROOT}

# Configurar o handler principal
CMD ["auth.App::handleRequest"]
