package io.github.alexeygrishin.hashfile.btreebased;

import io.github.alexeygrishin.bytestorage.SynchronizedByteContainer;
import io.github.alexeygrishin.blockalloc.serializers.Serializers;

import java.nio.ByteBuffer;

public class MetaInformationWrapper implements SynchronizedByteContainer {
    private SynchronizedByteContainer wrapped;
    private int metaBlockSize;
    private MetaInfo metaInfo;

    public MetaInformationWrapper(SynchronizedByteContainer wrapped) {
        this.wrapped = wrapped;
        this.metaBlockSize = Serializers.INSTANCE.getSize(MetaInfo.class);
        if (wrapped.getSize() < metaBlockSize) {
            metaInfo = new MetaInfo();
        }
        else {
            ByteBuffer buffer = ByteBuffer.allocate(metaBlockSize);
            wrapped.read(0, buffer);
            buffer.rewind();
            metaInfo = Serializers.INSTANCE.get(MetaInfo.class).load(buffer);
        }
    }

    public MetaInfo getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(MetaInfo newMetaInfo) {
        metaInfo = newMetaInfo;
        ByteBuffer buffer = ByteBuffer.allocate(metaBlockSize);
        Serializers.INSTANCE.get(MetaInfo.class).save(buffer, newMetaInfo);
        if (wrapped.getSize() == 0) {
            wrapped.append(buffer);
        }
        else {
            wrapped.write(0, buffer);
        }
    }

    @Override
    public void read(long position, ByteBuffer target) {
        wrapped.read(position + metaBlockSize, target);
    }

    @Override
    public void write(long position, ByteBuffer target) {
        wrapped.write(position + metaBlockSize, target);
    }

    @Override
    public long append(ByteBuffer target) {
        return wrapped.append(target) - metaBlockSize;
    }

    @Override
    public long getSize() {
        return wrapped.getSize() - metaBlockSize;
    }

    @Override
    public void close() {

        wrapped.close();
    }

    public static class MetaInfo {
        public int version = 0x01;
        public int blockSize;
        public int truncateMethod;
    }
}
