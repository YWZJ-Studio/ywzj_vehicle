package org.ywzj.vehicle.vehicle.schedule;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllConfigs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs each vehicle's physics solve on a worker pool and applies the results at a per-level
 * barrier, preserving the outcome a synchronous tick order would have produced.
 */
@EventBusSubscriber(modid = YwzjVehicle.MOD_ID)
public final class VehiclePhysicsScheduler {


    private static final Map<ServerLevel, List<VehiclePhysicsJob>> IN_FLIGHT =
            new ConcurrentHashMap<>();

    private static volatile ExecutorService pool;

    private VehiclePhysicsScheduler() {}

    /** Whether launching an async solve is worth attempting at all this tick. */
    public static boolean available() {
        return AllConfigs.Cached.asyncVehiclePhysics;
    }


    public static void submit(VehiclePhysicsJob job) {
        IN_FLIGHT.computeIfAbsent((ServerLevel) job.vehicle.level(),
                level -> new CopyOnWriteArrayList<>()).add(job);
        try {
            job.future = pool().submit(job::solve);
        } catch (RejectedExecutionException shuttingDown) {
            job.future = null;
        }
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<VehiclePhysicsJob> jobs = IN_FLIGHT.get(level);
        if (jobs == null || jobs.isEmpty()) {
            return;
        }
        for (VehiclePhysicsJob job : jobs) {
            job.await();
            job.vehicle.completePhysicsJob(job);
        }
        jobs.clear();
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        for (List<VehiclePhysicsJob> jobs : IN_FLIGHT.values()) {
            if (jobs.isEmpty()) {
                continue;
            }
            for (VehiclePhysicsJob job : jobs) {
                job.await();
                job.vehicle.abandonPhysicsJob(job);
            }
            jobs.clear();
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            IN_FLIGHT.remove(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        IN_FLIGHT.clear();
        ExecutorService running = pool;
        pool = null;
        if (running != null) {
            running.shutdown();
            try {
                if (!running.awaitTermination(5, TimeUnit.SECONDS)) {
                    running.shutdownNow();
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                running.shutdownNow();
            }
        }
    }

    private static ExecutorService pool() {
        ExecutorService current = pool;
        if (current == null) {
            synchronized (VehiclePhysicsScheduler.class) {
                current = pool;
                if (current == null) {
                    current = Executors.newFixedThreadPool(poolSize(), new WorkerFactory());
                    pool = current;
                }
            }
        }
        return current;
    }

    private static int poolSize() {
        int configured = AllConfigs.Cached.physicsThreads;
        if (configured > 0) {
            return configured;
        }
        return Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    }

    private static final class WorkerFactory implements ThreadFactory {

        private final AtomicInteger index = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "ywzj-vehicle-physics-" + index.getAndIncrement());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        }

    }

}
