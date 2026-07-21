package com.pedrodalben.bigbangeventos.fabric;

import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import com.pedrodalben.bigbangeventos.snapshot.PlayerSnapshot;
import com.pedrodalben.bigbangeventos.snapshot.SnapshotGateway;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class FabricSnapshotGateway implements SnapshotGateway {
    private static final Logger LOG = LoggerFactory.getLogger(FabricSnapshotGateway.class);
    private final MinecraftServer server;

    public FabricSnapshotGateway(MinecraftServer server) {
        this.server = server;
    }

    private ServerPlayer resolve(UUID playerId) {
        return server.getPlayerList().getPlayer(playerId);
    }

    @Override
    public PlayerSnapshot capture(UUID playerId, UUID snapshotId, UUID sessionId) {
        ServerPlayer player = resolve(playerId);
        if (player == null) throw new IllegalStateException("Jogador offline: " + playerId);

        StoredLocation location = new StoredLocation(
                "cobbleverse",
                player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());

        return new PlayerSnapshot(
                snapshotId, playerId, sessionId, location,
                serializeInventoryItems(playerId),
                serializeArmorItems(playerId),
                serializeOffhandItem(playerId),
                captureTotalExperience(playerId),
                captureExperienceLevel(playerId),
                captureExperienceProgress(playerId),
                captureHealth(playerId),
                captureAbsorption(playerId),
                captureFoodLevel(playerId),
                captureSaturation(playerId),
                captureGameMode(playerId),
                captureAllowFlight(playerId),
                captureIsFlying(playerId),
                captureFlySpeed(playerId),
                captureWalkSpeed(playerId),
                captureSelectedSlot(playerId),
                captureFireTicks(playerId),
                captureFallDistance(playerId),
                captureActiveEffects(playerId),
                Map.of()
        );
    }

    @Override
    public boolean restoreState(UUID playerId, PlayerSnapshot snapshot) {
        ServerPlayer player = resolve(playerId);
        if (player == null) return false;
        try {
            player.totalExperience = snapshot.totalExperience();
            player.experienceLevel = snapshot.experienceLevel();
            player.experienceProgress = snapshot.experienceProgress();
            player.setHealth((float) snapshot.health());
            player.setAbsorptionAmount((float) snapshot.absorption());
            player.getFoodData().setFoodLevel(snapshot.foodLevel());
            player.getFoodData().setSaturation(snapshot.saturation());
            player.getInventory().selected = snapshot.selectedSlot();

            try {
                net.minecraft.world.level.GameType gm = net.minecraft.world.level.GameType.valueOf(snapshot.gameMode());
                player.setGameMode(gm);
            } catch (IllegalArgumentException e) {
                LOG.warn("GameMode desconhecido: {}", snapshot.gameMode());
            }

            player.getAbilities().mayfly = snapshot.allowFlight();
            player.getAbilities().flying = snapshot.isFlying();
            player.getAbilities().setFlyingSpeed(snapshot.flySpeed());
            player.getAbilities().setWalkingSpeed(snapshot.walkSpeed());
            player.onUpdateAbilities();

            player.setRemainingFireTicks(snapshot.fireTicks());
            player.fallDistance = snapshot.fallDistance();

            player.removeAllEffects();
            restoreEffects(player, snapshot.activeEffects());

            return true;
        } catch (Exception e) {
            LOG.error("Falha ao restaurar estado de {}", playerId, e);
            return false;
        }
    }

    @Override
    public boolean restoreInventory(UUID playerId, PlayerSnapshot snapshot) {
        ServerPlayer player = resolve(playerId);
        if (player == null) return false;
        try {
            Inventory inv = player.getInventory();
            inv.clearContent();

            for (Map.Entry<String, String> entry : snapshot.serializedInventory().entrySet()) {
                int slot = Integer.parseInt(entry.getKey());
                ItemStack stack = deserializeItem(entry.getValue());
                if (slot >= 0 && slot < inv.items.size()) {
                    inv.items.set(slot, stack);
                }
            }
            return true;
        } catch (Exception e) {
            LOG.error("Falha ao restaurar inventário de {}", playerId, e);
            return false;
        }
    }

    @Override
    public boolean restoreArmor(UUID playerId, PlayerSnapshot snapshot) {
        ServerPlayer player = resolve(playerId);
        if (player == null) return false;
        try {
            Inventory inv = player.getInventory();
            inv.armor.clear();

            for (Map.Entry<String, String> entry : snapshot.serializedArmor().entrySet()) {
                int slot = Integer.parseInt(entry.getKey());
                ItemStack stack = deserializeItem(entry.getValue());
                if (slot >= 0 && slot < inv.armor.size()) {
                    inv.armor.set(slot, stack);
                }
            }

            if (!snapshot.serializedOffhand().isEmpty()) {
                inv.offhand.set(0, deserializeItem(snapshot.serializedOffhand()));
            }
            return true;
        } catch (Exception e) {
            LOG.error("Falha ao restaurar armadura de {}", playerId, e);
            return false;
        }
    }

    @Override
    public void clearInventory(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        if (player != null) {
            player.getInventory().clearContent();
        }
    }

    @Override
    public Optional<StoredLocation> captureLocation(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        if (player == null) return Optional.empty();
        return Optional.of(new StoredLocation("cobbleverse",
                player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()));
    }

    @Override
    public Map<String, String> serializeInventoryItems(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        if (player == null) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        List<ItemStack> items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                result.put(Integer.toString(i), serializeItem(stack));
            }
        }
        return result;
    }

    @Override
    public Map<String, String> serializeArmorItems(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        if (player == null) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        List<ItemStack> armor = player.getInventory().armor;
        for (int i = 0; i < armor.size(); i++) {
            ItemStack stack = armor.get(i);
            if (!stack.isEmpty()) {
                result.put(Integer.toString(i), serializeItem(stack));
            }
        }
        return result;
    }

    @Override
    public String serializeOffhandItem(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        if (player == null) return "";
        ItemStack offhand = player.getInventory().offhand.getFirst();
        return offhand.isEmpty() ? "" : serializeItem(offhand);
    }

    @Override
    public String captureGameMode(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        if (player == null) return "SURVIVAL";
        return player.gameMode.getGameModeForPlayer().name();
    }

    @Override
    public int captureTotalExperience(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.totalExperience : 0;
    }

    @Override
    public int captureExperienceLevel(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.experienceLevel : 0;
    }

    @Override
    public float captureExperienceProgress(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.experienceProgress : 0;
    }

    @Override
    public double captureHealth(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.getHealth() : 20;
    }

    @Override
    public double captureAbsorption(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.getAbsorptionAmount() : 0;
    }

    @Override
    public int captureFoodLevel(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.getFoodData().getFoodLevel() : 20;
    }

    @Override
    public float captureSaturation(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.getFoodData().getSaturationLevel() : 5;
    }

    @Override
    public boolean captureAllowFlight(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null && player.getAbilities().mayfly;
    }

    @Override
    public boolean captureIsFlying(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null && player.getAbilities().flying;
    }

    @Override
    public float captureFlySpeed(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.getAbilities().getFlyingSpeed() : 0.05f;
    }

    @Override
    public float captureWalkSpeed(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.getAbilities().getWalkingSpeed() : 0.1f;
    }

    @Override
    public int captureSelectedSlot(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.getInventory().selected : 0;
    }

    @Override
    public int captureFireTicks(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.getRemainingFireTicks() : 0;
    }

    @Override
    public float captureFallDistance(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        return player != null ? player.fallDistance : 0;
    }

    @Override
    public String captureActiveEffects(UUID playerId) {
        ServerPlayer player = resolve(playerId);
        if (player == null) return "";
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            list.add(effect.save());
        }
        tag.put("effects", list);
        return tag.toString();
    }

    private String serializeItem(ItemStack stack) {
        CompoundTag tag = (CompoundTag) stack.save(server.registryAccess());
        return tag.toString();
    }

    private ItemStack deserializeItem(String data) {
        if (data == null || data.isEmpty()) return ItemStack.EMPTY;
        try {
            Tag parsed = net.minecraft.nbt.TagParser.parseTag(data);
            if (parsed instanceof CompoundTag tag) {
                return ItemStack.parse(server.registryAccess(), tag).orElse(ItemStack.EMPTY);
            }
        } catch (Exception e) {
            LOG.warn("Falha ao desserializar item: {}", e.getMessage());
        }
        return ItemStack.EMPTY;
    }

    private void restoreEffects(ServerPlayer player, String effectsData) {
        if (effectsData == null || effectsData.isEmpty()) return;
        try {
            Tag parsed = net.minecraft.nbt.TagParser.parseTag(effectsData);
            if (parsed instanceof CompoundTag tag && tag.contains("effects")) {
                ListTag list = tag.getList("effects", 10);
                for (int i = 0; i < list.size(); i++) {
                    MobEffectInstance effect = MobEffectInstance.load(list.getCompound(i));
                    if (effect != null) {
                        player.addEffect(effect);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Falha ao restaurar efeitos: {}", e.getMessage());
        }
    }
}
