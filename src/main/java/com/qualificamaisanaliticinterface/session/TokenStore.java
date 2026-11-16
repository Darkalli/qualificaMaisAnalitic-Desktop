package com.qualificamaisanaliticinterface.session;

import java.io.IOException;
import java.util.Optional;

public interface TokenStore {
    void save(String session) throws IOException;
    Optional<String> load() throws IOException;
    void clear() throws IOException;
}
