package com.boydti.fawe.cloudburst.adapter;

import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.cloudburst.CloudburstAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class LazyCompoundTag extends CompoundTag {

    private final Supplier<NbtMap> nmsTag;

    public LazyCompoundTag(Supplier<NbtMap> tag) {
        super(null);
        this.nmsTag = tag;
    }

    public LazyCompoundTag(NbtMap tag) {
        this(() -> tag);
    }

    public NbtMap get() {
        return nmsTag.get();
    }

    @Override
    public Map<String, Tag> getValue() {
        Map<String, Tag> value = super.getValue();
        if (value == null) {
            CompoundTag tag = CloudburstAdapter.adapt(nmsTag.get());
            setValue(tag.getValue());
        }
        return super.getValue();
    }

    public boolean containsKey(String key) {
        return nmsTag.get().containsKey(key);
    }

    public byte[] getByteArray(String key) {
        return nmsTag.get().getByteArray(key);
    }

    public byte getByte(String key) {
        return nmsTag.get().getByte(key);
    }

    public double getDouble(String key) {
        return nmsTag.get().getDouble(key);
    }

    public double asDouble(String key) {
        Object value = nmsTag.get().get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0;
    }

    public float getFloat(String key) {
        return nmsTag.get().getFloat(key);
    }

    public int[] getIntArray(String key) {
        return nmsTag.get().getIntArray(key);
    }

    public int getInt(String key) {
        return nmsTag.get().getInt(key);
    }

    public int asInt(String key) {
        Object value = nmsTag.get().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    public List<Tag> getList(String key) {
        Object tag = nmsTag.get().get(key);
        if (tag instanceof NbtList) {
            ArrayList<Tag> list = new ArrayList<>();
            NbtList nbtList = (NbtList) tag;
            for (Object elem : nbtList) {
                if (elem instanceof NbtMap) {
                    list.add(new LazyCompoundTag((NbtMap) elem));
                } else {
                    list.add(CloudburstAdapter.adaptTag(elem));
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    public ListTag getListTag(String key) {
        Object tag = nmsTag.get().get(key);
        if (tag instanceof NbtList) {
            return CloudburstAdapter.adaptTag(tag);
        }
        return new ListTag(StringTag.class, Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Tag> List<T> getList(String key, Class<T> listType) {
        ListTag listTag = getListTag(key);
        if (listTag.getType().equals(listType)) {
            return (List<T>) listTag.getValue();
        } else {
            return Collections.emptyList();
        }
    }

    public long[] getLongArray(String key) {
        return nmsTag.get().getLongArray(key);
    }

    public long getLong(String key) {
        return nmsTag.get().getLong(key);
    }

    public long asLong(String key) {
        Object value = nmsTag.get().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0;
    }

    public short getShort(String key) {
        return nmsTag.get().getShort(key);
    }

    public String getString(String key) {
        return nmsTag.get().getString(key);
    }

    @Override
    public String toString() {
        return nmsTag.get().toString();
    }
}
