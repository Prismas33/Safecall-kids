# Instruções para instalar `assetlinks.json` e validar App Links

Este ficheiro descreve exatamente o que fazer para que o domínio https://www.prismas33.eu valide App Links para o pacote `com.safecallkids.app`.

## 1) Conteúdo do ficheiro
Crie um ficheiro chamado `assetlinks.json` com o seguinte conteúdo EXACTO (não altere aspas ou colchetes):

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.safecallkids.app",
    "sha256_cert_fingerprints":
    ["7B:D9:8A:13:B5:41:90:B9:5F:20:11:C7:30:B4:A1:19:49:4C:52:6A:E5:D1:7F:22:D6:B7:34:E8:10:9C:EA:D9"]
  }
}]
```

> Observação: se usar Play App Signing, adicione também a fingerprint SHA‑256 do Play no array `sha256_cert_fingerprints` (adicione outra string na lista).

## 2) Local exacto no servidor
- O ficheiro deve ser servido em:
  - `https://www.prismas33.eu/.well-known/assetlinks.json`
- Requisitos:
  - HTTPS válido (certificado correcto)
  - Resposta HTTP 200 OK para `GET` no caminho acima
  - Opcional: cabeçalho `Content-Type: application/json` (recomendado)

## 3) Comandos para fazer upload / verificar (Windows PowerShell)
- Copie o ficheiro criado para o servidor web no caminho `.well-known/assetlinks.json` (use o método de deploy do seu host: SFTP/SSH/FTP/Panel/S3/Netlify, etc.).

- Verificar status HTTP:
```powershell
curl -I https://www.prismas33.eu/.well-known/assetlinks.json
```
- Verificar conteúdo:
```powershell
curl https://www.prismas33.eu/.well-known/assetlinks.json
```
Deve retornar `200 OK` no primeiro comando e o JSON acima no segundo.

## 4) Como obter a SHA‑256 da sua keystore local (se precisar alterar o JSON)
- No seu ambiente Windows (PowerShell), execute:
```powershell
keytool -list -v -keystore "E:\projetos-vs\Safecall-kids\release-key.keystore" -alias safecall-release
```
- Copie a linha `SHA256:` e coloque essa string no campo `sha256_cert_fingerprints` do JSON (substitua a existente ou adicione como segundo item).

> Se a keystore pedir password e quiser passar no comando, utilize `-storepass` (cuidado ao expor senhas em logs).

## 5) Validar na Play Console
- Abra Play Console → App → Setup → App integrity (ou Web Links) → Web links associados → Revalidar domínio.
- Se estiver correcto, o Play mostrará que o domínio está validado e os links funcionarão.

## 6) Erros comuns e como corrigir
- 404 em `/.well-known/assetlinks.json`: ficheiro não existe no servidor nesse caminho — carregue para esse caminho.
- 200 OK mas JSON errado: verifique se o ficheiro serve o JSON EXACTO (sem HTML, sem redirecionamento que devolve HTML).
- HTTPS inválido: corrija o certificado do domínio.
- Uso de Play App Signing: inclua a fingerprint do Play no array (ver Play Console → App integrity → App signing key certificate → SHA‑256).

## 7) Adicional (opcional)
- Se desejar, posso gerar um `assetlinks.json` que contenha duas fingerprints (sua release + Play). Se fornecer a fingerprint do Play eu actualizo o ficheiro.
- Posso também ajudá‑lo a fazer upload via SFTP/SSH se me fornecer credenciais ou instruções de deploy (ou posso gerar o ficheiro pronto para copiar).

---
Arquivo criado: `ASSETLINKS_INSTALL.md` na raiz do repositório. Siga os passos 1–3 para publicar e, em seguida, revalide na Play Console.
