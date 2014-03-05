package io.github.alexeygrishin.btree;

public interface TreeNameHelper {

    public String getFullName(int dataId);

    public String truncate(String fullName, int targetLen);

}
