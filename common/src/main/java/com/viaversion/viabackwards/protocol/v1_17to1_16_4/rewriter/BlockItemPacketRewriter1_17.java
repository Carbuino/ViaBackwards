/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_17to1_16_4.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.LongArrayTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.api.rewriters.MapColorRewriter;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.Protocol1_17To1_16_4;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.data.MapColorMappings1_16_4;
import com.viaversion.viabackwards.protocol.v1_17_1to1_17.Protocol1_17_1To1_17;
import com.viaversion.viabackwards.protocol.v1_17_1to1_17.storage.InventoryStateIds;
import com.viaversion.viabackwards.protocol.v1_17_1to1_17.storage.ProtocolStorables1_17_1;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.storage.PlayerLastCursorItem;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.storage.ProtocolStorables1_17;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_16_2;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_17;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ClientboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.CompactArrayUtil;
import com.viaversion.viaversion.util.MathUtil;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class BlockItemPacketRewriter1_17 extends BackwardsItemRewriter<ClientboundPackets1_17, ServerboundPackets1_16_2, Protocol1_17To1_16_4> {

    private static final int BEDROCK_BLOCK_STATE = 33;

    public BlockItemPacketRewriter1_17(Protocol1_17To1_16_4 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_SHORT_ARRAY);
    }

    @Override
    protected void registerPackets() {
        new RecipeRewriter<>(protocol).register(ClientboundPackets1_17.UPDATE_RECIPES);

        protocol.registerServerbound(ServerboundPackets1_16_2.EDIT_BOOK, wrapper -> handleItemToServer(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)));
        protocol.registerServerbound(ServerboundPackets1_16_2.PLAYER_ACTION, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Action
            shiftServerboundBlockPosition(wrapper);
            wrapper.passthrough(Types.UNSIGNED_BYTE); // Face
        });
        protocol.registerServerbound(ServerboundPackets1_16_2.USE_ITEM_ON, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Hand
            shiftServerboundBlockPosition(wrapper);
            wrapper.passthrough(Types.VAR_INT); // Face
            wrapper.passthrough(Types.FLOAT); // Cursor X
            wrapper.passthrough(Types.FLOAT); // Cursor Y
            wrapper.passthrough(Types.FLOAT); // Cursor Z
            wrapper.passthrough(Types.BOOLEAN); // Inside block
        });
        protocol.registerServerbound(ServerboundPackets1_16_2.BLOCK_ENTITY_TAG_QUERY, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Transaction ID
            shiftServerboundBlockPosition(wrapper);
        });
        protocol.registerServerbound(ServerboundPackets1_16_2.SET_COMMAND_BLOCK, wrapper -> {
            shiftServerboundBlockPosition(wrapper);
            wrapper.passthrough(Types.STRING); // Command
            wrapper.passthrough(Types.VAR_INT); // Mode
            wrapper.passthrough(Types.BYTE); // Flags
        });
        protocol.registerServerbound(ServerboundPackets1_16_2.SET_STRUCTURE_BLOCK, wrapper -> {
            shiftServerboundBlockPosition(wrapper);
            wrapper.passthrough(Types.VAR_INT); // Action
            wrapper.passthrough(Types.VAR_INT); // Mode
            wrapper.passthrough(Types.STRING); // Name
            wrapper.passthrough(Types.BYTE); // Offset X
            wrapper.passthrough(Types.BYTE); // Offset Y
            wrapper.passthrough(Types.BYTE); // Offset Z
            wrapper.passthrough(Types.BYTE); // Size X
            wrapper.passthrough(Types.BYTE); // Size Y
            wrapper.passthrough(Types.BYTE); // Size Z
            wrapper.passthrough(Types.VAR_INT); // Mirror
            wrapper.passthrough(Types.VAR_INT); // Rotation
            wrapper.passthrough(Types.STRING); // Metadata
            wrapper.passthrough(Types.FLOAT); // Integrity
            wrapper.passthrough(Types.VAR_LONG); // Seed
            wrapper.passthrough(Types.BYTE); // Flags
        });
        protocol.registerServerbound(ServerboundPackets1_16_2.SET_JIGSAW_BLOCK, wrapper -> {
            shiftServerboundBlockPosition(wrapper);
            wrapper.passthrough(Types.STRING); // Name
            wrapper.passthrough(Types.STRING); // Target
            wrapper.passthrough(Types.STRING); // Pool
            wrapper.passthrough(Types.STRING); // Final state
            wrapper.passthrough(Types.STRING); // Joint type
            wrapper.passthrough(Types.VAR_INT); // Selection priority
            wrapper.passthrough(Types.VAR_INT); // Placement priority
        });
        protocol.registerServerbound(ServerboundPackets1_16_2.JIGSAW_GENERATE, wrapper -> {
            shiftServerboundBlockPosition(wrapper);
            wrapper.passthrough(Types.VAR_INT); // Levels
            wrapper.passthrough(Types.BOOLEAN); // Keep jigsaws
        });
        protocol.registerServerbound(ServerboundPackets1_16_2.SIGN_UPDATE, wrapper -> {
            shiftServerboundBlockPosition(wrapper);
            wrapper.passthrough(Types.STRING); // Line 1
            wrapper.passthrough(Types.STRING); // Line 2
            wrapper.passthrough(Types.STRING); // Line 3
            wrapper.passthrough(Types.STRING); // Line 4
        });

        // TODO Since the carried and modified items are typically set incorrectly, the server sends unnecessary
        // set slot packets after practically every window click, since it thinks the client and server
        // inventories are desynchronized. Ideally, we want to store a replica of each container state, and update
        // it appropriately for both serverbound and clientbound window packets, then fill in the carried
        // and modified items array as appropriate here. That would be a ton of work and replicated vanilla code,
        // and the hack below mitigates the worst side effects of this issue, which is an incorrect carried item
        // sent to the client when a right/left click drag is started. It works, at least for now...
        protocol.replaceServerbound(ServerboundPackets1_16_2.CONTAINER_CLICK, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BYTE);
                handler(wrapper -> {
                    short slot = wrapper.passthrough(Types.SHORT); // Slot
                    byte button = wrapper.passthrough(Types.BYTE); // Button
                    wrapper.read(Types.SHORT); // Action id - removed
                    int mode = wrapper.passthrough(Types.VAR_INT); // Mode
                    Item clicked = handleItemToServer(wrapper.user(), wrapper.read(Types.ITEM1_13_2)); // Clicked item

                    // The 1.17 client would check the entire inventory for changes before -> after a click
                    // and send the changed slots here. We include the clicked slot so the server
                    // detects any desync (e.g. cancelled InventoryClickEvent) and sends corrections.
                    if (slot >= 0 && clicked != null) {
                        wrapper.write(Types.VAR_INT, 1); // One modified slot
                        wrapper.write(Types.SHORT, slot); // The clicked slot
                        wrapper.write(Types.ITEM1_13_2, (Item) null); // Predicted: empty (item picked up)
                    } else {
                        wrapper.write(Types.VAR_INT, 0); // Empty array of slot+item
                    }

                    // Non-PICKUP modes (shift-click, swap, throw, drag, etc.) affect multiple slots
                    // that we can't easily predict. Force a state ID mismatch so the server
                    // sends a full inventory resync instead of individual slot corrections.
                    if (mode != 0) {
                        ProtocolStorables1_17_1 storables1_17_1 = wrapper.user().storables(Via.getManager().getProtocolManager().getProtocol(Protocol1_17_1To1_17.class));
                        InventoryStateIds stateIds = storables1_17_1.inventoryStateIds();
                        if (stateIds != null) {
                            stateIds.setStateId(wrapper.get(Types.BYTE, 0), -1);
                        }
                    }

                    ProtocolStorables1_17 storables = wrapper.user().storables(protocol);
                    PlayerLastCursorItem state = storables.playerLastCursorItem();
                    if (mode == 0 && button == 0 && clicked != null) {
                        // Left click PICKUP
                        // Carried item will (usually) be the entire clicked stack
                        state.setLastCursorItem(clicked);
                    } else if (mode == 0 && button == 1 && clicked != null) {
                        // Right click PICKUP
                        // Carried item will (usually) be half of the clicked stack (rounding up)
                        // if the clicked slot is empty, otherwise it will (usually) be the whole clicked stack
                        if (state.isSet()) {
                            state.setLastCursorItem(clicked);
                        } else {
                            state.setLastCursorItem(clicked, (clicked.amount() + 1) / 2);
                        }
                    } else if (mode == 5 && slot == -999 && (button == 0 || button == 4)) {
                        // Start drag (left or right click)
                        // Preserve guessed carried item and forward to server
                        // This mostly fixes the click and drag ghost item issue

                        // no-op
                    } else {
                        // Carried item unknown (TODO)
                        state.setLastCursorItem(null);
                    }

                    Item carried = state.getLastCursorItem();
                    if (carried == null) {
                        // Expected is the carried item after clicking, old clients send the clicked one (*mostly* being the same)
                        wrapper.write(Types.ITEM1_13_2, clicked);
                    } else {
                        wrapper.write(Types.ITEM1_13_2, carried);
                    }
                });
            }
        });

        protocol.replaceClientbound(ClientboundPackets1_17.CONTAINER_SET_SLOT, wrapper -> {
            byte windowId = wrapper.passthrough(Types.BYTE);
            short slot = wrapper.passthrough(Types.SHORT);

            Item carried = wrapper.read(Types.ITEM1_13_2);
            if (carried != null && windowId == -1 && slot == -1) {
                // This is related to the hack to fix click and drag ghost items above.
                // After a completed drag, we have no idea how many items remain on the cursor,
                // and vanilla logic replication would be required to calculate the value.
                // When the click drag complete packet is sent, we will send an incorrect
                // carried item, and the server will helpfully send this packet allowing us
                // to update the internal state. This is necessary for fixing multiple sequential
                // click drag actions without intermittent pickup actions.
                ProtocolStorables1_17 storables = wrapper.user().storables(protocol);
                storables.playerLastCursorItem().setLastCursorItem(carried);
            }

            wrapper.write(Types.ITEM1_13_2, handleItemToClient(wrapper.user(), carried));
        });

        protocol.registerServerbound(ServerboundPackets1_16_2.CONTAINER_ACK, null, wrapper -> {
            wrapper.cancel();
            if (!ViaBackwards.getConfig().handlePingsAsInvAcknowledgements()) {
                return;
            }

            // Handle ping packet replacement
            byte inventoryId = wrapper.read(Types.BYTE);
            short confirmationId = wrapper.read(Types.SHORT);
            boolean accepted = wrapper.read(Types.BOOLEAN);
            if (inventoryId == 0 && accepted) {
                PacketWrapper pongPacket = wrapper.create(ServerboundPackets1_17.PONG);
                pongPacket.write(Types.INT, (int) confirmationId);
                pongPacket.sendToServer(Protocol1_17To1_16_4.class);
            }
        });

        protocol.replaceClientbound(ClientboundPackets1_17.LEVEL_PARTICLES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Particle id
                map(Types.BOOLEAN); // Long distance
                map(Types.DOUBLE); // X
                map(Types.DOUBLE); // Y
                map(Types.DOUBLE); // Z
                map(Types.FLOAT); // Offset X
                map(Types.FLOAT); // Offset Y
                map(Types.FLOAT); // Offset Z
                map(Types.FLOAT); // Particle data
                map(Types.INT); // Particle count
                handler(wrapper -> {
                    int id = wrapper.get(Types.INT, 0);
                    if (id == 16) {
                        wrapper.passthrough(Types.FLOAT); // R
                        wrapper.passthrough(Types.FLOAT); // G
                        wrapper.passthrough(Types.FLOAT); // B
                        wrapper.passthrough(Types.FLOAT); // Scale

                        // Dust color transition -> Dust
                        wrapper.read(Types.FLOAT); // R
                        wrapper.read(Types.FLOAT); // G
                        wrapper.read(Types.FLOAT); // B
                    } else if (id == 37) {
                        // Vibration signal - no nice mapping possible without tracking entity positions and doing particle tasks
                        wrapper.set(Types.INT, 0, -1);
                        wrapper.cancel();
                    }
                });
                handler(protocol.getParticleRewriter().levelParticlesHandler1_13(Types.INT));
                handler(wrapper -> wrapper.set(Types.DOUBLE, 1,
                    wrapper.get(Types.DOUBLE, 1) - WorldHeightRewriter1_17.worldBaseY()));
            }
        });

        protocol.mergePacket(ClientboundPackets1_17.SET_BORDER_SIZE, ClientboundPackets1_16_2.SET_BORDER, 0);
        protocol.mergePacket(ClientboundPackets1_17.SET_BORDER_LERP_SIZE, ClientboundPackets1_16_2.SET_BORDER, 1);
        protocol.mergePacket(ClientboundPackets1_17.SET_BORDER_CENTER, ClientboundPackets1_16_2.SET_BORDER, 2);
        protocol.mergePacket(ClientboundPackets1_17.INITIALIZE_BORDER, ClientboundPackets1_16_2.SET_BORDER, 3);
        protocol.mergePacket(ClientboundPackets1_17.SET_BORDER_WARNING_DELAY, ClientboundPackets1_16_2.SET_BORDER, 4);
        protocol.mergePacket(ClientboundPackets1_17.SET_BORDER_WARNING_DISTANCE, ClientboundPackets1_16_2.SET_BORDER, 5);

        // The Great Shrunkening
        // Chunk sections *will* be lost ¯\_(ツ)_/¯
        protocol.registerClientbound(ClientboundPackets1_17.LIGHT_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // X
                map(Types.VAR_INT); // Z
                map(Types.BOOLEAN); // Trust edges
                handler(wrapper -> {
                    EntityTracker tracker = wrapper.user().getEntityTracker(protocol);
                    int startFromSection = (WorldHeightRewriter1_17.worldBaseY() - tracker.currentMinY()) >> 4;

                    long[] skyLightMask = wrapper.read(Types.LONG_ARRAY_PRIMITIVE);
                    long[] blockLightMask = wrapper.read(Types.LONG_ARRAY_PRIMITIVE);
                    int cutSkyLightMask = cutLightMask(skyLightMask, startFromSection);
                    int cutBlockLightMask = cutLightMask(blockLightMask, startFromSection);
                    wrapper.write(Types.VAR_INT, cutSkyLightMask);
                    wrapper.write(Types.VAR_INT, cutBlockLightMask);

                    long[] emptySkyLightMask = wrapper.read(Types.LONG_ARRAY_PRIMITIVE);
                    long[] emptyBlockLightMask = wrapper.read(Types.LONG_ARRAY_PRIMITIVE);
                    wrapper.write(Types.VAR_INT, cutLightMask(emptySkyLightMask, startFromSection));
                    wrapper.write(Types.VAR_INT, cutLightMask(emptyBlockLightMask, startFromSection));

                    writeLightArrays(wrapper, BitSet.valueOf(skyLightMask), cutSkyLightMask, startFromSection);
                    writeLightArrays(wrapper, BitSet.valueOf(blockLightMask), cutBlockLightMask, startFromSection);
                });
            }

            private void writeLightArrays(PacketWrapper wrapper, BitSet bitMask, int cutBitMask, int startFromSection) {
                int packetContentsLength = wrapper.read(Types.VAR_INT);
                int read = 0;
                List<byte[]> light = new ArrayList<>();

                for (int section = bitMask.nextSetBit(0); section >= 0 && read < packetContentsLength;
                     section = bitMask.nextSetBit(section + 1)) {
                    byte[] data = wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
                    read++;
                    int clientSection = section - startFromSection;
                    if (clientSection >= 0 && clientSection < 18 && isSet(cutBitMask, clientSection)) {
                        light.add(data);
                    }
                }

                if (read != packetContentsLength) {
                    for (int i = read; i < packetContentsLength; i++) {
                        wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
                    }
                }

                // Aaand we're done
                for (byte[] bytes : light) {
                    wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, bytes);
                }
            }

            private boolean isSet(int mask, int i) {
                return (mask & (1 << i)) != 0;
            }
        });

        protocol.replaceClientbound(ClientboundPackets1_17.SECTION_BLOCKS_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.LONG); // Chunk pos
                map(Types.BOOLEAN); // Suppress light updates
                handler((wrapper) -> {
                    long chunkPos = wrapper.get(Types.LONG, 0);
                    int serverSectionY = (int) (chunkPos << 44 >> 44);
                    int clientSectionY = serverSectionY - (WorldHeightRewriter1_17.worldBaseY() >> 4);
                    if (clientSectionY < 0 || clientSectionY > 15) {
                        wrapper.cancel();
                        return;
                    }
                    wrapper.set(Types.LONG, 0, (chunkPos & ~0xFFFFFL) | ((long) clientSectionY & 0xFFFFF));

                    BlockChangeRecord[] records = wrapper.passthrough(Types.VAR_LONG_BLOCK_CHANGE_ARRAY);
                    for (BlockChangeRecord record : records) {
                        if (ViaBackwards.getConfig().bedrockAtY0() && clientSectionY == 0 && record.getSectionY() == 0) {
                            record.setBlockId(BEDROCK_BLOCK_STATE);
                        } else {
                            record.setBlockId(protocol.getMappingData().getNewBlockStateId(record.getBlockId()));
                        }
                    }
                });
            }
        });

        protocol.replaceClientbound(ClientboundPackets1_17.BLOCK_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14);
                map(Types.VAR_INT);
                handler((wrapper) -> {
                    BlockPosition serverPosition = wrapper.get(Types.BLOCK_POSITION1_14, 0);
                    int clientY = WorldHeightRewriter1_17.toClientY(serverPosition.y());
                    if (!WorldHeightRewriter1_17.isClientY(clientY)) {
                        wrapper.cancel();
                        return;
                    }
                    wrapper.set(Types.BLOCK_POSITION1_14, 0, new BlockPosition(serverPosition.x(), clientY, serverPosition.z()));

                    if (ViaBackwards.getConfig().bedrockAtY0() && clientY == 0) {
                        wrapper.set(Types.VAR_INT, 0, BEDROCK_BLOCK_STATE);
                    } else {
                        wrapper.set(Types.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(wrapper.get(Types.VAR_INT, 0)));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.LEVEL_CHUNK, wrapper -> {
            EntityTracker tracker = wrapper.user().getEntityTracker(protocol);
            int currentWorldSectionHeight = tracker.currentWorldSectionHeight();
            int worldBaseY = WorldHeightRewriter1_17.worldBaseY();

            Chunk chunk = wrapper.read(new ChunkType1_17(currentWorldSectionHeight));
            wrapper.write(ChunkType1_16_2.TYPE, chunk);

            int startFromSection = (worldBaseY - tracker.currentMinY()) >> 4;
            ChunkSection[] sourceSections = chunk.getSections();
            int[] sourceBiomeData = chunk.getBiomeData();
            ChunkSection[] sections = new ChunkSection[16];
            int[] biomeData = new int[16 * 64];
            for (int section = 0; section < 16; section++) {
                int sourceSection = startFromSection + section;
                if (sourceSection >= 0 && sourceSection < sourceSections.length) {
                    sections[section] = sourceSections[sourceSection];
                }

                int sourceBiomeIndex = sourceSection * 64;
                if (sourceBiomeData != null && sourceBiomeIndex >= 0 && sourceBiomeIndex < sourceBiomeData.length) {
                    System.arraycopy(sourceBiomeData, sourceBiomeIndex, biomeData, section * 64,
                        Math.min(64, sourceBiomeData.length - sourceBiomeIndex));
                }
            }
            chunk.setSections(sections);
            chunk.setBiomeData(biomeData);
            chunk.setBitmask(cutMask(chunk.getChunkMask(), startFromSection, false));
            chunk.setChunkMask(null);

            CompoundTag heightMaps = chunk.getHeightMap();
            for (Tag heightMapTag : heightMaps.values()) {
                if (!(heightMapTag instanceof final LongArrayTag heightMap)) {
                    continue; // Client can handle bad data
                }

                int[] heightMapData = new int[256];
                int bitsPerEntry = MathUtil.ceilLog2((currentWorldSectionHeight << 4) + 1);
                CompactArrayUtil.iterateCompactArrayWithPadding(bitsPerEntry, heightMapData.length, heightMap.getValue(),
                    (i, v) -> heightMapData[i] = MathUtil.clamp(v + tracker.currentMinY() - worldBaseY, 0, 255));
                heightMap.setValue(CompactArrayUtil.createCompactArrayWithPadding(9, heightMapData.length, i -> heightMapData[i]));
            }

            protocol.getBlockRewriter().handleChunk(chunk);

            if (ViaBackwards.getConfig().bedrockAtY0()) {
                final ChunkSection lowestSection = chunk.getSections()[0];
                if (lowestSection != null) {
                    final DataPalette blocks = lowestSection.palette(PaletteType.BLOCKS);
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            blocks.setIdAt(x, 0, z, BEDROCK_BLOCK_STATE);
                        }
                    }
                }
            }

            chunk.getBlockEntities().removeIf(compound -> {
                NumberTag tag = compound.getNumberTag("y");
                if (tag == null) {
                    return false;
                }

                final int y = WorldHeightRewriter1_17.toClientY(tag.asInt());
                compound.putInt("y", y);
                return !WorldHeightRewriter1_17.isClientY(y) || (ViaBackwards.getConfig().bedrockAtY0() && y == 0);
            });
        });

        protocol.registerClientbound(ClientboundPackets1_17.BLOCK_ENTITY_DATA, wrapper -> {
            BlockPosition position = wrapper.passthrough(Types.BLOCK_POSITION1_14);
            int clientY = WorldHeightRewriter1_17.toClientY(position.y());
            if (!WorldHeightRewriter1_17.isClientY(clientY) || (ViaBackwards.getConfig().bedrockAtY0() && clientY == 0)) {
                wrapper.cancel();
                return;
            }
            wrapper.set(Types.BLOCK_POSITION1_14, 0, new BlockPosition(position.x(), clientY, position.z()));
            wrapper.passthrough(Types.VAR_INT); // Block entity type
            shiftBlockEntityY(wrapper.passthrough(Types.NAMED_COMPOUND_TAG));
        });

        protocol.registerClientbound(ClientboundPackets1_17.BLOCK_DESTRUCTION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                handler(wrapper -> {
                    BlockPosition position = wrapper.passthrough(Types.BLOCK_POSITION1_14);
                    int clientY = WorldHeightRewriter1_17.toClientY(position.y());
                    if (!WorldHeightRewriter1_17.isClientY(clientY) || (ViaBackwards.getConfig().bedrockAtY0() && clientY == 0)) {
                        wrapper.cancel();
                        return;
                    }
                    wrapper.set(Types.BLOCK_POSITION1_14, 0, new BlockPosition(position.x(), clientY, position.z()));
                });
            }
        });

        protocol.appendClientbound(ClientboundPackets1_17.BLOCK_EVENT,
            wrapper -> shiftClientboundBlockPosition(wrapper, ClientboundPackets1_17.BLOCK_EVENT));
        protocol.appendClientbound(ClientboundPackets1_17.BLOCK_BREAK_ACK,
            wrapper -> shiftClientboundBlockPosition(wrapper, ClientboundPackets1_17.BLOCK_BREAK_ACK));
        protocol.appendClientbound(ClientboundPackets1_17.LEVEL_EVENT,
            wrapper -> shiftClientboundBlockPosition(wrapper, ClientboundPackets1_17.LEVEL_EVENT));
        protocol.appendClientbound(ClientboundPackets1_17.OPEN_SIGN_EDITOR,
            wrapper -> shiftClientboundBlockPosition(wrapper, ClientboundPackets1_17.OPEN_SIGN_EDITOR));

        protocol.appendClientbound(ClientboundPackets1_17.SOUND, wrapper -> shiftPositionedSound(wrapper, false));
        protocol.appendClientbound(ClientboundPackets1_17.CUSTOM_SOUND, wrapper -> shiftPositionedSound(wrapper, true));

        protocol.registerClientbound(ClientboundPackets1_17.MAP_ITEM_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Map ID
                map(Types.BYTE); // Scale
                handler(wrapper -> wrapper.write(Types.BOOLEAN, true)); // Tracking position
                map(Types.BOOLEAN); // Locked
                handler(wrapper -> {
                    final int iconCount;
                    final boolean hasMarkers = wrapper.read(Types.BOOLEAN);
                    if (hasMarkers) {
                        iconCount = wrapper.passthrough(Types.VAR_INT);
                    } else {
                        wrapper.write(Types.VAR_INT, 0); // Add icon count
                        iconCount = 0;
                    }
                    MapColorRewriter.rewriteMapColors(wrapper, MapColorMappings1_16_4::getMappedColor, iconCount);
                });
            }
        });
    }

    private int cutLightMask(long[] mask, int startFromSection) {
        if (mask.length == 0) return 0;
        return cutMask(BitSet.valueOf(mask), startFromSection, true);
    }

    private int cutMask(BitSet mask, int startFromSection, boolean lightMask) {
        int cutMask = 0;
        int sectionCount = lightMask ? 18 : 16;
        for (int clientSection = 0; clientSection < sectionCount; clientSection++) {
            int serverSection = startFromSection + clientSection;
            if (serverSection >= 0 && mask.get(serverSection)) {
                cutMask |= (1 << clientSection);
            }
        }
        return cutMask;
    }

    private void shiftBlockEntityY(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        NumberTag yTag = tag.getNumberTag("y");
        if (yTag != null) {
            tag.putInt("y", WorldHeightRewriter1_17.toClientY(yTag.asInt()));
        }
    }

    private void shiftServerboundBlockPosition(PacketWrapper wrapper) {
        BlockPosition position = wrapper.read(Types.BLOCK_POSITION1_14);
        wrapper.write(Types.BLOCK_POSITION1_14, WorldHeightRewriter1_17.toServerPosition(position));
    }

    private void shiftClientboundBlockPosition(PacketWrapper wrapper, ClientboundPackets1_17 packet) {
        wrapper.resetReader();
        if (packet == ClientboundPackets1_17.LEVEL_EVENT) {
            wrapper.passthrough(Types.INT);
        }

        BlockPosition position = wrapper.read(Types.BLOCK_POSITION1_14);
        int clientY = WorldHeightRewriter1_17.toClientY(position.y());
        if (!WorldHeightRewriter1_17.isClientY(clientY)
            || (ViaBackwards.getConfig().bedrockAtY0() && clientY == 0)) {
            wrapper.cancel();
            return;
        }

        wrapper.write(Types.BLOCK_POSITION1_14, new BlockPosition(position.x(), clientY, position.z()));
        if (packet == ClientboundPackets1_17.BLOCK_EVENT) {
            wrapper.passthrough(Types.UNSIGNED_BYTE); // Action
            wrapper.passthrough(Types.UNSIGNED_BYTE); // Parameter
            wrapper.passthrough(Types.VAR_INT); // Block type
        } else if (packet == ClientboundPackets1_17.BLOCK_BREAK_ACK) {
            wrapper.passthrough(Types.VAR_INT); // Block state
        } else if (packet == ClientboundPackets1_17.LEVEL_EVENT) {
            wrapper.passthrough(Types.INT); // Data
            wrapper.passthrough(Types.BOOLEAN); // Disable relative volume
        }
    }

    private void shiftPositionedSound(PacketWrapper wrapper, boolean customSound) {
        wrapper.resetReader();
        if (customSound) {
            wrapper.passthrough(Types.STRING);
            wrapper.passthrough(Types.VAR_INT);
        } else {
            wrapper.passthrough(Types.VAR_INT); // Sound ID
            wrapper.passthrough(Types.VAR_INT); // Source
        }
        wrapper.passthrough(Types.INT); // X
        int y = wrapper.read(Types.INT);
        wrapper.write(Types.INT, (int) (y - (long) WorldHeightRewriter1_17.worldBaseY() * 8));
        wrapper.passthrough(Types.INT); // Z
        wrapper.passthrough(Types.FLOAT); // Volume
        wrapper.passthrough(Types.FLOAT); // Pitch
    }
}
