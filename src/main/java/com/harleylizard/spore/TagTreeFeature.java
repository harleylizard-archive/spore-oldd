package com.harleylizard.spore;

import com.harleylizard.spore.mixin.StructureTemplateAccessor;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public final class TagTreeFeature extends Feature<TagTreeFeatureConfiguration> {

    public TagTreeFeature(Codec<TagTreeFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<TagTreeFeatureConfiguration> featurePlaceContext) {
        var configuration = featurePlaceContext.config();
        var level = featurePlaceContext.level();
        var random = featurePlaceContext.random();
        var blockPos = featurePlaceContext.origin();
        if (!canSupportTree(level, blockPos)) {
            return false;
        }

        var logs = new HashSet<BlockPos>();
        var leaves = new HashSet<BlockPos>();

        var log = configuration.getLog();
        var leavesProvider = configuration.getLeaves();

        var bodies = configuration.getBodies();
        if (bodies.isEmpty()) {
            throw new IllegalArgumentException("At least one tree body is required.");
        }
        var body = bodies.get(level, random);
        var highestLog = placeBody(level, body, blockPos, logs, leaves, log, leavesProvider, random);

        var canopies = configuration.getCanopies();
        if (!canopies.isEmpty()) {
            var canopy = canopies.get(level, random);
            placeCanopy(level, body.getSize().getY(), highestLog, canopy, blockPos, logs, leaves, log, leavesProvider, random);
        }

        applyDecorators(level, random, configuration,
                Collections.unmodifiableSet(logs),
                Collections.unmodifiableSet(leaves));
        return true;
    }

    private static BlockPos placeBody(WorldGenLevel level, StructureTemplate template, BlockPos blockPos, Set<BlockPos> logs, Set<BlockPos> leaves, BlockStateProvider log, BlockStateProvider leavesProvider, RandomSource source) {
        var settings = new StructurePlaceSettings().setRotation(Rotation.getRandom(source));

        var palette = getPalette(settings, template, blockPos);
        var size = template.getSize(settings.getRotation());

        for (var info : palette.blocks(Blocks.RED_WOOL)) {
            var pos = calculateRotated(size, settings, info, blockPos);
            setBlock(level, pos, log.getState(source, pos));
            logs.add(pos);

            if (info.pos().getY() < 1) {
                for (var below = pos.below(); canLogReplace(level, below); below = below.below()) {
                    setBlock(level, below, log.getState(source, below));
                    logs.add(below);
                }
            }
        }
        for (var info : palette.blocks()) {
            if (!info.state().is(BlockTags.LEAVES)) {
                continue;
            }
            var pos = calculateRotated(size, settings, info, blockPos);
            if (canReplace(level, pos)) {
                setBlock(level, pos, leavesProvider.getState(source, pos));
                leaves.add(pos);
            }
        }
        return calculateRotated(size, settings, highestBlockPos(palette), blockPos).above();
    }

    private static void placeCanopy(WorldGenLevel level, int height, BlockPos highestLog, StructureTemplate template, BlockPos blockPos, Set<BlockPos> logs, Set<BlockPos> leaves, BlockStateProvider log, BlockStateProvider leavesProvider, RandomSource source) {
        var settings = new StructurePlaceSettings().setRotation(Rotation.getRandom(source));

        var palette = getPalette(settings, template, blockPos);
        var size = template.getSize(settings.getRotation());
        if (size.getX() % 2 == 0) {
            size = size.offset(-1, 0, 0);
        }
        if (size.getZ() % 2 == 0) {
            size = size.offset(0, 0, -1);
        }

        for (var info : palette.blocks(Blocks.RED_WOOL)) {
            var pos = calculateRotated(size, settings, info, highestLog);

            setBlock(level, pos, log.getState(source, pos));
            logs.add(pos);
        }
        for (var info : palette.blocks()) {
            if (!info.state().is(BlockTags.LEAVES)) {
                continue;
            }
            var pos = calculateRotated(size, settings, info, highestLog);

            if (canReplace(level, pos)) {
                setBlock(level, pos, leavesProvider.getState(source, pos));
                leaves.add(pos);
            }
        }
    }

    private static void applyDecorators(WorldGenLevel level, RandomSource source, TagTreeFeatureConfiguration configuration, Set<BlockPos> logs, Set<BlockPos> leaves) {
        BiConsumer<BlockPos, BlockState> consumer = (blockPos, blockState) -> setBlock(level, blockPos, blockState);

        var roots = Set.<BlockPos>of();
        var context = new TreeDecorator.Context(level, consumer, source, logs, leaves, roots);
        for (var decorator : configuration.getDecorators()) {
            decorator.place(context);
        }
    }

    private static void setBlock(WorldGenLevel level, BlockPos blockPos, BlockState blockState) {
        level.setBlock(blockPos, blockState, 2);
    }

    private static StructureTemplate.Palette getPalette(StructurePlaceSettings settings, StructureTemplate template, BlockPos blockPos) {
        return settings.getRandomPalette(((StructureTemplateAccessor) template).spore_palettes(), blockPos);
    }

    private static boolean canSupportTree(WorldGenLevel level, BlockPos blockPos) {
        var blockState = level.getBlockState(blockPos.below());
        return blockState.isFaceSturdy(level, blockPos.below(), Direction.UP);
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos blockPos) {
        return level.isEmptyBlock(blockPos) || level.getBlockState(blockPos).is(BlockTags.REPLACEABLE_BY_TREES);
    }

    private static boolean canLogReplace(WorldGenLevel level, BlockPos blockPos) {
        var blockState = level.getBlockState(blockPos);
        return level.isEmptyBlock(blockPos) || blockState.is(Blocks.WATER) || blockState.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    private static BlockPos calculateRotated(Vec3i size, StructurePlaceSettings settings, StructureTemplate.StructureBlockInfo info, BlockPos blockPos) {
        return calculateRotated(size, settings, info.pos(), blockPos);
    }

    private static BlockPos calculateRotated(Vec3i size, StructurePlaceSettings settings, BlockPos pos, BlockPos blockPos) {
        var x = size.getX() / 2;
        var y = 0;
        var z = size.getZ() / 2;
        return StructureTemplate.calculateRelativePosition(settings, pos.offset(-x, y, -z)).offset(blockPos);
    }

    private static BlockPos highestBlockPos(StructureTemplate.Palette palette) {
        var blockPos = new BlockPos(0, 0, 0);
        for (var info : palette.blocks(Blocks.RED_WOOL)) {
            var pos = info.pos();
            if (pos.getY() > blockPos.getY()) {
                blockPos = pos;
            }
        }
        return blockPos;
    }
}
