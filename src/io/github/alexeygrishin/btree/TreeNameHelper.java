package io.github.alexeygrishin.btree;

public interface TreeNameHelper {

    public String getFullName(long dataId);

    public String truncate(String fullName, int targetLen);

}
