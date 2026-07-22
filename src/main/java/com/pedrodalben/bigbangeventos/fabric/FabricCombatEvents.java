package com.pedrodalben.bigbangeventos.fabric;

import com.pedrodalben.bigbangeventos.BigBangEventos;
import com.pedrodalben.bigbangeventos.core.EventEngine;
import com.pedrodalben.bigbangeventos.definition.combat.CombatRuleSet;
import com.pedrodalben.bigbangeventos.domain.CombatEvents;
import com.pedrodalben.bigbangeventos.participant.combat.ParticipantCombatState;
import com.pedrodalben.bigbangeventos.session.EventSession;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public final class FabricCombatEvents {
    private FabricCombatEvents() {}

    public static void register() {
        registerDeathHandler();
        registerDamageHandler();
        registerRespawnHandler();
    }

    private static void registerDeathHandler() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayer victim)) return;
            EventEngine engine = BigBangEventos.engine();
            EventSession session = engine.sessionByPlayer(victim.getUUID()).orElse(null);
            if (session == null) return;

            UUID killerId = null;
            if (source.getEntity() instanceof ServerPlayer killer) {
                killerId = killer.getUUID();
            }
            String sourceName = source.getMsgId();

            engine.events().publish(new CombatEvents.ParticipantDeath(
                session.eventId(), session.id(), victim.getUUID(), killerId, sourceName));
        });
    }

    private static void registerDamageHandler() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer victim)) return true;
            EventEngine engine = BigBangEventos.engine();
            EventSession session = engine.sessionByPlayer(victim.getUUID()).orElse(null);
            if (session == null) return true;

            ParticipantCombatState state = session.combatState(victim.getUUID()).orElse(null);
            if (state == null) return true;

            // ponytail: invulnerability check, no complex effect
            if (state.isInvulnerable(java.time.Clock.systemUTC().instant())) return false;

            // ponytail: friendly fire handled after death, but filter damage too
            if (source.getEntity() instanceof ServerPlayer attacker) {
                if (session.hasSpectator(attacker.getUUID()) || session.combatState(attacker.getUUID())
                        .map(s -> s.eliminated()).orElse(false)) return false;
            }

            engine.events().publish(new CombatEvents.ParticipantDamaged(
                session.eventId(), session.id(), victim.getUUID(),
                source.getEntity() instanceof ServerPlayer a ? a.getUUID() : null,
                amount, source.getMsgId()));
            return true;
        });
    }

    private static void registerRespawnHandler() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            EventEngine engine = BigBangEventos.engine();
            EventSession session = engine.sessionByPlayer(newPlayer.getUUID()).orElse(null);
            if (session == null) return;

            engine.events().publish(new CombatEvents.ParticipantRespawned(
                session.eventId(), session.id(), newPlayer.getUUID()));
        });
    }
}
