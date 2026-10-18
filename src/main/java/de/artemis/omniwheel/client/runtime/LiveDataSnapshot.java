package de.artemis.omniwheel.client.runtime;

import de.artemis.omniwheel.common.OmniWheelText;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Client-visible data captured together, without requesting anything from the server.
 */
public final class LiveDataSnapshot {

    private final Map<String, String> values;

    public static LiveDataSnapshot capture(Minecraft minecraft) {
        Map<String, String> values = new LinkedHashMap<>();
        net.minecraft.client.entity.player.ClientPlayerEntity player = minecraft.player;
        net.minecraft.client.world.ClientWorld level = minecraft.level;
        if (player == null || level == null)
            return new LiveDataSnapshot(values);
        putPosition(values, "", player.blockPosition());
        values.put("{position}", position(player.blockPosition()));
        values.put("{exact_position}", decimal(player.getX()) + " " + decimal(player.getY()) + " " + decimal(player.getZ()));
        values.put("{player}", player.getName().getString());
        values.put("{player_uuid}", player.getUUID().toString());
        values.put("{facing}", player.getDirection().getName());
        values.put("{yaw}", decimal(player.yRot));
        values.put("{pitch}", decimal(player.xRot));
        values.put("{health}", decimal(player.getHealth()));
        values.put("{dimension}", level.dimension().location().toString());
        net.minecraft.util.ResourceLocation biome = level.registryAccess().registryOrThrow(net.minecraft.util.registry.Registry.BIOME_REGISTRY).getKey(level.getBiome(player.blockPosition()));
        if (biome != null)
            values.put("{biome}", biome.toString());
        net.minecraft.client.multiplayer.ServerData server = minecraft.getCurrentServer();
        values.put("{server}", server == null ? OmniWheelText.translate("omniwheel.placeholder.singleplayer") : server.name);
        values.put("{profile}", OmniWheelText.resolve(OmniWheelClientRuntime.getInstance().activeProfile().displayName()));
        if (minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() != null) {
            String name = minecraft.gameMode.getPlayerMode().getName();
            String key = "selectWorld.gameMode." + name.toLowerCase(Locale.ROOT);
            values.put("{gamemode}", OmniWheelText.isTranslationKey(key) ? OmniWheelText.translate(key) : name);
        }
        if (minecraft.hitResult instanceof BlockRayTraceResult && ((BlockRayTraceResult) (minecraft.hitResult)).getType() == RayTraceResult.Type.BLOCK) {
            net.minecraft.block.BlockState state = level.getBlockState(((BlockRayTraceResult) (minecraft.hitResult)).getBlockPos());
            putPosition(values, "target_block_", ((BlockRayTraceResult) (minecraft.hitResult)).getBlockPos());
            values.put("{target_block_position}", position(((BlockRayTraceResult) (minecraft.hitResult)).getBlockPos()));
            values.put("{target_block_id}", Registry.BLOCK.getKey(state.getBlock()).toString());
            values.put("{target_block_name}", state.getBlock().getName().getString());
        }
        if (minecraft.hitResult instanceof EntityRayTraceResult) {
            net.minecraft.entity.Entity entity = ((EntityRayTraceResult) (minecraft.hitResult)).getEntity();
            putPosition(values, "target_entity_", entity.blockPosition());
            values.put("{target_entity_position}", position(entity.blockPosition()));
            values.put("{target_entity_uuid}", entity.getUUID().toString());
            values.put("{target_entity_id}", Registry.ENTITY_TYPE.getKey(entity.getType()).toString());
            values.put("{target_entity_name}", entity.getName().getString());
        }
        net.minecraft.item.ItemStack item = player.getMainHandItem();
        if (!item.isEmpty()) {
            values.put("{held_item_id}", Registry.ITEM.getKey(item.getItem()).toString());
            values.put("{held_item_name}", item.getHoverName().getString());
            values.put("{held_item_count}", Integer.toString(item.getCount()));
            values.put("{held_item_durability}", item.isDamageableItem() ? Integer.toString(Math.max(0, item.getMaxDamage() - item.getDamageValue())) : "");
        }
        return new LiveDataSnapshot(values);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String position(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static void putPosition(Map<String, String> values, String prefix, BlockPos pos) {
        values.put("{" + prefix + "x}", Integer.toString(pos.getX()));
        values.put("{" + prefix + "y}", Integer.toString(pos.getY()));
        values.put("{" + prefix + "z}", Integer.toString(pos.getZ()));
    }

    public String missingMessage(String template) {
        for (de.artemis.omniwheel.client.runtime.LiveDataValues.Value value : LiveDataValues.ALL) if (template.contains(value.token()) && !values.containsKey(value.token()))
            return value.missingMessage();
        return null;
    }

    public String resolve(String template) {
        template = omitUnavailableDurability(template);
        // Replace only tokens in the original template; inserted names cannot inject other tokens.
        java.lang.StringBuilder result = new StringBuilder();
        for (int cursor = 0; cursor < template.length(); ) {
            int open = template.indexOf('{', cursor);
            int close = open < 0 ? -1 : template.indexOf('}', open);
            if (close < 0) {
                result.append(template.substring(cursor));
                break;
            }
            result.append(template, cursor, open);
            String token = template.substring(open, close + 1);
            result.append(values.getOrDefault(token, token));
            cursor = close + 1;
        }
        return result.toString();
    }

    /**
     * Omit standalone information fields before substitution, so item names remain literal.
     */
    public String omitUnavailableDurability(String template) {
        String token = "{held_item_durability}";
        if (!"".equals(values.get(token)) || !template.contains(token))
            return template;
        java.util.ArrayList<java.lang.String> lines = new java.util.ArrayList<String>();
        for (String line : template.split("\\R", -1)) {
            java.util.ArrayList<java.lang.String> fields = new java.util.ArrayList<String>();
            boolean omitted = false;
            for (String field : line.split("\\|", -1)) {
                // A mixed field must keep its other values; its durability token simply becomes empty.
                if (field.contains(token) && !field.replace(token, "").contains("{"))
                    omitted = true;
                else
                    fields.add(field);
            }
            if (!omitted)
                lines.add(line);
            else if (!fields.isEmpty())
                lines.add(fields.stream().map(de.artemis.omniwheel.common.Java8::strip).collect(java.util.stream.Collectors.joining(" | ")));
        }
        return String.join(template.contains("\r\n") ? "\r\n" : "\n", lines);
    }

    public LiveDataSnapshot(Map<String, String> values) {
        values = de.artemis.omniwheel.common.Java8.copyMap(values);
        this.values = values;
    }

    public Map<String, String> values() {
        return values;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        LiveDataSnapshot other = (LiveDataSnapshot) value;
        return true && java.util.Objects.equals(values, other.values);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(values);
    }

    @Override
    public String toString() {
        return "LiveDataSnapshot[" + "values=" + values + "]";
    }
}
