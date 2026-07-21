# Módulo Parkour — BigBangEventos

## O que é

Módulo que adiciona o tipo de evento `parkour` ao BigBangEventos. Um
evento de parkour consiste em um percurso com checkpoints numerados,
cronômetro individual, contagem de quedas e ranking por tempo de
conclusão.

## Compilar

```bash
# No diretório raiz do BigBangEventos
./gradlew :modules:parkour:build
```

Ou compile o módulo independentemente (requer `bigbangeventos.jar` como
dependência local).

## Instalar

1. Coloque `bigbangeventos-*.jar` em `mods/`.
2. Coloque `parkour-module-*.jar` em `mods/`.
3. Inicie o servidor.
4. Verifique os logs: `[BigBangEventos] Module 'parkour' loaded`.

## Dependências

| Dependência | Versão Mínima |
|-------------|---------------|
| BigBangEventos | 0.1.0 |
| Fabric Loader | 0.18.4 |
| Fabric API | 0.116.13+1.21.1 |
| Minecraft | 1.21.1 |
| Java | 21 |

## Criar um Evento Parkour

```bash
# 1. Criar evento
/evento create meu_parkour parkour

# 2. Editar
/evento edit meu_parkour

# 3. Definir localizações (fique no local e use:)
/evento set lobby
/evento set entrance
/evento set exit

# 4. Definir área do percurso (opcional, para detectar saída)
/evento set area

# 5. Criar checkpoints (para cada checkpoint no percurso)
/evento trigger create checkpoint_1 sign_interact
/evento trigger bind checkpoint_1

# Clique na placa do checkpoint 1
/evento trigger create checkpoint_2 sign_interact
/evento trigger bind checkpoint_2

# Clique na placa do checkpoint 2
# ... mais checkpoints ...

# 6. Criar gatilho de chegada
/evento trigger create finish sign_interact
/evento trigger bind finish

# 7. Verificar configuração
/evento validate meu_parkour

# 8. Abrir inscrições
/evento open meu_parkour

# 9. Iniciar
/evento start meu_parkour
```

## Configurar

Veja `docs/configuration.md` para campos de configuração.

## Comandos

| Comando | Descrição |
|---------|-----------|
| `/parkour` | Status do parkour |
| `/parkour leave` | Sair do parkour |
| `/parkour top` | Top 10 ranking |
| `/parkour checkpoints` | Listar checkpoints |
| `/parkour admin reset <player>` | Resetar jogador (admin) |
| `/parkour admin tp <checkpoint>` | Teleportar para checkpoint (admin) |
| `/parkour admin info` | Informações detalhadas (admin) |

## Testar

```bash
./gradlew :modules:parkour:test
```

Veja `docs/testing.md`.

## Limitações

- Checkpoints usam placas (SIGN_INTERACT). Região (REGION_ENTER) não
  está implementada.
- Detecção de queda usa distância vertical e área. Não detecta
  colisão com blocos específicos.
- Ranking é global por sessão. Não há ranking por categoria.
- Um jogador por sessão de parkour (não há equipes).

## Próximos Passos

- Implementar detecção de queda por altura personalizada por checkpoint.
- Adicionar power-ups (velocidade, salto).
- Suporte a múltiplas tentativas (melhor tempo).
- Checkpoints visuais com partículas.
- GUI de seleção de checkpoint no admin.
