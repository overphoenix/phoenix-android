package tech.nagual.phoenix.tools.browser.work;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import tech.nagual.phoenix.tools.browser.BrowserManager;
import tech.nagual.phoenix.tools.browser.LogUtils;
import threads.lite.IPFS;
import threads.lite.utils.ReaderProgress;
import tech.nagual.phoenix.tools.browser.BrowserActivity;
import tech.nagual.common.R;
import tech.nagual.phoenix.tools.browser.core.Content;

public class DownloadFileWorker extends Worker {

    private static final String TAG = DownloadFileWorker.class.getSimpleName();
    private final NotificationManager mNotificationManager;


    @SuppressWarnings("WeakerAccess")
    public DownloadFileWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static OneTimeWorkRequest getWork(@NonNull Uri uri, @NonNull Uri source,
                                              @NonNull String filename, @NonNull String mimeType,
                                              long size) {
        Data.Builder data = new Data.Builder();
        data.putString(Content.URI, uri.toString());
        data.putString(Content.NAME, filename);
        data.putString(Content.TYPE, mimeType);
        data.putLong(Content.SIZE, size);
        data.putString(Content.FILE, source.toString());

        return new OneTimeWorkRequest.Builder(DownloadFileWorker.class)
                .setInputData(data.build())
                .setInitialDelay(1, TimeUnit.MILLISECONDS)
                .build();
    }

    public static void download(@NonNull Context context, @NonNull Uri uri, @NonNull Uri source,
                                @NonNull String filename, @NonNull String mimeType, long size) {
        WorkManager.getInstance(context).enqueue(getWork(uri, source, filename, mimeType, size));
    }


    @Override
    public void onStopped() {
        closeNotification();
    }


    @NonNull
    @Override
    public Result doWork() {


        String dest = getInputData().getString(Content.URI);
        Objects.requireNonNull(dest);
        long start = System.currentTimeMillis();
        LogUtils.info(TAG, " start ... " + dest);


        try {

            Uri uriDest = Uri.parse(dest);
            DocumentFile doc = DocumentFile.fromTreeUri(getApplicationContext(), uriDest);
            Objects.requireNonNull(doc);


            long size = getInputData().getLong(Content.SIZE, 0);
            String name = getInputData().getString(Content.NAME);
            Objects.requireNonNull(name);
            String mimeType = getInputData().getString(Content.TYPE);
            Objects.requireNonNull(mimeType);

            String url = getInputData().getString(Content.FILE);
            Objects.requireNonNull(url);
            Uri uri = Uri.parse(url);

            reportProgress(name, 0);

            if (Objects.equals(uri.getScheme(), Content.HTTPS) ||
                    Objects.equals(uri.getScheme(), Content.HTTP)) {
                try {
                    HttpURLConnection.setFollowRedirects(false);

                    HttpURLConnection huc;
                    URL urlCon = new URL(uri.toString());
                    huc = (HttpURLConnection) urlCon.openConnection();
                    huc.setReadTimeout(30000);
                    huc.connect();

                    try (InputStream is = huc.getInputStream()) {
                        DocumentFile child = doc.createFile(mimeType, name);
                        Objects.requireNonNull(child);
                        try (OutputStream os = getApplicationContext().
                                getContentResolver().openOutputStream(child.getUri())) {
                            Objects.requireNonNull(os);
                            IPFS.copy(is, os, new ReaderProgress() {
                                @Override
                                public long getSize() {
                                    return size;
                                }

                                @Override
                                public void setProgress(int progress) {
                                    reportProgress(name, progress);
                                }

                                @Override
                                public boolean doProgress() {
                                    return !isStopped();
                                }

                                @Override
                                public boolean isClosed() {
                                    return !isStopped();
                                }

                            });
                        }

                        if (isStopped()) {
                            child.delete();
                        } else {
                            closeNotification();
                            buildCompleteNotification(name, uriDest);
                        }
                    }


                } catch (Throwable e) {
                    if (!isStopped()) {
                        buildFailedNotification(name);
                    }
                    throw e;
                }
            }
        } catch (Throwable e) {
            LogUtils.error(TAG, e);
        } finally {
            LogUtils.info(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }

        return Result.success();

    }


    private void closeNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(getId().hashCode());
        }
    }


    private void buildFailedNotification(@NonNull String name) {

        Notification.Builder builder = new Notification.Builder(
                getApplicationContext(), BrowserManager.STORAGE_CHANNEL_ID);

        builder.setContentTitle(getApplicationContext().getString(R.string.browser_download_failed, name));
        builder.setSmallIcon(R.drawable.browser_download);
        Intent defaultIntent = new Intent(getApplicationContext(), BrowserActivity.class);

        int requestID = (int) System.currentTimeMillis();
        PendingIntent defaultPendingIntent = PendingIntent.getActivity(
                getApplicationContext(), requestID, defaultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.setContentIntent(defaultPendingIntent);
        builder.setAutoCancel(true);
        builder.setGroup(BrowserManager.STORAGE_GROUP_ID);
        Notification notification = builder.build();

        if (mNotificationManager != null) {
            mNotificationManager.notify(TAG.hashCode(), notification);
        }
    }


    private void reportProgress(@NonNull String title, int percent) {

        if (!isStopped()) {

            Notification notification = createNotification(title, percent);

            if (mNotificationManager != null) {
                mNotificationManager.notify(getId().hashCode(), notification);
            }

            setForegroundAsync(new ForegroundInfo(getId().hashCode(), notification));
        }
    }


    private Notification createNotification(@NonNull String title, int progress) {

        Notification.Builder builder = new Notification.Builder(getApplicationContext(),
                BrowserManager.STORAGE_CHANNEL_ID);


        PendingIntent intent = WorkManager.getInstance(getApplicationContext())
                .createCancelPendingIntent(getId());
        String cancel = getApplicationContext().getString(android.R.string.cancel);

        Intent main = new Intent(getApplicationContext(), BrowserActivity.class);

        int requestID = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), requestID,
                main, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Action action = new Notification.Action.Builder(
                Icon.createWithResource(getApplicationContext(), R.drawable.browser_pause), cancel,
                intent).build();

        builder.setContentTitle(title)
                .setSubText("" + progress + "%")
                .setContentIntent(pendingIntent)
                .setProgress(100, progress, false)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.browser_download)
                .setGroup(BrowserManager.STORAGE_GROUP_ID)
                .addAction(action)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setUsesChronometer(true)
                .setOngoing(true);

        return builder.build();
    }

    private void buildCompleteNotification(@NonNull String name, @NonNull Uri uri) {

        Notification.Builder builder = new Notification.Builder(
                getApplicationContext(), BrowserManager.STORAGE_CHANNEL_ID);

        builder.setContentTitle(getApplicationContext().getString(R.string.browser_download_complete, name));
        builder.setSmallIcon(R.drawable.browser_download);

        Intent defaultIntent = new Intent(BrowserActivity.SHOW_DOWNLOADS, uri,
                getApplicationContext(), BrowserActivity.class);
        int requestID = (int) System.currentTimeMillis();
        PendingIntent defaultPendingIntent = PendingIntent.getActivity(
                getApplicationContext(), requestID, defaultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.setContentIntent(defaultPendingIntent);
        builder.setAutoCancel(true);
        builder.setGroup(BrowserManager.STORAGE_GROUP_ID);
        builder.setCategory(Notification.CATEGORY_EVENT);
        Notification notification = builder.build();

        if (mNotificationManager != null) {
            mNotificationManager.notify(TAG.hashCode(), notification);
        }
    }
}
