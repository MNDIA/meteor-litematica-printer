package com.kkllffaa.meteor_litematica_printer.Functions;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.util.math.BlockPos;

public class BlockPosUtils {

    /**
     * 计算两个BlockPos之间的曼哈顿距离，不预防整数溢出
     * @param pos1 第一个位置
     * @param pos2 第二个位置
     * @return 曼哈顿距离
     */
    public static int getManhattanDistance(BlockPos pos1, BlockPos pos2) {
        return Math.abs(pos1.getX() - pos2.getX()) + 
               Math.abs(pos1.getY() - pos2.getY()) + 
               Math.abs(pos1.getZ() - pos2.getZ());
    }

    public static double getDistanceFromPosCenterToPlayerEyes(BlockPos pos) {
        return  Utils.distance(
            mc.player.getX(), 
            mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), 
            mc.player.getZ(), 
            pos.getX() + 0.5, 
            pos.getY() + 0.5, 
            pos.getZ() + 0.5
        );
    }

}