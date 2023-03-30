package tech.nagual.phoenix.tools.browser.work;

import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.WebViewDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;

import tech.nagual.phoenix.tools.browser.LogUtils;
import threads.lite.data.BLOCKS;
import tech.nagual.phoenix.BuildConfig;
import tech.nagual.phoenix.tools.browser.core.pages.PAGES;
import tech.nagual.phoenix.tools.browser.provider.FileProvider;

public class ClearBrowserDataWorker extends Worker {

    private static final String TAG = ClearBrowserDataWorker.class.getSimpleName();

    @SuppressWarnings("WeakerAccess")
    public ClearBrowserDataWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    private static OneTimeWorkRequest getWork() {

        return new OneTimeWorkRequest.Builder(ClearBrowserDataWorker.class).build();

    }

    public static void clearCache(@NonNull Context context) {

        WorkManager.getInstance(context).enqueueUniqueWork(
                TAG, ExistingWorkPolicy.REPLACE, getWork());

    }

    private void deleteCache(@NonNull Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }

    private boolean deleteDir(@Nullable File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private void logCacheDir(@NonNull Context context) {
        try {
            File[] files = context.getCacheDir().listFiles();
            if (files != null) {
                for (File file : files) {
                    LogUtils.error(TAG, "" + file.length() + " " + file.getAbsolutePath());
                    if (file.isDirectory()) {
                        File[] children = file.listFiles();
                        if (children != null) {
                            for (File child : children) {
                                LogUtils.error(TAG, "" + child.length() + " " + child.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LogUtils.error(TAG, e);
        }
    }


    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();

        LogUtils.info(TAG, " start ...");

        try {

            // Clear all the cookies
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();


            // clears passwords
            WebViewDatabase.getInstance(getApplicationContext()).clearHttpAuthUsernamePassword();

            // Clear local data
            FileProvider fileProvider = FileProvider.getInstance(getApplicationContext());
            fileProvider.cleanImageDir();
            fileProvider.cleanDataDir();

            // Clear ipfs and pages data
            BLOCKS.getInstance(getApplicationContext()).clear();
            PAGES.getInstance(getApplicationContext()).clear();

            deleteCache(getApplicationContext());

            if (BuildConfig.DEBUG) {
                logCacheDir(getApplicationContext());
            }

        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);

        } finally {
            LogUtils.info(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }

        return Result.success();
    }
}

