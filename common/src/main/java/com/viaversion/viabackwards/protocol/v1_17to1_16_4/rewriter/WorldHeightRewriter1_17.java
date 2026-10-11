/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
 *
 * ViaBackwards is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ViaBackwards is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.v1_17to1_16_4.rewriter;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;

public final class WorldHeightRewriter1_17 {

    private static final int LEGACY_WORLD_HEIGHT = 256;

    private WorldHeightRewriter1_17() {
    }

    public static int worldBaseY() {
        return ViaBackwards.getConfig().worldBase1_16();
    }

    public static int toClientY(int serverY) {
        return serverY - worldBaseY();
    }

    public static int toServerY(int clientY) {
        return clientY + worldBaseY();
    }

    public static boolean isClientY(int y) {
        return y >= 0 && y < LEGACY_WORLD_HEIGHT;
    }

    public static BlockPosition toClientPosition(BlockPosition position) {
        return new BlockPosition(position.x(), toClientY(position.y()), position.z());
    }

    public static BlockPosition toServerPosition(BlockPosition position) {
        return new BlockPosition(position.x(), toServerY(position.y()), position.z());
    }

    public static void shiftBlockPositionToClient(PacketWrapper wrapper, int index) {
        wrapper.set(Types.BLOCK_POSITION1_14, index, toClientPosition(wrapper.get(Types.BLOCK_POSITION1_14, index)));
    }

}