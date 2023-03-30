package tech.nagual.phoenix.tools.browser.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadLocalRandom;

import tech.nagual.phoenix.tools.browser.provider.FileProvider;
import tech.nagual.phoenix.tools.browser.services.MimeTypeService;
import threads.lite.LogUtils;
import threads.magnet.data.Storage;
import threads.magnet.data.StorageUnit;
import threads.magnet.event.EventBus;
import threads.magnet.metainfo.Torrent;
import threads.magnet.metainfo.TorrentFile;

public class ContentStorage implements Storage {


    private static final String TAG = ContentStorage.class.getSimpleName();
    private static final String APP_KEY = "AppKey";
    private static final String MAGNET_KEY = "magnetKey";
    private final DocumentFile root;
    private final EventBus eventBus;
    private final Context context;
    private final FileProvider fileProvider;
    private final List<ContentStorageUnit> units = new ArrayList<>();

    public ContentStorage(@NonNull Context context, @NonNull EventBus eventbus, @NonNull DocumentFile root) {
        this.context = context;
        this.eventBus = eventbus;
        this.root = root;
        this.fileProvider = FileProvider.getInstance(context);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static long copy(@NonNull InputStream source, @NonNull OutputStream sink) throws IOException {
        long nread = 0L;
        byte[] buf = new byte[4096];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }

    public static int nextFreePort() {
        int port = ThreadLocalRandom.current().nextInt(4001, 65535);
        while (true) {
            if (isLocalPortFree(port)) {
                return port;
            } else {
                port = ThreadLocalRandom.current().nextInt(4001, 65535);
            }
        }
    }

    private static boolean isLocalPortFree(int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Nullable
    public static String getMagnet(@NonNull Context context) {
        Objects.requireNonNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getString(MAGNET_KEY, null);
    }

    public static void setMagnet(@NonNull Context context, @NonNull String magnet) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(magnet);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(MAGNET_KEY, magnet);
        editor.apply();

    }

    @NonNull
    File getDataDir() {
        return fileProvider.getDataDir();
    }

    @NonNull
    Context getContext() {
        return context;
    }

    @NonNull
    EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public StorageUnit getUnit(@NonNull Torrent torrent, @NonNull TorrentFile torrentFile) {

        try {
            DocumentFile child = root;

            List<String> paths = torrentFile.getPathElements();
            long elements = paths.size();
            for (int i = 0; i < elements; i++) {
                String path = PathNormalizer.normalize(paths.get(i));

                Objects.requireNonNull(child);

                DocumentFile find = child.findFile(path);
                if (i < (elements - 1)) {

                    if (find != null && find.exists() && find.isDirectory()) {
                        child = find;
                    } else {
                        child = child.createDirectory(path);
                    }

                } else {
                    if (find != null && find.exists() && !find.isDirectory()) {
                        child = find;
                    } else {
                        String mimeType = MimeTypeService.getMimeType(path);

                        child = child.createFile(mimeType, path);
                    }
                }
            }
            Objects.requireNonNull(child);
            ContentStorageUnit unit = new ContentStorageUnit(this, torrentFile, child);
            units.add(unit);
            return unit;
        } catch (Throwable e) {
            LogUtils.error(TAG, e);
            throw new RuntimeException(e);
        }
    }

    public void finish() {
        for (ContentStorageUnit unit : units) {
            try {
                unit.finish();
            } catch (Throwable throwable) {
                LogUtils.error(TAG, throwable);
            }
        }
    }

    static class PathNormalizer {
        private static final String separator = File.separator;


        static String normalize(String path) {
            String normalized = path.trim();
            if (normalized.isEmpty()) {
                return "_";
            }

            StringTokenizer tokenizer = new StringTokenizer(normalized, separator, true);
            StringBuilder buf = new StringBuilder(normalized.length());
            boolean first = true;
            while (tokenizer.hasMoreTokens()) {
                String element = tokenizer.nextToken();
                if (separator.equals(element)) {
                    if (first) {
                        buf.append("_");
                    }
                    buf.append(separator);
                    // this will handle inner slash sequences, like ...a//b...
                    first = true;
                } else {
                    buf.append(normalizePathElement(element));
                    first = false;
                }
            }

            normalized = buf.toString();
            return replaceTrailingSlashes(normalized);
        }

        private static String normalizePathElement(String pathElement) {
            // truncate leading and trailing whitespaces
            String normalized = pathElement.trim();
            if (normalized.isEmpty()) {
                return "_";
            }

            // truncate trailing whitespaces and dots;
            // this will also eliminate '.' and '..' relative names
            char[] value = normalized.toCharArray();
            int to = value.length;
            while (to > 0 && (value[to - 1] == '.' || value[to - 1] == ' ')) {
                to--;
            }
            if (to == 0) {
                normalized = "";
            } else if (to < value.length) {
                normalized = normalized.substring(0, to);
            }

            return normalized.isEmpty() ? "_" : normalized;
        }

        private static String replaceTrailingSlashes(String path) {
            if (path.isEmpty()) {
                return path;
            }

            int k = 0;
            while (path.endsWith(separator)) {
                path = path.substring(0, path.length() - separator.length());
                k++;
            }
            if (k > 0) {
                char[] separatorChars = separator.toCharArray();
                char[] value = new char[path.length() + (separatorChars.length + 1) * k];
                System.arraycopy(path.toCharArray(), 0, value, 0, path.length());
                for (int offset = path.length(); offset < value.length; offset += separatorChars.length + 1) {
                    System.arraycopy(separatorChars, 0, value, offset, separatorChars.length);
                    value[offset + separatorChars.length] = '_';
                }
                path = new String(value);
            }

            return path;
        }
    }
}
