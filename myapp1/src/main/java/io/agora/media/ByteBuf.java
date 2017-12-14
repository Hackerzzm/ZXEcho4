package io.agora.media;

import java.nio.*;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Li on 10/1/2016.
 */
public class ByteBuf {
    ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

    public byte[] asBytes() {
        byte[] out = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(out, 0, out.length);
        return out;
    }

    public ByteBuf put(short v) {
        buffer.putShort(v);
        return this;
    }

    public ByteBuf put(byte[] v) {
        put((short)v.length);
        buffer.put(v);
        return this;
    }

    public ByteBuf put(int v) {
        buffer.putInt(v);
        return this;
    }

    public ByteBuf put(String v) {
        return put(v.getBytes());
    }

    public ByteBuf put(TreeMap<Short, String> extra) {
        put((short)extra.size());

        for (Map.Entry<Short, String> pair : extra.entrySet()) {
            put(pair.getKey());
            put(pair.getValue());
        }

        return this;
    }
}
