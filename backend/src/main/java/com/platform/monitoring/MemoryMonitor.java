package com.platform.monitoring;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.GarbageCollectorMXBean;
import java.util.List;

@Component
@Slf4j
public class MemoryMonitor {

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    private long lastHeapUsed = 0;
    private long lastGcCount = 0;

    @Scheduled(fixedRate = 60000) // каждую минуту
    public void logMemoryUsage() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

        long heapUsedMB = heap.getUsed() / (1024 * 1024);
        long heapMaxMB = heap.getMax() / (1024 * 1024);
        long heapCommittedMB = heap.getCommitted() / (1024 * 1024);
        long nonHeapUsedMB = nonHeap.getUsed() / (1024 * 1024);

        double heapPercent = (double) heap.getUsed() / heap.getMax() * 100;

        // Расчет утечки памяти (рост heap без GC)
        long heapGrowth = heap.getUsed() - lastHeapUsed;
        lastHeapUsed = heap.getUsed();

        // GC статистика
        long totalGcCount = gcBeans.stream().mapToLong(gc -> gc.getCollectionCount()).sum();
        long gcDelta = totalGcCount - lastGcCount;
        lastGcCount = totalGcCount;

        log.info("MEMORY: Heap={}/{}MB ({}%), Committed={}MB, NonHeap={}MB, PendingFinalization={}, HeapGrowth={}KB, GCs={}",
            heapUsedMB, heapMaxMB, String.format("%.1f", heapPercent),
            heapCommittedMB, nonHeapUsedMB, memoryBean.getObjectPendingFinalizationCount(),
            heapGrowth / 1024, gcDelta);

        // Предупреждения
        if (heapPercent > 80) {
            log.warn("⚠️ High memory usage: {}%", String.format("%.1f", heapPercent));
        }

        if (heapPercent > 90) {
            log.error("🚨 CRITICAL: Memory usage above 90%!");
        }

        // Обнаружение возможной утечки
        if (heapGrowth > 50 * 1024 * 1024 && gcDelta == 0) { // рост >50MB без GC
            log.warn("⚠️ Potential memory leak detected! Heap grew by {}MB without GC", heapGrowth / (1024 * 1024));
        }
    }

    @Scheduled(fixedRate = 300000) // каждые 5 минут
    public void forceGCIfNeeded() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        double heapPercent = (double) heap.getUsed() / heap.getMax() * 100;

        if (heapPercent > 85) {
            log.info("High memory usage ({}%), forcing GC...", String.format("%.1f", heapPercent));
            System.gc();

            // Повторная проверка через 5 секунд
            try {
                Thread.sleep(5000);
                MemoryUsage afterGc = memoryBean.getHeapMemoryUsage();
                double afterPercent = (double) afterGc.getUsed() / afterGc.getMax() * 100;
                log.info("After GC: {}% -> {}%", String.format("%.1f", heapPercent), String.format("%.1f", afterPercent));

                if (afterPercent > heapPercent - 5) {
                    log.warn("⚠️ GC didn't free significant memory. Check for strong references!");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public String getMemoryReport() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        return String.format("Heap: %dMB/%dMB (%.1f%%), NonHeap: %dMB, GC count: %d",
            heap.getUsed() / (1024 * 1024),
            heap.getMax() / (1024 * 1024),
            (double) heap.getUsed() / heap.getMax() * 100,
            memoryBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024),
            gcBeans.stream().mapToLong(gc -> gc.getCollectionCount()).sum()
        );
    }
}