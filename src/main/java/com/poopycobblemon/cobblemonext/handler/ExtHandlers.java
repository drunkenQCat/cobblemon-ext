package com.poopycobblemon.cobblemonext.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 通用 handler 注册表：把「订阅 ExtEvents + 自行过滤 + 自行隔离异常」收敛为
 * 声明式注册。功能代码只写「条件 + 动作」，生命周期归本表管理——
 * 每次注册返回 {@link Registration}，可随时撤销；{@link #unregisterAll()}
 * 供服务器关闭等场景一次性清理。
 *
 * <pre>{@code
 * Registration senna = ExtHandlers.subscribe(ExtEvents.MOVE_USED,
 *         event -> isHoldingSenna(event.user()),
 *         event -> applySpeedDrop(event));
 * ...
 * senna.unregister();
 * }</pre>
 *
 * <p>线程安全：内部包装器为无状态闭包 + {@link AtomicBoolean}，注册列表本身
 * 沿用事件总线的并发实现。过滤器或动作抛出的异常都会被隔离记录，
 * 不影响同一条总线上的其他订阅者。
 */
public final class ExtHandlers {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtHandlers.class);

    /** 全部仍存活的注册（含一次性已触发的），unregisterAll 用 */
    private static final Set<Registration> LIVE = new CopyOnWriteArraySet<>();

    private ExtHandlers() {
    }

    /** 订阅：每当事件通过 filter 就执行 action */
    public static <E> Registration subscribe(List<Consumer<E>> bus, Predicate<E> filter, Consumer<E> action) {
        return doSubscribe(bus, filter, action, false);
    }

    /** 一次性订阅：首次通过 filter 的事件执行 action 后自动注销 */
    public static <E> Registration subscribeOnce(List<Consumer<E>> bus, Predicate<E> filter, Consumer<E> action) {
        return doSubscribe(bus, filter, action, true);
    }

    /** 无条件订阅（filter 恒真） */
    public static <E> Registration subscribe(List<Consumer<E>> bus, Consumer<E> action) {
        return doSubscribe(bus, event -> true, action, false);
    }

    /** 撤销本表登记的全部订阅（一般仅在服务器关闭等收尾场景调用） */
    public static void unregisterAll() {
        for (Registration registration : LIVE) {
            registration.unregister();
        }
        LIVE.clear();
    }

    /** 当前仍存活的注册数量（测试与诊断用） */
    public static int liveCount() {
        return LIVE.size();
    }

    private static <E> Registration doSubscribe(List<Consumer<E>> bus, Predicate<E> filter,
                                                Consumer<E> action, boolean once) {
        AtomicBoolean active = new AtomicBoolean(true);
        // 包装器先于句柄创建，once 触发时通过此持有者反向调用句柄注销
        List<Registration> self = new java.util.ArrayList<>(1);
        Consumer<E> wrapper = event -> {
            if (!active.get()) {
                return;
            }
            boolean matched;
            try {
                matched = filter.test(event);
            } catch (Throwable t) {
                LOGGER.error("[cobblemon-ext] handler 过滤器异常，本次按不匹配处理", t);
                return;
            }
            if (!matched) {
                return;
            }
            if (once) {
                if (self.isEmpty()) {
                    active.set(false); // 句柄尚未就绪（极端并发），退化为仅失效
                } else {
                    self.get(0).unregister(); // 置失效并从总线摘除，动作内再触发也不会重入
                }
            }
            try {
                action.accept(event);
            } catch (Throwable t) {
                LOGGER.error("[cobblemon-ext] handler 执行异常", t);
            }
        };
        bus.add(wrapper);

        Registration registration = new Registration() {
            @Override
            public boolean isActive() {
                return active.get();
            }

            @Override
            public void unregister() {
                if (active.compareAndSet(true, false)) {
                    bus.remove(wrapper);
                }
                LIVE.remove(this);
            }
        };
        self.add(registration);
        LIVE.add(registration);
        return registration;
    }
}
