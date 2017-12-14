package io.agora.media;

/**
 * Created by Li on 10/1/2016.
 */
public interface Packable {
    public ByteBuf marshall(ByteBuf out);
}
