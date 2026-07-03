package com.threethan.browser.diagnostics;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.threethan.browser.BuildConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class AppDiagnostics {
    private static final String TAG = "AppDiagnostics";
    private static final String FILE_NAME = "runtime_events.log";
    private static final String LAST_STATE_FILE_NAME = "last_runtime_state.txt";
    private static final int MAX_EVENTS = 80;
    private static final int MAX_MESSAGE_LENGTH = 320;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA);

    private AppDiagnostics() {
    }

    public static synchronized void record(Context context, String event, String message) {
        if (context == null) return;

        List<String> lines = readLines(context);
        lines.add(formatLine(context, event, message));
        while (lines.size() > MAX_EVENTS) {
            lines.remove(0);
        }
        writeLines(context, lines);
    }

    public static synchronized void recordProcessStart(Context context) {
        if (context == null) return;

        String lastState = readLastState(context);
        if (isMainProcess(context) && !TextUtils.isEmpty(lastState) && !lastState.startsWith("CLEAN_EXIT")) {
            record(context, "PREVIOUS_EXIT", "上次进程未正常关闭，lastState=" + lastState);
        }
        markRuntimeState(context, "RUNNING pid=" + Process.myPid());
        record(context, "PROCESS_START", "应用进程启动");
    }

    public static synchronized void markCleanExit(Context context, String reason) {
        if (context == null) return;

        markRuntimeState(context, "CLEAN_EXIT " + safeText(reason));
    }

    public static synchronized void markRuntimeState(Context context, String state) {
        if (context == null) return;

        File file = getLastStateFile(context);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.w(TAG, "Cannot create diagnostics directory: " + parent.getAbsolutePath());
            return;
        }

        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(DATE_FORMAT.format(new Date()));
            writer.write(" ");
            writer.write(safeText(state));
            writer.write(" | pid=");
            writer.write(String.valueOf(Process.myPid()));
        } catch (IOException e) {
            Log.w(TAG, "Failed to write runtime state", e);
        }
    }

    public static synchronized String readRecentText(Context context, int maxLines) {
        if (context == null) return "暂无运行日志";

        List<String> lines = readLines(context);
        if (lines.isEmpty()) return "暂无运行日志";

        int from = Math.max(0, lines.size() - Math.max(1, maxLines));
        StringBuilder builder = new StringBuilder();
        for (int i = lines.size() - 1; i >= from; i--) {
            builder.append(lines.get(i));
            if (i > from) builder.append("\n");
        }
        return builder.toString();
    }

    public static String memorySummary(Context context) {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
        long totalMb = runtime.totalMemory() / 1024L / 1024L;
        long maxMb = runtime.maxMemory() / 1024L / 1024L;
        StringBuilder builder = new StringBuilder();
        builder.append("heap ").append(usedMb).append("/").append(totalMb).append("/").append(maxMb).append("MB");

        try {
            long nativeMb = Debug.getNativeHeapAllocatedSize() / 1024L / 1024L;
            builder.append(", native ").append(nativeMb).append("MB");

            Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(memoryInfo);
            builder.append(", pss ").append(memoryInfo.getTotalPss() / 1024L).append("MB");
            builder.append(", private ").append(memoryInfo.getTotalPrivateDirty() / 1024L).append("MB");
        } catch (Throwable throwable) {
            builder.append(", dbgInfoError=").append(safeMessage(throwable));
        }

        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
                manager.getMemoryInfo(info);
                builder.append(", avail ").append(info.availMem / 1024L / 1024L).append("MB");
                builder.append(", low=").append(info.lowMemory);
            }
        } catch (Throwable throwable) {
            builder.append(", memInfoError=").append(safeMessage(throwable));
        }
        return builder.toString();
    }

    public static String throwableSummary(Throwable throwable) {
        if (throwable == null) return "-";

        StringBuilder builder = new StringBuilder();
        builder.append(throwable.getClass().getSimpleName());
        String message = throwable.getMessage();
        if (!TextUtils.isEmpty(message)) {
            builder.append(": ").append(message);
        }
        StackTraceElement[] stack = throwable.getStackTrace();
        int limit = Math.min(stack == null ? 0 : stack.length, 5);
        for (int i = 0; i < limit; i++) {
            builder.append(" | ").append(stack[i].toString());
        }
        return truncate(builder.toString(), MAX_MESSAGE_LENGTH);
    }

    private static String formatLine(Context context, String event, String message) {
        String safeEvent = TextUtils.isEmpty(event) ? "event" : event.trim();
        String safeMessage = TextUtils.isEmpty(message) ? "-" : message.trim();
        return DATE_FORMAT.format(new Date())
                + " [" + safeEvent + "] "
                + truncate(safeMessage, MAX_MESSAGE_LENGTH)
                + " | pid=" + Process.myPid()
                + " | proc=" + processName(context)
                + " | sdk=" + Build.VERSION.SDK_INT
                + " | v=" + BuildConfig.VERSION_NAME
                + " | " + memorySummary(context);
    }

    private static List<String> readLines(Context context) {
        List<String> lines = new ArrayList<>();
        File file = getFile(context);
        if (!file.exists()) return lines;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to read runtime log", e);
        }
        return lines;
    }

    private static void writeLines(Context context, List<String> lines) {
        File file = getFile(context);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.w(TAG, "Cannot create diagnostics directory: " + parent.getAbsolutePath());
            return;
        }

        try (FileWriter writer = new FileWriter(file, false)) {
            for (String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to write runtime log", e);
        }
    }

    private static File getFile(Context context) {
        return new File(new File(context.getFilesDir(), "diagnostics"), FILE_NAME);
    }

    private static File getLastStateFile(Context context) {
        String suffix = isMainProcess(context) ? "main" : sanitizeFilePart(processName(context));
        return new File(new File(context.getFilesDir(), "diagnostics"), suffix + "_" + LAST_STATE_FILE_NAME);
    }

    private static String readLastState(Context context) {
        File file = getLastStateFile(context);
        if (!file.exists()) return "";

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            return TextUtils.isEmpty(line) ? "" : line.trim();
        } catch (IOException e) {
            Log.w(TAG, "Failed to read runtime state", e);
            return "";
        }
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable == null) return "-";
        String message = throwable.getMessage();
        return TextUtils.isEmpty(message) ? throwable.getClass().getSimpleName() : message;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String safeText(String value) {
        return TextUtils.isEmpty(value) ? "-" : truncate(value.trim(), MAX_MESSAGE_LENGTH);
    }

    private static boolean isMainProcess(Context context) {
        return context.getPackageName().equals(processName(context));
    }

    private static String processName(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return android.app.Application.getProcessName();
        }

        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
                if (processes != null) {
                    int pid = Process.myPid();
                    for (ActivityManager.RunningAppProcessInfo info : processes) {
                        if (info.pid == pid) return info.processName;
                    }
                }
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to read process name", throwable);
        }
        return context.getPackageName() + ":unknown";
    }

    private static String sanitizeFilePart(String value) {
        if (TextUtils.isEmpty(value)) return "unknown";
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
