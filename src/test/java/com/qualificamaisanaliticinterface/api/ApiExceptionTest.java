package com.qualificamaisanaliticinterface.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApiExceptionTest {
    @Test
    void shouldPreserveStatusCode(){
        ApiException exception = new ApiException(400, "Mensagem de erro de teste");
        Assertions.assertEquals(400, exception.getStatusCode());
    }
    @Test
    void shouldPreserveMessage(){
        ApiException exception = new ApiException(400, "Teste");
        Assertions.assertEquals("Teste", exception.getMessage());
    }
}
