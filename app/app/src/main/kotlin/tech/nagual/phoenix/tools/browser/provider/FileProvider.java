package tech.nagual.phoenix.tools.browser.provider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;

import androidx.annotation.NonNull;

import java.io.File;

import tech.nagual.phoenix.tools.browser.LogUtils;

public class FileProvider {
    private static final String TAG = FileProvider.class.getSimpleName();
    private static final String IMAGES = "images";
    private static final String DATA = "data";
    private static FileProvider INSTANCE = null;
    private final File mImageDir;
    private final File mDataDir;

    private FileProvider(@NonNull Context context) {
        mImageDir = new File(context.getCacheDir(), IMAGES);
        mDataDir = new File(context.getCacheDir(), DATA);
    }

    public static boolean hasNoWritePermission(@NonNull Context context, @NonNull Uri uri) {
        int perm = context.checkUriPermission(uri, Binder.getCallingPid(), Binder.getCallingUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return perm == PackageManager.PERMISSION_DENIED;
    }

    public static FileProvider getInstance(@NonNull Context context) {

        if (INSTANCE == null) {
            synchronized (FileProvider.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FileProvider(context);
                }
            }
        }
        return INSTANCE;
    }

    public File getImageDir() {
        if (!mImageDir.isDirectory() && !mImageDir.exists()) {
            boolean result = mImageDir.mkdir();
            if (!result) {
                throw new RuntimeException("image directory does not exists");
            }
        }
        return mImageDir;
    }

    public File getDataDir() {
        if (!mDataDir.isDirectory() && !mDataDir.exists()) {
            boolean result = mDataDir.mkdir();
            if (!result) {
                throw new RuntimeException("image directory does not exists");
            }
        }
        return mDataDir;
    }

    public void cleanImageDir() {
        deleteFile(getImageDir());
    }

    public void cleanDataDir() {
        deleteFile(getDataDir());
    }

    private void deleteFile(@NonNull File root) {
        try {
            if (root.isDirectory()) {
                File[] files = root.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            deleteFile(file);
                            boolean result = file.delete();
                            if (!result) {
                                LogUtils.error(TAG, "File " + file.getName() + " not deleted");
                            }
                        } else {
                            boolean result = file.delete();
                            if (!result) {
                                LogUtils.error(TAG, "File " + file.getName() + " not deleted");
                            }
                        }
                    }
                }
                boolean result = root.delete();
                if (!result) {
                    LogUtils.error(TAG, "File " + root.getName() + " not deleted");
                }
            } else {
                boolean result = root.delete();
                if (!result) {
                    LogUtils.error(TAG, "File " + root.getName() + " not deleted");
                }
            }
        } catch (Throwable e) {
            LogUtils.error(TAG, e);
        }
    }
}
