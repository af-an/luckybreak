package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * Places an NBT structure file at the break position.
 *
 * Structure files live in: data/<namespace>/structures/<name>.nbt
 * They can be created in‑game with the Structure Block (/give @s structure_block).
 *
 * JSON fields:
 *   structure: "luckybreak:treasure_room"   (namespace:path, no .nbt extension)
 *   offset_x / offset_y / offset_z: integer offsets from break pos (default 0)
 */
public class SpawnStructureEvent implements LuckyEvent {

    private final Identifier structureId;
    private final int offsetX, offsetY, offsetZ;

    private SpawnStructureEvent(Identifier id, int ox, int oy, int oz) {
        this.structureId = id;
        this.offsetX = ox;
        this.offsetY = oy;
        this.offsetZ = oz;
    }

    public static SpawnStructureEvent fromJson(JsonObject obj) {
        String id = obj.get("structure").getAsString();
        int ox = obj.has("offset_x") ? obj.get("offset_x").getAsInt() : 0;
        int oy = obj.has("offset_y") ? obj.get("offset_y").getAsInt() : 0;
        int oz = obj.has("offset_z") ? obj.get("offset_z").getAsInt() : 0;
        return new SpawnStructureEvent(Identifier.parse(id), ox, oy, oz);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> template = manager.get(structureId);
        if (template.isEmpty()) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Structure not found: {}", structureId);
            return;
        }
        BlockPos placeAt = pos.offset(offsetX, offsetY, offsetZ);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        template.get().placeInWorld(level, placeAt, placeAt, settings, level.getRandom(), 2);
    }
}
