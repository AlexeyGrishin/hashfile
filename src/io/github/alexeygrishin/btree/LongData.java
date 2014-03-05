package io.github.alexeygrishin.btree;

public class LongData implements TreeData {

    private long value;

    public LongData(long value) {
        this.value = value;
    }

    @Override
    public long createData() {
        return value;
    }

    @Override
    public long updateData(long oldData) {
        return value;
    }
}
