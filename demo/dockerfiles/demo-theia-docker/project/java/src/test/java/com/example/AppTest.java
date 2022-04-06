package com.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppTest {

    @Test
    public void shouldAnswerWithTrue() {
        String name = "foo";
        App app = new App(name);
        assertEquals(name, app.getName());
    }
}
