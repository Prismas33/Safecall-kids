# SafecallKids - Bloqueador de Chamadas para Crianças

Um aplicativo Android desenvolvido em Kotlin que bloqueia automaticamente chamadas de números que não estão na lista de contatos, oferecendo proteção especializada para crianças contra chamadas indesejadas.

## ⚠️ ERRO DE COMPILAÇÃO DETECTADO

**O projeto ainda não pode ser compilado porque falta configurar o ambiente de desenvolvimento.**

### Para Resolver o Erro:

#### 1. Instalar Java Development Kit (JDK)
```bash
# O erro mostrou que Java não está instalado
java -version  # ❌ Comando não encontrado
```

**Solução:**
- Baixe o **JDK 17** em: https://adoptium.net/
- Instale e configure a variável `JAVA_HOME`
- Adicione `%JAVA_HOME%\bin` ao PATH do Windows

#### 2. Instalar Android SDK
- Baixe o **Android Studio**: https://developer.android.com/studio  
- Instale o Android SDK
- Configure `ANDROID_HOME` para o caminho do SDK
- Adicione `%ANDROID_HOME%\platform-tools` ao PATH

#### 3. Verificar Configuração
Após instalar, teste:
```bash
java -version           # Deve mostrar Java 17+
.\gradlew.bat --version # Deve executar sem erro
```

## Funcionalidades

- 🔒 **Bloqueio Automático**: Bloqueia automaticamente chamadas de números desconhecidos
- 📱 **Lista de Contatos**: Permite chamadas apenas de números salvos nos contatos
- 📊 **Estatísticas**: Mostra quantas chamadas foram bloqueadas
- 🔔 **Notificação Persistente**: Indica quando a proteção está ativa
- 🛡️ **Proteção Contínua**: Funciona em segundo plano

## Como Compilar (Após Configurar o Ambiente)

1. **Compile o projeto:**
   ```bash
   .\gradlew.bat assembleDebug
   ```

2. **Instale no dispositivo:**
   ```bash
   .\gradlew.bat installDebug
   ```

## Código Criado

O projeto já contém todos os arquivos necessários:

- ✅ `MainActivity.kt` - Interface principal com permissões
- ✅ `CallBlockingService.kt` - Serviço que monitora chamadas  
- ✅ `CallReceiver.kt` - Recebe eventos de chamadas telefônicas
- ✅ `ContactsHelper.kt` - Gerencia lista de contatos
- ✅ `AndroidManifest.xml` - Permissões configuradas
- ✅ Layouts e recursos visuais

## Permissões Necessárias

- `READ_PHONE_STATE` - Detectar chamadas recebidas
- `READ_CONTACTS` - Acessar lista de contatos
- `ANSWER_PHONE_CALLS` - Bloquear chamadas (Android 9+)
- `SYSTEM_ALERT_WINDOW` - Interface sobre outras telas

## Limitações do Android

- **Android 10+**: Google limitou acesso a chamadas
- **Dispositivo Físico**: Emulador não suporta chamadas reais
- **App Padrão**: Pode precisar ser definido como app de telefone padrão

---

**Próximo Passo**: Configure o Java e Android SDK seguindo as instruções acima, depois execute `.\gradlew.bat assembleDebug` para compilar o projeto.

## Requisitos

- Android 7.0 (API 24) ou superior
- Permissões necessárias:
  - Leitura de estado do telefone
  - Leitura de contatos
  - Responder chamadas telefônicas
  - Janela de sistema (para notificações)

## Como Usar

1. **Instalar o App**: Instale o SafecallKids no dispositivo
2. **Conceder Permissões**: O app solicitará as permissões necessárias
3. **Ativar Proteção**: Toque em "Conceder Permissões" para ativar
4. **Configurar como App Padrão**: Para funcionar completamente, pode ser necessário definir como app de telefone padrão

## Desenvolvimento

### Estrutura do Projeto

```
app/
├── src/main/
│   ├── java/com/safecallkids/app/
│   │   ├── MainActivity.kt          # Tela principal
│   │   ├── CallBlockingService.kt   # Serviço de bloqueio
│   │   ├── CallReceiver.kt          # Receptor de chamadas
│   │   └── ContactsHelper.kt        # Helper para contatos
│   ├── res/                         # Recursos do app
│   └── AndroidManifest.xml          # Configurações e permissões
```

### Como Compilar

1. Instale o Android SDK
2. Configure as variáveis de ambiente
3. Execute:
   ```bash
   ./gradlew assembleDebug
   ```

### Como Instalar

```bash
./gradlew installDebug
```

## Observações Importantes

- **Limitações do Android**: A partir do Android 10, o bloqueio de chamadas pode ter limitações devido às políticas de segurança
- **Permissões Especiais**: Algumas funcionalidades podem requerer que o app seja definido como aplicativo de telefone padrão
- **Teste em Emulador**: Para testar completamente, use um dispositivo físico com chip de telefone

## Licença

Este projeto foi desenvolvido para fins educacionais e de proteção infantil.

## Contribuição

Para contribuir com melhorias:
1. Faça um fork do projeto
2. Crie uma branch para sua feature
3. Commit suas mudanças
4. Faça um pull request

---

**Importante**: Este app foi desenvolvido com foco na segurança e proteção de crianças. Use com responsabilidade e sempre teste em ambiente controlado antes de usar em produção.
