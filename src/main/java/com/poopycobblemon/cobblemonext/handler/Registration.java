package com.poopycobblemon.cobblemonext.handler;

/**
 * 一次 handler 注册的句柄：持有它可以在运行期撤销订阅。
 * <p>注销是幂等的；注销后句柄永久失效（isActive 恒为 false）。
 */
public interface Registration {

    /** 是否仍在生效（未注销且未过期） */
    boolean isActive();

    /** 撤销订阅（幂等，可重复调用） */
    void unregister();

    /** 一个恒不生效的空句柄 */
    Registration INACTIVE = new Registration() {
        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public void unregister() {
        }
    };
}
