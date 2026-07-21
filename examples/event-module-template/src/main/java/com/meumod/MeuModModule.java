package com.meumod;

import com.pedrodalben.bigbangeventos.api.BigBangEventModule;
import com.pedrodalben.bigbangeventos.api.EventModuleContext;

public class MeuModModule implements BigBangEventModule {
    @Override
    public String moduleId() {
        return "meumod";
    }

    @Override
    public int apiVersion() {
        return 1;
    }

    @Override
    public void onLoad(EventModuleContext ctx) {
        // 1. Ler config, inicializar estruturas
    }

    @Override
    public void onEnable(EventModuleContext ctx) {
        // 2. Registrar EventType, comandos, listeners
        ctx.typeRegistry().register(new MeuTipoEvento());
    }

    @Override
    public void onDisable(EventModuleContext ctx) {
        // 3. Salvar dados, limpar recursos
    }
}
