package org.ywzj.vehicle.custom.sync;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface SyncDataSerializer<T> {
    void write(FriendlyByteBuf buf, T value);
    T read(FriendlyByteBuf buf);
    boolean compare(T a, T b);

    static <T> SyncDataEntry<?> readEntry(FriendlyByteBuf buf, SyncDataSerializer<T> serializer, int index) {
        T value = serializer.read(buf);
        return new SyncDataEntry<>(index, serializer, value);
    }

    static <T> SyncDataSerializer<T> create(
            @NotNull BiConsumer<FriendlyByteBuf, T> writer,
            @NotNull Function<FriendlyByteBuf, T> reader,
            @NotNull BiFunction<T, T, Boolean> comparator
    ) {
        return new SyncDataSerializer<>() {
            @Override
            public void write(FriendlyByteBuf buf, T value) {
                writer.accept(buf, value);
            }

            @Override
            public T read(FriendlyByteBuf buf) {
                return reader.apply(buf);
            }

            @Override
            public boolean compare(T a, T b) {
                return comparator.apply(a, b);
            }
        };
    }
}
