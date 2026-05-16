package org.tavall.platform.global.abstracts;

import org.tavall.logging.Log;
import org.tavall.enums.*;
import org.tavall.internal.event.EventContext;
import org.tavall.internal.event.EventSerializer;
import org.tavall.internal.event.tags.interfaces.EventListener;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AbstractEvent {
    private final EventContext context = new EventContext();
    private final EnumSet<EventCapability> capabilities = EnumSet.noneOf(EventCapability.class);
    public long completedAt;
    private boolean async = false;
    private boolean cancelled = false;
    private boolean cancellable = true;
    private boolean suppressible = true;
    private boolean suppress = false;
    private boolean retryable = false;
    public boolean fired = false;
    public boolean completed = false;
    private final boolean failed = false;
    private final Throwable lastErrorLocal = null;
    private final Throwable lastErrorGlobal = null;
    private ScheduledFuture<?> scheduledReset;
    private String webhookUrl;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AbstractEvent-Scheduler");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool();
    private final List<EventListener<?>> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<AbstractEvent>> middleware = new CopyOnWriteArrayList<>();



    public boolean isCancelled() {
        return cancelled;
    }

    public AbstractEvent setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        return this;
    }
    public boolean isCancellable() {
        return cancellable;
    }
    public boolean isRetryable() {
        return retryable;
    }
    
    public AbstractEvent setRetryable(boolean retryable){
        this.retryable = retryable;
        return this;
    }

    public AbstractEvent setCancelled() {
        if (isCancelable()) {
            capabilities.add(EventCapability.CANCELLING_REQUESTED);
        } else {
            warnInvalidStateChange("cancelled", null);
        }
        return this;
    }
    public boolean isAsync() {
        return async;
    }

    public AbstractEvent setAsync(boolean async) {
        this.async = async;
        return this;
    }

    public AbstractEvent setSuppressible(boolean suppressable) {
        this.suppressible = suppressable;
        if (!suppressable) resetSuppression();
        return this;
    }


    public boolean shouldSuppress() {
        return suppressible && suppress;
    }

    public void markSuppressed() {
        markSuppressed(30);
    }

    public void markSuppressed(int seconds) {
        if (!suppressible) return;

        suppress = true;
        if (scheduledReset != null && !scheduledReset.isDone()) {
            scheduledReset.cancel(false);
        }

        scheduledReset = scheduler.schedule(() -> {
            suppress = false;
            scheduledReset = null;
        }, seconds, TimeUnit.SECONDS);
    }

    public void resetSuppression() {
        suppress = false;
        if (scheduledReset != null) {
            scheduledReset.cancel(false);
            scheduledReset = null;
        }
    }

    public AbstractEvent tag(EventTag tag) {
        context.getTags().add(tag);
        return this;
    }

    public boolean hasTag(EventTag tag) {
        return context.getTags().contains(tag);
    }
    public boolean has(String key) {
        return context.containsKey(key);
    }
    public Set<EventTag> getTags() {
        return context.getTags();
    }



    public EventStatus getStatus() {
        return context.getStatus();
    }

    public AbstractEvent setStatus(EventStatus status) {
        context.setStatus(status);
        return this;
    }
    public AbstractEvent setDomain(EventDomain status) {
        context.setDomain(status);
        return this;
    }

    public EventPriority getPriority() {
        return context.getPriority();
    }

    public AbstractEvent setPriority(EventPriority priority) {
        context.setPriority(priority);
        return this;
    }

    public AbstractEvent setSource(EventSource source) {
        context.setSource(source);
        return this;
    }
    public AbstractEvent setTags(EventTag tag) {
        context.setTags(tag);
        return this;
    }

    public Object getSource() {
        return context.getSource();
    }


    public Set<EventCapability> getCapabilities() {
        return context.getCapabilities();
    }


    public void applyMiddleware() {
        for (Consumer<AbstractEvent> mw : middleware) {
            mw.accept(this);
        }
    }

    public String serialize() {
        return EventSerializer.serialize(this);
    }

    public AbstractEvent deserialize(String json) {
        return EventSerializer.deserialize(json);
    }

    public void registerMiddleware(Consumer<AbstractEvent> mw) {
        middleware.add(mw);
    }

    public void shutdownScheduler() {
        scheduler.shutdownNow();
    }

    public synchronized void await() throws InterruptedException {
        wait();
    }

    public synchronized void await(long millis) throws InterruptedException {
        wait(millis);
    }

    public synchronized void awaitSeconds(int seconds) throws InterruptedException {
        wait(seconds * 1000L);
    }

    public synchronized void signal() {
        notify();
    }

    public synchronized void signalAll() {
        notifyAll();
    }

    public synchronized void waitUntil(Predicate<AbstractEvent> condition) throws InterruptedException {
        while (!condition.test(this)) {
            wait();
        }
    }

    public synchronized void waitUntil(Predicate<AbstractEvent> condition, long timeoutMillis) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        while (!condition.test(this) && System.currentTimeMillis() < endTime) {
            wait(50);
        }
    }
    public boolean isSuppressible() {
        return capabilities.contains(EventCapability.SUPPRESSIBLE);
    }

    public boolean isCancelable() {
        return capabilities.contains(EventCapability.CANCELABLE);
    }
    public boolean hasCapability(EventCapability capability) {
        return context.getCapabilities().contains(capability);
    }

    public AbstractEvent enableCapability(EventCapability capability) {
        context.getCapabilities().add(capability);
        return this;
    }

    public AbstractEvent enableCapabilities(EventCapability... capabilities) { //new
        for (EventCapability cap : capabilities) {
            context.getCapabilities().add(cap);
        }
        return this;
    }

    public AbstractEvent disableCapabilities(EventCapability... capability) {
        for (EventCapability cap : capabilities) {
            context.getCapabilities().add(cap);
        }
        return this;
    }
    public AbstractEvent isDatabase() {
        return tag(EventTag.DATABASE);
    }

    public AbstractEvent isGameplay() {
        return tag(EventTag.GAMEPLAY);
    }

    public AbstractEvent isCore() {
        return tag(EventTag.CORE);
    }

    public AbstractEvent isVelocity() {
        return tag(EventTag.VELOCITY);
    }

    public AbstractEvent isPaper() {
        return tag(EventTag.PAPER);
    }
    public AbstractEvent isAPI(){
        return tag(EventTag.API);
    }

    public AbstractEvent isWeb() { return tag(EventTag.WEB); }
    public AbstractEvent isCrossRegion() { return tag(EventTag.CROSS_REGION); }
    public AbstractEvent isLocalOnly() { return tag(EventTag.LOCAL); }



    public void beforeFire() {
        // Optional hook for any setup before an event is fired
    }

    public void onFire() {
        // Optional hook for after an event is fired
    }
    private void logSettingChange(String field, Object value) {
        String info = "[Event Info] " + this.getClass().getSimpleName() + " Ã¢â€ â€™ " + field + " set to " + value;
        System.out.println(info);
    }

    private void warnInvalidStateChange(String method, String reason) {
        String warning = "[Event Warning] " + this.getClass().getSimpleName() + "." + method + "() was called but the new value is invalid. This change was ignored.";
        System.err.println(warning);
    }
    public void logLifecycle() {
        StringBuilder builder = new StringBuilder();
        builder.append("[Event Lifecycle] ").append(this.getClass().getSimpleName());

        builder.append("\n > Tags: ").append(getTags());
        builder.append("\n > Async: ").append(isAsync());
        builder.append("\n > Cancelled: ").append(isCancelled());
        builder.append("\n > Suppressable: ").append(isSuppressible());
        builder.append("\n > Suppressed: ").append(shouldSuppress());

        System.out.println(builder);
    }

    public void logException(Throwable ex) {
        Log.warn("[Event Error] " + this.getClass().getSimpleName() + " threw an exception:");
        for (StackTraceElement element : ex.getStackTrace()) {
            String className = element.getClassName();
            if (!className.startsWith("java.") && !className.startsWith("sun.") && !className.startsWith("jdk.")) {
                Log.warn(" --> at " + element);
                break;
            }
        }
        Log.warn(" > Message: " + ex.getMessage());
        Log.warn(" > Cause: " + ex.getCause());

    }



}

