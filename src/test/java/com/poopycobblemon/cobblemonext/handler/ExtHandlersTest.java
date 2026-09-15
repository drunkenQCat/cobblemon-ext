package com.poopycobblemon.cobblemonext.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ExtHandlers 注册表生命周期与隔离语义（纯 JVM） */
class ExtHandlersTest {

    private final List<Consumer<String>> bus = new CopyOnWriteArrayList<>();
    private final List<String> seen = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        ExtHandlers.unregisterAll();
    }

    private void fire(String value) {
        bus.forEach(consumer -> consumer.accept(value));
    }

    @Test
    void filterDecidesDelivery() {
        ExtHandlers.subscribe(bus, event -> event.startsWith("s"), seen::add);
        fire("senna");
        fire("other");
        assertEquals(List.of("senna"), seen);
    }

    @Test
    void onceFiresSingleTime() {
        Registration registration = ExtHandlers.subscribeOnce(bus, event -> true, seen::add);
        assertTrue(registration.isActive());
        fire("a");
        fire("b");
        assertFalse(registration.isActive());
        assertEquals(List.of("a"), seen);
        assertEquals(0, ExtHandlers.liveCount()); // 触发后自动注销
        assertTrue(bus.isEmpty()); // 包装器已从总线摘除
    }

    @Test
    void unregisterStopsDeliveryAndIsIdempotent() {
        Registration registration = ExtHandlers.subscribe(bus, event -> true, seen::add);
        assertTrue(registration.isActive());
        registration.unregister();
        registration.unregister(); // 幂等
        assertFalse(registration.isActive());
        fire("x");
        assertTrue(seen.isEmpty());
    }

    @Test
    void actionExceptionDoesNotPropagate() {
        ExtHandlers.subscribe(bus, event -> true, event -> {
            throw new IllegalStateException("boom");
        });
        ExtHandlers.subscribe(bus, event -> true, seen::add); // 后注册的仍应执行
        fire("go");
        assertEquals(List.of("go"), seen);
    }

    @Test
    void filterExceptionTreatedAsNoMatch() {
        ExtHandlers.subscribe(bus, event -> {
            throw new IllegalStateException("filter boom");
        }, seen::add);
        fire("go");
        assertTrue(seen.isEmpty());
    }

    @Test
    void unregisterAllClearsEverything() {
        assertEquals(0, ExtHandlers.liveCount());
        ExtHandlers.subscribe(bus, event -> true, seen::add);
        ExtHandlers.subscribeOnce(bus, event -> true, seen::add);
        assertEquals(2, ExtHandlers.liveCount());
        ExtHandlers.unregisterAll();
        assertEquals(0, ExtHandlers.liveCount());
        assertTrue(bus.isEmpty());
        fire("x");
        assertTrue(seen.isEmpty());
    }
}
