# Plano de melhorias da app SafecallKids

## Objetivo

Melhorar a app por partes, mantendo a funcionalidade atual a 100% salvo quando existir uma melhoria clara, justificada e validada. A prioridade e reduzir risco, melhorar manutencao, limpar apresentacao e preparar a app para Play Store sem reescrever tudo de uma vez.

## Estado atual observado

A app e Android nativa em Kotlin, com uma estrutura pequena e funcional:

- `MainActivity.kt`: tela principal, permissoes, dialogs, idioma, verificacao de estado, navegacao para definicoes e diagnostico.
- `CallScreeningService.kt`: decisao moderna de permitir/bloquear chamadas.
- `CallReceiver.kt`: fallback legado para eventos de telefone.
- `CallBlockingService.kt`: servico foreground e notificacao persistente.
- `ContactsHelper.kt`: leitura e comparacao de contactos.
- `SafecallApplication.kt`: aplicacao de idioma guardado.
- XMLs de layout, strings, backup e manifest.

O principal problema nao e falta de funcionalidade. O problema e concentracao de responsabilidades, duplicacao de regras e alguns riscos de seguranca, UX e publicacao.

## Principio de trabalho

Cada etapa deve ser pequena, reversivel e facil de testar. Antes de alterar comportamento real de bloqueio de chamadas, a regra deve ficar explicita e validavel.

Nao vamos fazer uma refatoracao grande de uma vez. A app lida com permissoes sensiveis, chamadas, roles do Android e politicas da Play Store, por isso a abordagem correta e cirurgica.

## Prioridades

1. Preservar bloqueio atual de chamadas.
2. Evitar regressao em permissoes, roles e estado ativo/inativo.
3. Melhorar layout e apresentacao sem mudar regras internas.
4. Separar responsabilidades em ficheiros proprios.
5. Centralizar estado e regras duplicadas.
6. Reduzir risco de Play Store e politica de familias.
7. Melhorar textos, idiomas e consistencia visual.
8. Reativar validacoes quando o Gradle permitir correr localmente.

## Problemas encontrados

### 1. MainActivity demasiado grande

`MainActivity.kt` tem cerca de 1186 linhas e mistura muitas responsabilidades:

- UI principal.
- Estado da protecao.
- Permissoes Android.
- Role de telefone/call screening.
- Dialogs de configuracao.
- SharedPreferences.
- Idioma.
- Diagnostico.
- Abertura de definicoes do sistema.

Isto torna cada alteracao arriscada, porque uma mudanca visual pode tocar em logica critica.

### 2. Regras duplicadas

A decisao "a protecao esta ativa?" aparece em varios sitios:

- `MainActivity.kt`
- `CallScreeningService.kt`
- `CallReceiver.kt`

Tambem ha acesso repetido a `safecall_prefs`, `all_setup_completed`, `call_screening_configured` e `blocked_calls_count`.

Quando esta logica fica duplicada, uma correcao pode ser aplicada num ficheiro e esquecida noutro.

### 3. Layout e apresentacao podem melhorar

A tela principal funciona, mas pode ficar mais clara:

- Estado principal mais forte e facil de entender.
- Botao principal unico para acao mais importante.
- Separacao visual melhor entre configuracao, estado e estatisticas.
- Textos menos tecnicos para utilizador final.
- Melhor adaptacao a ecras pequenos.
- Dialogs mais limpos e orientados por passos.

A melhoria visual deve preservar IDs e fluxo atual sempre que possivel.

### 4. Strings e idiomas inconsistentes

Existem textos misturados entre portugues e ingles no fallback `values/strings.xml`, termos tecnicos como "Call Screening" visiveis ao utilizador e alguns erros de escrita, como "Bloquer".

Tambem existem textos longos com HTML em strings simples. Isto deve ser revisto com cuidado para nao quebrar exibicao.

### 5. Backup e privacidade

O manifest permite backup e `backup_rules.xml` inclui tudo. Para uma app de protecao/familia, isto deve ser revisto. Preferivel excluir preferencias e dados internos que possam conter estado sensivel, logs ou informacao operacional.

### 6. Lint release desativado

O `build.gradle` desativa checks de lint no release. Isto ajuda a gerar build, mas pode esconder problemas importantes para Play Store, Android 15, permissoes e traducoes.

Nao recomendo ligar tudo de uma vez se isso bloquear release, mas devemos tratar como divida tecnica e ir removendo supressoes.

### 7. Contactos consultados diretamente em cada decisao

`ContactsHelper` consulta todos os contactos para comparar numeros. Para uma app pequena isto pode funcionar, mas a longo prazo pode ser melhor separar:

- leitura de contactos;
- normalizacao de numeros;
- decisao de correspondencia;
- cache temporaria controlada.

Qualquer cache deve ser simples e segura, porque contactos mudam e permissao pode ser revogada.

## Estrutura proposta

Primeira fase, sem mudar comportamento:

```text
app/src/main/java/com/safecallkids/app/
  data/
    ProtectionPreferences.kt

  domain/
    ProtectionStatus.kt
    ProtectionStatusChecker.kt

  system/
    PermissionChecker.kt
    RoleChecker.kt
    SettingsNavigator.kt

  ui/
    MainUiState.kt
```

Fases seguintes, se fizer sentido:

```text
app/src/main/java/com/safecallkids/app/
  contacts/
    ContactsRepository.kt
    PhoneNumberMatcher.kt

  calls/
    CallBlockDecision.kt
    BlockedCallsCounter.kt

  ui/dialogs/
    GuidedSetupDialog.kt
    InstructionsDialog.kt
```

Esta organizacao evita ficheiros artificiais. Cada ficheiro so deve nascer quando retirar acoplamento real de `MainActivity`, `CallScreeningService` ou `CallReceiver`.

## Plano por fases

### Fase 1 - Base segura sem alterar comportamento

O que fazer:

- Criar `ProtectionPreferences`.
- Criar `PermissionChecker`.
- Criar `RoleChecker`.
- Criar `ProtectionStatusChecker`.
- Trocar leituras diretas repetidas por essas classes.
- Manter os mesmos nomes de flags em SharedPreferences.
- Manter os mesmos fluxos de ativar/desativar.

Por que:

- Reduz duplicacao.
- Permite testar e auditar a regra central de protecao.
- Prepara a app para futuras melhorias sem tocar no bloqueio em si.

Validacao:

- App continua a mostrar estado correto.
- Ativar/desativar mantem comportamento.
- Service e receiver consultam a mesma regra.
- Nenhuma chave de preferencia muda.

### Fase 2 - Melhorar layout sem mudar logica

O que fazer:

- Reorganizar `activity_main.xml`.
- Dar mais destaque ao estado da protecao.
- Melhorar espacamentos e hierarquia visual.
- Garantir scroll em ecras pequenos.
- Melhorar estados dos botoes.
- Manter IDs usados por `MainActivity`.

Por que:

- Melhora a experiencia sem mexer na regra de chamadas.
- Reduz confusao entre "configurar", "verificar" e "ativar".

Validacao:

- Todos os botoes continuam ligados aos mesmos handlers.
- Layout nao corta texto em portugues e ingles.
- Tela funciona em ecras pequenos.

### Fase 3 - Textos e apresentacao

O que fazer:

- Corrigir erros de portugues.
- Reduzir termos tecnicos visiveis.
- Alinhar `values`, `values-pt` e `values-en`.
- Garantir que textos de Play Store e app apontam para "ferramenta para pais/responsaveis".
- Rever dialogs longos.

Por que:

- A app fica mais profissional.
- Ajuda em revisao da Play Store.
- Reduz confusao para utilizador final.

Validacao:

- Sem missing strings.
- Sem textos truncados nos botoes.
- Sem regressao de idioma.

### Fase 4 - Privacidade e Play Store

O que fazer:

- Rever `allowBackup`.
- Rever `backup_rules.xml` e `data_extraction_rules.xml`.
- Evitar backup de dados sensiveis.
- Rever permissoes no manifest.
- Avaliar se todas as permissoes sao realmente necessarias.
- Reduzir supressoes de lint gradualmente.

Por que:

- App lida com chamadas e contactos.
- Google Play e politicas de familia exigem cuidado extra.

Validacao:

- Build release continua possivel.
- Funcionalidade de chamada nao perde permissao necessaria.
- Lint fica progressivamente mais limpo.

### Fase 5 - Contactos e decisao de bloqueio

O que fazer:

- Separar normalizacao de numeros.
- Separar query de contactos.
- Separar decisao "bloquear ou permitir".
- Eventualmente adicionar cache curta e invalidavel.

Por que:

- Facilita testar comparacao de numeros.
- Reduz custo por chamada.
- Evita bugs com formatos internacionais.

Validacao:

- Numeros guardados continuam permitidos.
- Numeros desconhecidos continuam bloqueados.
- Chamadas privadas continuam bloqueadas quando a protecao esta ativa.

### Fase 6 - Validacao automatica

O que fazer:

- Correr `:app:lintDebug`.
- Correr `:app:assembleDebug`.
- Se possivel, correr `:app:bundleRelease`.
- Adicionar testes unitarios pequenos para normalizacao/comparacao de numeros quando a logica for separada.

Por que:

- Evita quebrar a app silenciosamente.
- Garante que refatoracoes sao seguras.

Nota:

Nesta analise inicial, o Gradle ficou bloqueado no cache fora do workspace. Antes de considerar a build validada, e preciso voltar a correr os comandos num ambiente Gradle desbloqueado.

## Regras de seguranca durante as alteracoes

- Nao mudar nomes de SharedPreferences sem migracao.
- Nao mudar a regra de bloqueio sem decisao explicita.
- Nao remover permissoes do manifest sem confirmar impacto.
- Nao mexer no versionCode/versionName sem pedido claro.
- Nao fazer refatoracao geral fora da fase atual.
- Nao misturar melhoria visual com mudanca de comportamento de chamadas.

## Sugestoes de melhoria real de comportamento

Estas melhorias podem valer a pena, mas devem ser tratadas como mudanca de produto, nao como refatoracao silenciosa:

1. Mostrar estado mais claro quando falta apenas uma permissao.
2. Trocar "Call Screening" por texto amigavel, como "filtro de chamadas".
3. Permitir reset do contador de chamadas bloqueadas.
4. Mostrar ultimo motivo de bloqueio apenas localmente, sem guardar numeros sensiveis.
5. Melhorar suporte para formatos internacionais de telefone.
6. Adicionar pagina simples de privacidade dentro da app.
7. Avisar quando a app deixou de ser telefone/filtro padrao.

## Primeira execucao recomendada

Comecar pela Fase 1:

1. Criar classes pequenas para preferencias, permissoes, roles e estado.
2. Atualizar `MainActivity`, `CallScreeningService` e `CallReceiver` para usar essas classes.
3. Nao alterar textos, layout ou manifest nesta fase.
4. Validar comportamento atual.

Depois seguir para Fase 2:

1. Melhorar layout principal.
2. Manter IDs existentes.
3. Validar visualmente e por build.

## Checklist de cada parte

Antes de fechar cada fase:

- A app compila.
- Nao ha alteracao inesperada no fluxo de ativar/desativar.
- Nao ha perda de permissoes.
- A regra de bloqueio continua igual, salvo decisao explicita.
- O diff fica pequeno e facil de rever.
- Qualquer melhoria de comportamento fica documentada.

