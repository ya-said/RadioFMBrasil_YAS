# Rádio Brasil FM

Aplicativo Android de rádios brasileiras: gratuito, **sem anúncios**, sem conta e sem rastreamento.
Catálogo do diretório comunitário [Radio Browser](https://www.radio-browser.info), com cache offline e atualização diária.

## Funções
Busca por nome, estado e categoria · favoritos reordenáveis · histórico · mini player e player completo ·
rádio anterior/próxima (no app, na notificação e nos fones Bluetooth) · reprodução em segundo plano ·
reconexão automática · temporizador para dormir · tema claro/escuro · região preferida.

## Gerar o APK pelo GitHub (sem instalar nada no PC)
1. Crie um repositório novo no GitHub.
2. Envie **o conteúdo da pasta** (não a pasta em si): abra a pasta extraída, selecione tudo (Ctrl+A) e arraste para **Add file → Upload files**. Na raiz do repositório devem aparecer `.github`, `app`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` e `README.md`.
3. Se a pasta `.github` não subir, use **Add file → Create new file**, digite `.github/workflows/build-apk.yml` e cole o conteúdo do arquivo de mesmo nome deste projeto.
4. Aba **Actions** → abra a execução mais recente → no fim da página, em **Artifacts**, baixe **RadioBrasilFM-apk** (ZIP com o `app-debug.apk`).
5. Instale no celular permitindo "fontes desconhecidas".

O APK usa uma chave de assinatura fixa (`app/debug.keystore`), então os próximos APKs **atualizam o app por cima**, sem desinstalar e sem perder favoritos. (Se já tiver instalado uma versão antiga gerada com outra chave, desinstale-a uma única vez.)

## Testes
`gradle testDebugUnitTest` roda no workflow: busca/filtros, mapeamento do catálogo, estados, temporizador e navegação anterior/próxima.
Reprodução real, segundo plano e reconexão precisam ser testados no celular.

## Arquitetura
Kotlin + Jetpack Compose (Material 3), MVVM: `domain/` (lógica pura), `data/` (catálogo OkHttp+Gson com cache em arquivo, DataStore, WorkManager), `player/` (Media3 ExoPlayer + MediaSession em serviço de primeiro plano) e `ui/`.
HTTP em claro é aceito só para os streams (muitas rádios não têm HTTPS); a API do catálogo é sempre HTTPS e a validação TLS nunca é desativada.

## Limitações
O Radio Browser não tem campo de cidade (use o filtro por estado). Categorias dependem das tags das emissoras. Dados são comunitários: alguns streams estão fora do ar. "Informar problema" oculta a emissora só neste aparelho. A chave de assinatura é de teste (não serve para a Play Store).
