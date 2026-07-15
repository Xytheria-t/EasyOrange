package com.cartethyia.easyorange.framework.file.storage;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorage {

    String store(byte[] content, String originalFilename, String contentType) throws IOException;

    void delete(String identifier) throws IOException;

    Path getPath(String identifier);

    String getUrl(String identifier);
}
