package com.kkllffaa.meteor_litematica_printer.Commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.block.Blocks
import net.minecraft.command.CommandSource
import net.minecraft.text.ClickEvent.CopyToClipboard
import net.minecraft.text.HoverEvent.ShowText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.chunk.WorldChunk
import kotlin.collections.ArrayList

object NetherCrackerCommand : meteordevelopment.meteorclient.commands.Command(
    "nethercracker",
    "Finds bedrock at Y=4 and Y=123 in the Nether within a specified radius."
) {
    override fun build(builder: LiteralArgumentBuilder<CommandSource>) {
        builder.executes {
            val player = mc.player?: run {
                error("Player not found.")
                return@executes SINGLE_SUCCESS
            }
            val world = mc.world?: run {
                error("World not found.")
                return@executes SINGLE_SUCCESS
            }
            if (world.registryKey != World.NETHER) {
                error("You must be in the Nether to use this command.")
                return@executes SINGLE_SUCCESS
            }

            val playerPos = player.blockPos
            val centerChunkPos = world.getChunk(playerPos).getPos()

            val bedrockCandidates: MutableList<BlockPos> = ArrayList()

            val chunkRadius: Int = (SEARCH_RADIUS shr 4) + 1

            info("Scanning chunks in a %d block radius (%d chunk radius)...", SEARCH_RADIUS, chunkRadius)

            for (r in 0..<chunkRadius) {
                for (chunkX in centerChunkPos.x - r..centerChunkPos.x + r) {
                    for (chunkZ in centerChunkPos.z - r..centerChunkPos.z + r) {
                        if (r > 0 && (chunkX > centerChunkPos.x - r && chunkX < centerChunkPos.x + r) && (chunkZ > centerChunkPos.z - r && chunkZ < centerChunkPos.z + r)) {
                            continue
                        }

                        val chunk: Chunk = world.getChunk(chunkX, chunkZ)

                        if (chunk is WorldChunk) {
                            addBedrockBlocks(chunk, bedrockCandidates)
                        }
                    }
                }
            }

            info(String.format("Found %d bedrock blocks at y=4 and y=123.", bedrockCandidates.size))

            if (!bedrockCandidates.isEmpty()) {
                val sb = StringBuilder()
                for (pos in bedrockCandidates) {
                    sb.append(String.format("%d %d %d\n", pos.x, pos.y, pos.z))
                }
                val coords: String = sb.toString().trim { it <= ' ' }

                val copyText: Text = Texts.bracketed(
                    Text.literal("Copy Coords")
                        .fillStyle(
                            Style.EMPTY
                                .withColor(Formatting.GREEN)
                                .withClickEvent(CopyToClipboard(coords))
                                .withHoverEvent(ShowText(Text.literal("Click to copy all coordinates")))
                                .withInsertion(coords)
                        )
                )

                info(copyText)
            }
            SINGLE_SUCCESS
        }
    }

    private const val SEARCH_RADIUS = 128

    private fun addBedrockBlocks(chunk: WorldChunk, blockCandidates: MutableList<BlockPos>) {
        val mutablePos = BlockPos.Mutable()
        val chunkPos = chunk.getPos()

        for (x in 0..15) {
            for (z in 0..15) {
                val worldX = chunkPos.startX + x
                val worldZ = chunkPos.startZ + z

                mutablePos.set(worldX, 4, worldZ)
                if (chunk.getBlockState(mutablePos).isOf(Blocks.BEDROCK)) {
                    blockCandidates.add(mutablePos.toImmutable())
                    continue
                }

                mutablePos.set(worldX, 123, worldZ)
                if (chunk.getBlockState(mutablePos).isOf(Blocks.BEDROCK)) {
                    blockCandidates.add(mutablePos.toImmutable())
                }
            }
        }
    }

}
