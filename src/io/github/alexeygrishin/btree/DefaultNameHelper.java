package io.github.alexeygrishin.btree;

public class DefaultNameHelper implements TreeNameHelper {
    @Override
    public String getFullName(long dataId) {
        throw new UnsupportedOperationException("There is no storage defined for long names");
    }

    @Override
    public String truncate(String fullName, int targetLen) {
        return Truncate.leading(fullName, targetLen);
    }
}
