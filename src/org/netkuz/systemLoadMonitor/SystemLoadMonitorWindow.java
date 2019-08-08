package org.netkuz.systemLoadMonitor;

import com.intellij.openapi.wm.ToolWindow;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.lang.management.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.TimeZone;

public class SystemLoadMonitorWindow {

    private static volatile SystemLoadMonitorWindow instance;

    private JPanel systemLoadMonitorWindowContent;
    private JProgressBar usedMemoryProgressBar;
    private JLabel initMemoryLabel;
    private JLabel usedHeapMemoryLabel;
    private JLabel maxHeapMemoryLabel;
    private JLabel commitedMemoryLabel;
    private JLabel totalLabel;
    private JProgressBar runnableProgressBar;
    private JLabel runnableLabel;
    private JProgressBar timedWaitingProgressBar;
    private JProgressBar waitingProgressBar;
    private JLabel timedWaitingLabel;
    private JLabel waitingLabel;
    private JProgressBar blockedProgressBar;
    private JLabel blockedLabel;
    private JTextPane garbageCollectorTextPane;
    private JTextPane systemTextPane;

    private SystemLoadMonitorWindow(ToolWindow toolWindow) {

        ActionListener updateAction = e ->
                updateData(ManagementFactory.getRuntimeMXBean(), ManagementFactory.getMemoryMXBean(),
                        ManagementFactory.getThreadMXBean(), ManagementFactory.getGarbageCollectorMXBeans());

        Timer t = new Timer(500, updateAction);
        t.start();
    }

    public static SystemLoadMonitorWindow getInstance(ToolWindow toolWindow) {
        SystemLoadMonitorWindow result = instance;
        if (result == null) {
            synchronized (SystemLoadMonitorWindow.class) {
                result = instance;
                if (result == null) {
                    instance = result = new SystemLoadMonitorWindow(toolWindow);
                }
            }
        }
        return instance;
    }

    private void updateData(Object...data) {
        for(Object d : data) {
            if (d instanceof RuntimeMXBean) {
                setSystemPane((RuntimeMXBean) d);
            }
            if (d instanceof MemoryMXBean) {
                setMemoryLabels((MemoryMXBean) d);
            }

            if (d instanceof ThreadMXBean) {
                int total = ((ThreadMXBean) d).getThreadCount();
                totalLabel.setText("Total: " + ((ThreadMXBean) d).getThreadCount());

                Map<String, Integer> infoThreadsMap = getThreadMap(((ThreadMXBean) d));

                setThreadLabels(total, infoThreadsMap, runnableLabel, "Runnable: ", Threads.RUNNABLE.name(), runnableProgressBar);
                setThreadLabels(total, infoThreadsMap, timedWaitingLabel, "Timed waiting: ", Threads.TIMED_WAITING.name(), timedWaitingProgressBar);
                setThreadLabels(total, infoThreadsMap, waitingLabel, "Waiting: ", Threads.WAITING.name(), waitingProgressBar);
                setThreadLabels(total, infoThreadsMap, blockedLabel, "Blocked: ", Threads.BLOCKED.name(), blockedProgressBar);
            }

            if (d instanceof List) {
                if (!((List) d).isEmpty() && ((List) d).get(0) instanceof GarbageCollectorMXBean) {
                    setGarbageCollectorPane((List<GarbageCollectorMXBean>) d);
                }
            }
        }

    }

    private void setSystemPane(RuntimeMXBean runtimeMXBean) {
        DateFormat format = new SimpleDateFormat("HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        StringBuilder sb = new StringBuilder();
        sb.append("Vendor: ")
                .append(runtimeMXBean.getVmVendor())
                .append("\n")
                .append("Version: ")
                .append(runtimeMXBean.getVmVersion())
                .append("\n")
                .append("Uptime: ")
                .append(format.format(runtimeMXBean.getUptime()));
        systemTextPane.setText(sb.toString());
    }

    private void setMemoryLabels(MemoryMXBean memoryMXBean) {
        initMemoryLabel.setText("Initial memory: " + format(memoryMXBean.getHeapMemoryUsage().getInit(), 2));
        commitedMemoryLabel.setText("Committed memory: " + format(memoryMXBean.getHeapMemoryUsage().getCommitted(), 2));
        maxHeapMemoryLabel.setText("Max heap memory: " + format(memoryMXBean.getHeapMemoryUsage().getMax(), 2));
        usedHeapMemoryLabel.setText("Used heap memory: " + format(memoryMXBean.getHeapMemoryUsage().getUsed(), 2));
        usedMemoryProgressBar.setValue((int) (((double) memoryMXBean.getHeapMemoryUsage().getUsed()) / ((double) memoryMXBean.getHeapMemoryUsage().getMax()) * 100.0));
    }

    @NotNull
    private Map<String, Integer> getThreadMap(ThreadMXBean threadMXBean) {
        Map<String, Integer> infoThreadsMap = new HashMap<>();
        infoThreadsMap.put(Threads.RUNNABLE.name(), 0);
        infoThreadsMap.put(Threads.TIMED_WAITING.name(), 0);
        infoThreadsMap.put(Threads.WAITING.name(), 0);
        infoThreadsMap.put(Threads.BLOCKED.name(), 0);
        for(long threadID : threadMXBean.getAllThreadIds()) {
            ThreadInfo info = threadMXBean.getThreadInfo(threadID);
            infoThreadsMap.computeIfPresent(info.getThreadState().toString(), (k, v) -> v += 1);
        }
        return infoThreadsMap;
    }

    private void setThreadLabels(int total, Map<String, Integer> infoThreadsMap, JLabel label, String nameLabel, String name, JProgressBar progressBar) {
        Integer threadValue = infoThreadsMap.get(name);
        label.setText(nameLabel + threadValue);
        progressBar.setMaximum(total);
        progressBar.setString(threadValue.toString());
        progressBar.setValue(threadValue);
    }

    private void setGarbageCollectorPane(List<GarbageCollectorMXBean> garbageCollectorMXBeanList) {
        StringBuilder sb = new StringBuilder();
        for (GarbageCollectorMXBean item: garbageCollectorMXBeanList) {
            sb.append(item.getName())
                    .append(": (Collections = ").append(item.getCollectionCount())
                    .append(" Total time spent = ").append(item.getCollectionTime())
                    .append(")\n");
        }
        garbageCollectorTextPane.setText(sb.toString());
    }

    private String format(double bytes, int digits) {
        String[] dictionary = { "bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        int index = 0;
        while (index < dictionary.length - 1) {
            if (bytes < 1024) {
                break;
            }
            bytes = bytes / 1024;
            index++;
        }
        return String.format("%." + digits + "f", bytes) + " " + dictionary[index];
    }

    public JPanel getContent() {
        return systemLoadMonitorWindowContent;
    }
}
