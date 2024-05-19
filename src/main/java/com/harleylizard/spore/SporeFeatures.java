package com.harleylizard.spore;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public final class SporeFeatures {
    public static final Feature<TagTreeFeatureConfiguration> TAG_TREE = new TagTreeFeature(TagTreeFeatureConfiguration.CODEC.stable());

    private SporeFeatures() {}

    public static void register() {
        register("tag_tree", TAG_TREE);
    }

    private static <T extends FeatureConfiguration> void register(String name, Feature<T> feature) {
        Registry.register(BuiltInRegistries.FEATURE, Spore.resourceLocation(name), feature);
    }
}
