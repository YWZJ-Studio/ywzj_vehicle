package org.ywzj.vehicle.custom.sync;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 同步数据容器，在服务端自动监听数据变更，并在必要时创建同步数据包<br/>
 * @param <T> 数据类型
 */
public class SyncDataHolder<T> {
    /**
     * 客户端收到数据后的处理方法
     */
    private final SyncDataSerializer<T> serializer;
    private final Consumer<T> setter;
    private final Supplier<T> getter;
    private final int index;
    private boolean isDirty = true;

    /**
     * 最后记录的值
     */
    private T value;

    protected SyncDataHolder(
            int index,
            SyncDataSerializer<T> serializer,
            Consumer<T> setter,
            Supplier<T> getter,
            T initialValue
    ) {
        this.serializer = serializer;
        this.setter = setter;
        this.getter = getter;
        this.index = index;
        this.value = initialValue;
    }

    @SuppressWarnings("unchecked")
    protected void onUpdate(SyncDataEntry<?> entry) {
        this.value = (T) entry.value();
        this.setter.accept(this.value);
    }

    public void refresh() {
        T currentValue = getter.get();
        if (serializer.compare(this.value, currentValue)) {
            return;
        }
        this.value = currentValue;
        this.isDirty = true;
    }

    public T get() {
        return value;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public SyncDataEntry<T> createEntry() {
        return new SyncDataEntry<>(index, serializer, value);
    }
}
