package com.harleylizard.spore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public final class TagTreeFeatureConfiguration implements FeatureConfiguration {
    public static final Codec<TagTreeFeatureConfiguration> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.simpleMap(Codec.STRING, WeightedList.CODEC, Keyable.forStrings(() -> Stream.of("body", "canopy"))).fieldOf("weighted_lists").forGetter(config -> config.weightedLists),
                    BlockStateProvider.CODEC.fieldOf("log").forGetter(config -> config.log),
                    BlockStateProvider.CODEC.fieldOf("leaves").forGetter(config -> config.leaves),
                    TreeDecorator.CODEC.listOf().optionalFieldOf("decorators", List.of()).forGetter(config -> config.decorators)
            ).apply(builder, TagTreeFeatureConfiguration::new));

    private final Map<String, WeightedList> weightedLists;
    private final BlockStateProvider log;
    private final BlockStateProvider leaves;
    private final List<TreeDecorator> decorators;

    public TagTreeFeatureConfiguration(Map<String, WeightedList> weightedLists, BlockStateProvider log, BlockStateProvider leaves, List<TreeDecorator> decorators) {
        this.weightedLists = weightedLists;
        this.log = log;
        this.leaves = leaves;
        this.decorators = decorators;
    }

    public WeightedList getBodies() {
        return requireNonNull(weightedLists.get("bodies"));
    }

    public WeightedList getCanopies() {
        return requireNonNull(weightedLists.get("canopies"));
    }

    public BlockStateProvider getLog() {
        return log;
    }

    public BlockStateProvider getLeaves() {
        return leaves;
    }

    public List<TreeDecorator> getDecorators() {
        return decorators;
    }
}
