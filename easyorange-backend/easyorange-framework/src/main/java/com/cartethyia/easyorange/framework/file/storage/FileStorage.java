package com.cartethyia.easyorange.framework.file.storage;

import java.io.IOException;

public interface FileStorage {

    String store(byte[] content, String originalFilename, String contentType) throws IOException;

    byte[] load(String identifier) throws IOException;

    void delete(String identifier) throws IOException;

    String getUrl(String identifier);

    boolean supportsDirectUrl();
}
