package com.kkllffaa.meteor_litematica_printer.Commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import meteordevelopment.meteorclient.commands.Command
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
import net.minecraft.world.chunk.WorldChunk

private const val SEARCH_RADIUS = 128
private val BEDROCK_Y_LEVELS = intArrayOf(4, 123)

object NetherCrackerCommand : Command(
    "nethercracker",
    "Finds bedrock at Y=4 and Y=123 in the Nether within a specified radius."
) {
    override fun build(builder: LiteralArgumentBuilder<CommandSource>) {
        builder.executes {
            val player = mc.player ?: return@executes error("Player not found.").let { SINGLE_SUCCESS }
            val world = mc.world ?: return@executes error("World not found.").let { SINGLE_SUCCESS }

            if (world.registryKey != World.NETHER) {
                error("You must be in the Nether to use this command.")
                return@executes SINGLE_SUCCESS
            }

            val centerChunkPos = world.getChunk(player.blockPos).pos
            val chunkRadius = SEARCH_RADIUS / 16

            info("Scanning chunks in a $SEARCH_RADIUS block radius ($chunkRadius chunk radius)...")

            val bedrockCandidates = buildList {
                for (r in 0..chunkRadius) {
                    val xRange = centerChunkPos.x - r..centerChunkPos.x + r
                    val zRange = centerChunkPos.z - r..centerChunkPos.z + r

                    for (chunkX in xRange) {
                        for (chunkZ in zRange) {
                            if (r > 0 && chunkX in (xRange.first + 1)..<xRange.last && chunkZ in (zRange.first + 1)..<zRange.last) {
                                continue
                            }

                            world.chunkManager.getWorldChunk(chunkX, chunkZ, false)?.let { chunk ->
                                addAll(chunk.findBedrockBlocks())
                            }
                        }
                    }
                }
            }

            info("Found ${bedrockCandidates.size} bedrock blocks at y=4 and y=123.")

            if (bedrockCandidates.isNotEmpty()) {
                val coords = bedrockCandidates.joinToString("\n") { "${it.x} ${it.y} ${it.z}" }

                val copyText = Texts.bracketed(
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

    private fun WorldChunk.findBedrockBlocks(): List<BlockPos> = buildList {
        val chunkPos = pos
        val mutablePos = BlockPos.Mutable()

        for (x in 0..15) {
            for (z in 0..15) {
                val worldX = chunkPos.startX + x
                val worldZ = chunkPos.startZ + z

                for (y in BEDROCK_Y_LEVELS) {
                    mutablePos.set(worldX, y, worldZ)
                    if (getBlockState(mutablePos).isOf(Blocks.BEDROCK)) {
                        add(mutablePos.toImmutable())
                        break
                    }
                }
            }
        }
    }
}
