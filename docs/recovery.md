# Recuperação

A política configurada é `cancel-and-restore`. Sessões persistem metadados e nunca são descartadas silenciosamente. O carregamento de snapshots reais e a restauração em reconnect ainda dependem de um adapter Fabric futuro; até lá o modo de inventário seguro é `KEEP` ou o gateway explícito `CLEAR_AND_RESTORE`.
