package com.wraith.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Small event bus for module hooks.
 *
 * Handlers are discovered reflectively once, at register()/unregister() time, and
 * flattened into a sorted array that is reused for every dispatch - so posting an
 * event per frame costs an array read and N reflective calls, no allocation. The
 * handlers themselves are private methods on Module subclasses, which is why
 * setAccessible is needed rather than a method handle from a public lookup.
 */
public final class EventBus {

    private record Subscription(Object listener, Method method, int priority) {}

    private static final Subscription[] NONE = new Subscription[0];

    private final Map<Class<?>, List<Subscription>> pending = new HashMap<>();
    private final Map<Class<?>, Subscription[]> dispatch = new HashMap<>();

    public void register(Object listener) {
        boolean changed = false;
        for (Method method : listener.getClass().getDeclaredMethods()) {
            EventTarget target = method.getAnnotation(EventTarget.class);
            if (target == null || method.getParameterCount() != 1) continue;
            if (!Event.class.isAssignableFrom(method.getParameterTypes()[0])) continue;
            method.setAccessible(true);
            Class<?> type = method.getParameterTypes()[0];
            pending.computeIfAbsent(type, k -> new ArrayList<>()).add(new Subscription(listener, method, target.priority()));
            dispatch.remove(type);
            changed = true;
        }
        if (changed) invalidateAll();
    }

    public void unregister(Object listener) {
        boolean changed = false;
        for (List<Subscription> list : pending.values()) {
            if (list.removeIf(s -> s.listener() == listener)) changed = true;
        }
        if (changed) invalidateAll();
    }

    /** Rebuild every dispatch array once, lazily, on the next post of each type. */
    private void invalidateAll() {
        dispatch.clear();
    }

    public <T extends Event> T post(T event) {
        // Static/shared event instances are reused every frame, so the flag has to
        // start clean or one cancel would permanently mute the whole bus.
        event.setCancelled(false);
        Subscription[] subs = dispatch.computeIfAbsent(event.getClass(), this::build);
        for (Subscription sub : subs) {
            if (event.isCancelled()) break;
            try {
                sub.method().invoke(sub.listener(), event);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Wraith: " + sub.listener().getClass().getSimpleName()
                        + "#" + sub.method().getName() + " failed handling "
                        + event.getClass().getSimpleName(), e);
            }
        }
        return event;
    }

    private Subscription[] build(Class<?> type) {
        List<Subscription> list = pending.get(type);
        if (list == null || list.isEmpty()) return NONE;
        Subscription[] array = list.toArray(new Subscription[0]);
        Arrays.sort(array, Comparator.comparingInt(Subscription::priority));
        return array;
    }

    public int handlerCount() {
        return pending.values().stream().mapToInt(List::size).sum();
    }
}
