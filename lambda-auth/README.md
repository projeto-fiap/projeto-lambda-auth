
# lambda-auth

Este projeto contém o código-fonte e arquivos de suporte para uma aplicação serverless que utiliza AWS Lambda para autenticação de clientes com base em CPF. Ele inclui os seguintes arquivos e pastas:

- `MyAuthFunction/src/main` - Código da função Lambda e o Dockerfile do projeto.
- `events` - Eventos de exemplo para invocar a função localmente.
- `template.yaml` - Um template que define os recursos AWS usados pela aplicação.

A aplicação utiliza diversos recursos da AWS, incluindo funções Lambda e uma API Gateway. Esses recursos estão definidos no arquivo `template.yaml`. Você pode atualizar o template para adicionar novos recursos AWS ou ajustar os existentes.

---

## **Pré-requisitos**

Certifique-se de que você possui as seguintes ferramentas instaladas:

1. [SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
2. [Docker](https://www.docker.com/products/docker-desktop/)
3. [Java 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
4. [Maven](https://maven.apache.org/install.html)

---

## **Configuração e Deploy**

### **Construir e Rodar Localmente**

1. **Construa o projeto com o SAM CLI**:
   ```bash
   sam build
   ```

2. **Teste a função Lambda localmente**:
   Use o evento de exemplo (`events/event.json`) para invocar a função Lambda:

   ```bash
   sam local invoke MyAuthFunction --event events/event.json
   ```

   **Exemplo de saída esperada:**
   ```json
   {
       "statusCode": 200,
       "headers": {
           "Content-Type": "application/json"
       },
       "body": "{"authorized": true, "message": "Cliente autenticado com sucesso", "token": "<TOKEN>"}"
   }
   ```

3. **Simule o API Gateway localmente**:
   Inicie o API Gateway local com o seguinte comando:

   ```bash
   sam local start-api
   ```

   **Acesse o endpoint localmente:**
   ```bash
   curl -X POST http://127.0.0.1:3000/authenticate    -H "Content-Type: application/json"    -d '{"cpf": "12345678901"}'
   ```

   **Resposta esperada:**
   ```json
   {
       "authorized": true,
       "message": "Cliente autenticado com sucesso",
       "token": "<TOKEN>"
   }
   ```

### **Deploy para a AWS**

Para implantar a aplicação na AWS:

1. **Execute o comando de deploy**:
   ```bash
   sam deploy --guided
   ```

2. **Preencha as informações solicitadas**:
    - Nome do Stack
    - Região AWS
    - Permissões (IAM)

   Após o deploy, você verá o URL gerado pelo API Gateway, que pode ser usado para acessar a função Lambda.

---

## **Estrutura do Projeto**

```plaintext
lambda-auth/
├── events/                 # Arquivos de eventos para testes locais
│   └── event.json          # Evento de exemplo para invocar a Lambda
├── MyAuthFunction/
│   ├── src/                # Código fonte da função Lambda
│   ├── Dockerfile          # Arquivo Docker para o ambiente da Lambda
│   └── pom.xml             # Configuração do Maven
├── template.yaml           # Template do AWS SAM para configuração da função
└── README.md              
```

---

## **Testes Unitários**

Os testes estão definidos na pasta `MyAuthFunction/src/test`. Para rodar os testes:

1. Navegue até o diretório da função:
   ```bash
   cd MyAuthFunction
   ```

2. Execute os testes:
   ```bash
   mvn test
   ```

---

## **Debugging**

1. **Ver logs locais:**
   Para verificar os logs da função local, use:
   ```bash
   sam logs -n MyAuthFunction
   ```

2. **Erros comuns:**
    - **"Docker Daemon Not Running"**: Certifique-se de que o Docker está ativo.
    - **"Class Version Error"**: Confirme que está usando Java 17 para compilar.

---

## **Personalização**

1. **Alterar a chave secreta do JWT**:
   No arquivo `App.java`, substitua `"your-secret-key"` por uma chave forte:
   ```java
   Algorithm algorithm = Algorithm.HMAC256("your-secret-key");
   ```

2. **Configuração de variáveis de ambiente**:
   Você pode adicionar variáveis no `template.yaml` para customizar o comportamento da Lambda:
   ```yaml
   Environment:
     Variables:
       TABLE_NAME: "ClientesTable"
       REGION: "us-east-1"
   ```

---

## **Limpeza**

Para remover a aplicação da AWS, use o comando:
```bash
sam delete --stack-name lambda-auth
```

---

## **Recursos**

- [Guia do desenvolvedor AWS SAM](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/what-is-sam.html)
- [AWS Serverless Application Repository](https://aws.amazon.com/serverless/serverlessrepo/)

---

## **Licença**

Este projeto é distribuído sob a licença MIT. Consulte o arquivo `LICENSE` para mais detalhes.