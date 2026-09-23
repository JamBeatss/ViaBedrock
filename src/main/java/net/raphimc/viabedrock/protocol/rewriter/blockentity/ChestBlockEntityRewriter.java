/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viabedrock.protocol.rewriter.blockentity;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import net.raphimc.viabedrock.api.chunk.BedrockBlockEntity;
import net.raphimc.viabedrock.api.chunk.BlockEntityWithBlockState;
import net.raphimc.viabedrock.api.model.BlockState;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;

/**
 * Bedrock stores the double chest pairing in the block entity (pairx/pairz), Java stores it in the block state (type=left/right).
 */
public class ChestBlockEntityRewriter extends LootableContainerBlockEntityRewriter {

    @Override
    public BlockEntity toJava(UserConnection user, BedrockBlockEntity bedrockBlockEntity) {
        final BlockEntity javaBlockEntity = super.toJava(user, bedrockBlockEntity);
        final CompoundTag bedrockTag = bedrockBlockEntity.tag();
        if (!bedrockTag.contains("pairx") || !bedrockTag.contains("pairz")) {
            return javaBlockEntity;
        }
        final int dx = bedrockTag.getInt("pairx") - bedrockBlockEntity.position().x();
        final int dz = bedrockTag.getInt("pairz") - bedrockBlockEntity.position().z();
        final String partnerDirection;
        if (dx == 1 && dz == 0) partnerDirection = "east";
        else if (dx == -1 && dz == 0) partnerDirection = "west";
        else if (dx == 0 && dz == 1) partnerDirection = "south";
        else if (dx == 0 && dz == -1) partnerDirection = "north";
        else return javaBlockEntity;

        final int javaBlockState = user.get(ChunkTracker.class).getJavaBlockState(bedrockBlockEntity.position());
        final BlockState state = BedrockProtocol.MAPPINGS.getJavaBlockStates().inverse().get(javaBlockState);
        if (state == null || !state.properties().containsKey("type") || !state.properties().containsKey("facing")) {
            return javaBlockEntity;
        }
        final String facing = state.properties().get("facing");
        final String type;
        if (partnerDirection.equals(clockwise(facing))) type = "left"; // Java: LEFT connects on the clockwise side of the facing
        else if (partnerDirection.equals(counterClockwise(facing))) type = "right";
        else return javaBlockEntity;

        final Integer pairedState = BedrockProtocol.MAPPINGS.getJavaBlockStates().get(state.replaceProperty("type", type));
        if (pairedState == null) {
            return javaBlockEntity;
        }
        return new BlockEntityWithBlockState(javaBlockEntity, pairedState);
    }

    private static String clockwise(final String facing) {
        return switch (facing) {
            case "north" -> "east";
            case "east" -> "south";
            case "south" -> "west";
            case "west" -> "north";
            default -> "";
        };
    }

    private static String counterClockwise(final String facing) {
        return switch (facing) {
            case "north" -> "west";
            case "west" -> "south";
            case "south" -> "east";
            case "east" -> "north";
            default -> "";
        };
    }

}
