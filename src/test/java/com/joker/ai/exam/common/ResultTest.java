package com.joker.ai.exam.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultTest {

    @Test
    void success_withData() {
        Result<Object> r = Result.success((Object) "x");
        assertEquals(200, r.getCode());
        assertEquals("操作成功", r.getMessage());
        assertEquals("x", r.getData());
    }

    @Test
    void error_withCustomCode() {
        Result<Void> r = Result.error(400, "bad");
        assertEquals(400, r.getCode());
        assertEquals("bad", r.getMessage());
        assertNull(r.getData());
    }
}
