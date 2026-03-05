package com.mystichorizons.mysticnametags.integrations.permissions;

import com.hyperperms.HyperPerms;
import com.hyperperms.api.HyperPermsAPI;
import com.hyperperms.api.ChatAPI;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class HyperPermsSupport implements PermissionSupport, PrefixSupport {

    @Nullable
    private final HyperPermsAPI api;

    public HyperPermsSupport() {
        this.api = resolveApiSafely();
    }

    @Override
    public boolean isAvailable() {
        try {
            return api != null && HyperPerms.getInstance() != null && HyperPerms.getInstance().isEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean hasPermission(UUID uuid, String permissionNode) {
        if (!isAvailable()) return false;
        try {
            return api.hasPermission(uuid, permissionNode);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean grantPermission(UUID uuid, String permissionNode) {
        return setPermission(uuid, permissionNode, true);
    }

    @Override
    public boolean revokePermission(UUID uuid, String node) {
        return setPermission(uuid, node, false);
    }

    private boolean setPermission(UUID uuid, String node, boolean value) {
        if (!isAvailable()) return false;

        try {
            // Use modifyUser so it persists properly (HyperPerms handles load/save)
            CompletableFuture<Void> fut = api.getUserManager().modifyUser(uuid, user -> {
                // We avoid hard-coding User method signatures by using reflection.
                // Tries: setPermission(String, boolean) then setPermission(String, boolean, Duration?) (won't match) then unsetPermission/removePermission, etc.
                invokeSetPermission(user, node, value);
            });

            // Fire-and-forget: return true if the call was accepted.
            // If you want strict success, you can block with a timeout (not recommended in server thread).
            return fut != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void invokeSetPermission(Object user, String node, boolean value) {
        if (user == null) return;

        // 1) setPermission(String, boolean)
        if (tryInvoke(user, "setPermission", new Class<?>[]{String.class, boolean.class}, new Object[]{node, value})) {
            return;
        }

        // 2) addPermission(String) / removePermission(String)
        if (value) {
            if (tryInvoke(user, "addPermission", new Class<?>[]{String.class}, new Object[]{node})) return;
            if (tryInvoke(user, "grantPermission", new Class<?>[]{String.class}, new Object[]{node})) return;
        } else {
            if (tryInvoke(user, "removePermission", new Class<?>[]{String.class}, new Object[]{node})) return;
            if (tryInvoke(user, "unsetPermission", new Class<?>[]{String.class}, new Object[]{node})) return;
            if (tryInvoke(user, "revokePermission", new Class<?>[]{String.class}, new Object[]{node})) return;
        }

        // 3) Nothing matched – do nothing safely.
    }

    private static boolean tryInvoke(Object target, String methodName, Class<?>[] sig, Object[] args) {
        try {
            Method m = target.getClass().getMethod(methodName, sig);
            m.setAccessible(true);
            m.invoke(target, args);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public @Nullable String getPrefix(UUID uuid) {
        // ChatAPI already has its own caching and availability checks.
        try {
            if (!ChatAPI.isAvailable()) return null;
            String prefix = ChatAPI.getPrefix(uuid);
            if (prefix == null) return null;
            prefix = prefix.trim();
            return prefix.isEmpty() ? null : prefix;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String getBackendName() {
        return "HyperPerms";
    }

    private static @Nullable HyperPermsAPI resolveApiSafely() {
        try {
            HyperPerms hp = HyperPerms.getInstance();
            if (hp == null) return null;

            // Try common getter names via reflection to avoid version mismatch.
            // e.g. getApi(), getAPI(), api()
            for (String name : new String[]{"getApi", "getAPI", "api"}) {
                try {
                    Method m = hp.getClass().getMethod(name);
                    Object out = m.invoke(hp);
                    if (out instanceof HyperPermsAPI) {
                        return (HyperPermsAPI) out;
                    }
                } catch (Throwable ignored) {}
            }

            return null;
        } catch (NoClassDefFoundError e) {
            return null;
        } catch (Throwable t) {
            return null;
        }
    }
}