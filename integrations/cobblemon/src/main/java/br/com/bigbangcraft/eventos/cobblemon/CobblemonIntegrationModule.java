package br.com.bigbangcraft.eventos.cobblemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.pedrodalben.bigbangeventos.api.combat.CombatProvider;
import com.pedrodalben.bigbangeventos.api.module.BigBangEventModule;
import com.pedrodalben.bigbangeventos.api.module.EventModuleContext;

public class CobblemonIntegrationModule implements BigBangEventModule {
    private CobblemonCombatProvider provider;

    @Override public String id() { return "cobblemon_integration"; }
    @Override public String name() { return "BigBangEventos Cobblemon Integration"; }
    @Override public String version() { return "0.4.0-SNAPSHOT"; }
    @Override public int requiredApiVersion() { return 4; }

    @Override
    public void onEnable(EventModuleContext ctx) {
        try {
            Class.forName("com.cobblemon.mod.common.Cobblemon");
        } catch (ClassNotFoundException e) {
            ctx.logger().warn("Cobblemon nao detectado. Integracao desabilitada.");
            return;
        }

        provider = new CobblemonCombatProvider(ctx.events());
        ctx.api().combatProviders().register(provider);
        ctx.logger().info("CobblemonCombatProvider registrado. Versao: " + Cobblemon.VERSION);

        CobblemonEvents.POKEMON_SENT_POST.subscribe(event -> {
            PokemonEntity entity = event.getPokemonEntity();
            if (entity != null) provider.onPokemonSent(entity);
        });

        CobblemonEvents.POKEMON_RECALL_POST.subscribe(event -> {
            PokemonEntity entity = event.getOldEntity();
            if (entity != null) provider.onPokemonRecalled(entity);
        });

        CobblemonEvents.POKEMON_FAINTED.subscribe(event -> {
            var pokemon = event.getPokemon();
            if (pokemon != null) provider.onPokemonFainted(pokemon.getUuid());
        });
    }

    @Override
    public void onDisable(EventModuleContext ctx) {
        if (provider != null) provider.cleanupAll();
        ctx.logger().info("CobblemonIntegration desabilitado.");
    }
}
