package com.github.buildcontainers.hello_world_groovy;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppTest {
    @Test
    public void testAppHasAGreeting() {
        App classUnderTest = new App();
        assertNotNull(classUnderTest.hello());
    }
}
