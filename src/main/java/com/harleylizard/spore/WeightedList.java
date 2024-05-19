package com.harleylizard.spore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

public final class WeightedList {
    public static final Codec<WeightedList> CODEC = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<WeightedList> read(DynamicOps<T> ops, T input) {
            return ops.getStream(input).flatMap(stream -> {
                var list = stream.toList();
                if (list.stream().allMatch(t -> Entry.CODEC.parse(ops, t).result().isPresent())) {
                    var entries = list.stream().map(t -> Entry.CODEC.parse(ops, t).result().get()).toList();
                    return DataResult.success(new WeightedList(entries));
                }
                return DataResult.error(() -> "Some elements are not viable: " + input);
            });
        }

        @Override
        public <T> T write(DynamicOps<T> ops, WeightedList value) {
            return null;
        }

        @Override
        public String toString() {
            return "WeightedList";
        }
    };

    private final List<Entry> list;
    private final int weight;

    public WeightedList(List<Entry> list) {
        this.list = list;
        var i = 0;
        for (var entry : list) {
            i += entry.weight;
        }
        weight = i;
    }

    public ResourceLocation get(RandomSource source) {
        var i = source.nextInt(weight);
        var j = 0;
        for (var entry : list) {
            j += entry.weight;
            if (i < j) {
                return entry.structure;
            }
        }
        Spore.LOGGER.warn("Weighted list has improper weights, defaulted to first element.");
        return list.get(0).structure;
    }

    public StructureTemplate get(WorldGenLevel level, RandomSource source) {
        var manager = level.getLevel().getStructureManager();
        var location = get(source);
        return manager.get(location).orElseThrow(() -> {
            Spore.LOGGER.warn("Missing structure for: {}", location.toString());
            return new RuntimeException("Missing structure for: " + location);
        });
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public record Entry(ResourceLocation structure, int weight) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(builder -> builder.group(ResourceLocation.CODEC.fieldOf("structure").forGetter(Entry::structure), Codec.INT.fieldOf("weight").forGetter(Entry::weight)).apply(builder, Entry::new));
    }
}
