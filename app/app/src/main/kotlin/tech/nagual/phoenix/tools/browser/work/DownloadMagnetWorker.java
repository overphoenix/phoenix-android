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
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import tech.nagual.phoenix.tools.browser.BrowserManager;
import tech.nagual.phoenix.tools.browser.LogUtils;
import threads.magnet.Client;
import threads.magnet.ClientBuilder;
import threads.magnet.IdentityService;
import threads.magnet.Runtime;
import threads.magnet.event.EventBus;
import threads.magnet.magnet.MagnetUri;
import threads.magnet.magnet.MagnetUriParser;
import threads.magnet.net.PeerId;
import tech.nagual.phoenix.tools.browser.BrowserActivity;
import tech.nagual.common.R;
import tech.nagual.phoenix.tools.browser.core.Content;
import tech.nagual.phoenix.tools.browser.utils.ContentStorage;

public class DownloadMagnetWorker extends Worker {

    private static final String TAG = DownloadMagnetWorker.class.getSimpleName();
    private final NotificationManager mNotificationManager;

    @SuppressWarnings("WeakerAccess")
    public DownloadMagnetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static OneTimeWorkRequest getWork(@NonNull Uri magnet, @NonNull Uri dest) {

        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putString(Content.MAGNET, magnet.toString());
        data.putString(Content.URI, dest.toString());

        return new OneTimeWorkRequest.Builder(DownloadMagnetWorker.class)
                .setInputData(data.build())
                .setConstraints(builder.build())
                .build();

    }

    public static void download(@NonNull Context context, @NonNull Uri magnet, @NonNull Uri dest) {
        WorkManager.getInstance(context).enqueue(getWork(magnet, dest));
    }


    @Override
    public void onStopped() {
        closeNotification();
    }

    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();

        LogUtils.info(TAG, " start [" + (System.currentTimeMillis() - start) + "]...");

        try {

            String magnet = getInputData().getString(Content.MAGNET);
            Objects.requireNonNull(magnet);
            String dest = getInputData().getString(Content.URI);
            Objects.requireNonNull(dest);


            MagnetUri magnetUri = MagnetUriParser.lenientParser().parse(magnet);

            String name = magnet;
            if (magnetUri.getDisplayName().isPresent()) {
                name = magnetUri.getDisplayName().get();
            }

            reportProgress(name, 0);

            Uri uri = Uri.parse(dest);
            DocumentFile rootDocFile = DocumentFile.fromTreeUri(getApplicationContext(), uri);
            Objects.requireNonNull(rootDocFile);


            DocumentFile find = rootDocFile.findFile(name);
            DocumentFile rootDoc;
            if (find != null && find.exists() && find.isDirectory()) {
                rootDoc = find;
            } else {
                rootDoc = rootDocFile.createDirectory(name);
            }


            try {
                Objects.requireNonNull(rootDoc);

                byte[] id = new IdentityService().getID();

                EventBus eventBus = Runtime.provideEventBus();
                ContentStorage storage = new ContentStorage(
                        getApplicationContext(), eventBus, rootDoc);
                Runtime runtime = new Runtime(PeerId.fromBytes(id), eventBus,
                        ContentStorage.nextFreePort());

                Client client = new ClientBuilder()
                        .runtime(runtime)
                        .storage(storage)
                        .magnet(magnet)
                        .build();

                AtomicInteger atomicProgress = new AtomicInteger(0);
                String finalName = name;
                client.startAsync((torrentSessionState) -> {

                    long completePieces = torrentSessionState.getPiecesComplete();
                    long totalPieces = torrentSessionState.getPiecesTotal();
                    int progress = (int) ((completePieces * 100.0f) / totalPieces);

                    LogUtils.info(TAG, "progress : " + progress +
                            " pieces : " + completePieces + "/" + totalPieces);

                    if (atomicProgress.getAndSet(progress) < progress) {
                        reportProgress(finalName, progress);
                    }
                    if (isStopped()) {
                        try {
                            client.stop();
                        } catch (Throwable throwable) {
                            LogUtils.error(TAG, throwable);
                        } finally {
                            LogUtils.info(TAG, "Client is stopped !!!");
                        }
                    }
                }, 1000).join();

                if (!isStopped()) {
                    storage.finish();
                    closeNotification();
                    buildCompleteNotification(name, uri);
                } else {
                    if (rootDoc.exists()) {
                        rootDoc.delete();
                    }
                }


            } catch (Throwable e) {
                if (!isStopped()) {
                    buildFailedNotification(name);
                }
                throw e;
            }

        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
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


    private void reportProgress(@NonNull String title, int percent) {

        if (!isStopped()) {
            setForegroundAsync(createForegroundInfo(title, percent));
        }
    }


    private Notification createNotification(@NonNull String content, int progress) {


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

        builder.setContentTitle(content)
                .setSubText("" + progress + "%")
                .setContentIntent(pendingIntent)
                .setProgress(100, progress, false)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.browser_download)
                .addAction(action)
                .setGroup(BrowserManager.STORAGE_GROUP_ID)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setOngoing(true);


        return builder.build();


    }

    @NonNull
    private ForegroundInfo createForegroundInfo(@NonNull String title, int progress) {
        Notification notification = createNotification(title, progress);
        if (mNotificationManager != null) {
            mNotificationManager.notify(getId().hashCode(), notification);
        }
        return new ForegroundInfo(getId().hashCode(), notification);
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
        builder.setCategory(Notification.CATEGORY_EVENT);
        Notification notification = builder.build();


        if (mNotificationManager != null) {
            mNotificationManager.notify(TAG.hashCode(), notification);
        }
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
