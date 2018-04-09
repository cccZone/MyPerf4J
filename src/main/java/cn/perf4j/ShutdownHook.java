package cn.perf4j;

import cn.perf4j.util.Logger;
import cn.perf4j.util.PerfStatsCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by LinShunkang on 2018/3/16
 */

/**
 * 该类用于在JVM关闭前通过调用asyncRecordProcessor把内存中的数据处理完，保证尽量不丢失采集的数据
 */
public final class ShutdownHook {

    private static AsyncPerfStatsProcessor asyncPerfStatsProcessor = AsyncPerfStatsProcessor.getInstance();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.info("ENTER ShutdownHook...");
                try {
                    Map<String, AbstractRecorder> recorderMap = RecorderContainer.getRecorderMap();
                    List<PerfStats> perfStatsList = new ArrayList<>(recorderMap.size());
                    AbstractRecorder recorder = null;
                    for (Map.Entry<String, AbstractRecorder> entry : recorderMap.entrySet()) {
                        recorder = entry.getValue();
                        perfStatsList.add(PerfStatsCalculator.calPerfStats(recorder));
                    }

                    if (recorder != null) {
                        asyncPerfStatsProcessor.process(perfStatsList, recorder.getStartTime(), recorder.getStopTime());
                    }

                    ThreadPoolExecutor executor = asyncPerfStatsProcessor.getExecutor();
                    executor.shutdown();
                    executor.awaitTermination(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Logger.info("EXIT ShutdownHook...");
                }
            }
        }));
    }
}