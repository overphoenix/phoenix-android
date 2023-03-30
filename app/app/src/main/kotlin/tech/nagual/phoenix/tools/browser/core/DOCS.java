package tech.nagual.phoenix.tools.browser.core;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import tech.nagual.phoenix.tools.browser.BrowserManager;
import tech.nagual.phoenix.tools.browser.LogUtils;
import tech.nagual.phoenix.tools.browser.Settings;
import tech.nagual.phoenix.tools.browser.core.books.BOOKS;
import tech.nagual.phoenix.tools.browser.core.pages.PAGES;
import tech.nagual.phoenix.tools.browser.core.pages.Page;
import tech.nagual.phoenix.tools.browser.magic.ContentInfo;
import tech.nagual.phoenix.tools.browser.magic.ContentInfoUtil;
import tech.nagual.phoenix.tools.browser.services.MimeTypeService;
import threads.lite.IPFS;
import threads.lite.cid.Cid;
import threads.lite.cid.PeerId;
import threads.lite.core.Closeable;
import threads.lite.core.ClosedException;
import threads.lite.host.DnsResolver;
import threads.lite.ipns.Ipns;
import threads.lite.utils.Link;

public class DOCS {

    private static final String TAG = DOCS.class.getSimpleName();
    private static final HashSet<Long> threads = new HashSet<>();
    private static final HashSet<Uri> uris = new HashSet<>();
    private static DOCS INSTANCE = null;
    public final AtomicBoolean darkMode = new AtomicBoolean(false);
    private final IPFS ipfs;
    private final PAGES pages;
    private final BOOKS books;
    private final Hashtable<PeerId, String> resolves = new Hashtable<>();
    private boolean isRedirectIndex;
    private boolean isRedirectUrl;

    private DOCS(@NonNull Context context) {
        long start = System.currentTimeMillis();
        ipfs = IPFS.getInstance(context);
        pages = PAGES.getInstance(context);
        books = BOOKS.getInstance(context);
        refreshRedirectOptions(context);
        LogUtils.info(BrowserManager.TIME_TAG, "DOCS finish [" +
                (System.currentTimeMillis() - start) + "]...");
    }

    public static DOCS getInstance(@NonNull Context context) {

        if (INSTANCE == null) {
            synchronized (DOCS.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DOCS(context);
                }
            }
        }
        return INSTANCE;
    }


    public void refreshRedirectOptions(@NonNull Context context) {
        isRedirectIndex = Settings.isRedirectIndexEnabled(context);
        isRedirectUrl = Settings.isRedirectUrlEnabled(context);
    }

    public int numUris() {
        synchronized (TAG.intern()) {
            return uris.size();
        }
    }

    public void detachUri(@NonNull Uri uri) {
        synchronized (TAG.intern()) {
            uris.remove(uri);
        }
    }

    public void attachUri(@NonNull Uri uri) {
        synchronized (TAG.intern()) {
            uris.add(uri);
        }
    }

    public void attachThread(@NonNull Long thread) {
        synchronized (TAG.intern()) {
            threads.add(thread);
        }
    }

    public void releaseContent() {
        ipfs.reset();
    }

    public void releaseThreads() {
        synchronized (TAG.intern()) {
            threads.clear();
        }
    }

    public boolean shouldRun(@NonNull Long thread) {
        synchronized (TAG.intern()) {
            return threads.contains(thread);
        }
    }

    @NonNull
    private String getMimeType(@NonNull Context context, @NonNull Cid cid,
                               @NonNull Closeable closeable) throws ClosedException {

        if (ipfs.isDir(cid, closeable)) {
            return MimeTypeService.DIR_MIME_TYPE;
        }

        return getContentMimeType(context, cid, closeable);
    }

    @NonNull
    private String getContentMimeType(@NonNull Context context, @NonNull Cid cid,
                                      @NonNull Closeable closeable) throws ClosedException {

        String mimeType = MimeTypeService.OCTET_MIME_TYPE;

        try (InputStream in = ipfs.getLoaderStream(cid, closeable)) {

            ContentInfo info = ContentInfoUtil.getInstance(context).findMatch(in);

            if (info != null) {
                mimeType = info.getMimeType();
            }

        } catch (ClosedException closedException) {
            throw closedException;
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }

        return mimeType;
    }

    @Nullable
    public String getHost(@NonNull Uri uri) {
        try {
            if (Objects.equals(uri.getScheme(), Content.IPNS)) {
                return uri.getHost();
            }
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        return null;
    }

    @NonNull
    public String resolveName(@NonNull Uri uri, @NonNull String name,
                              @NonNull Closeable closeable) throws ResolveNameException {

        PeerId peerId = PeerId.decodeName(name);
        String resolved = resolves.get(peerId);
        if (resolved != null) {
            return resolved;
        }

        long sequence = 0L;
        String cid = null;
        Page page = pages.getPage(peerId.toBase58());
        if (page != null) {
            sequence = page.getSequence();
            cid = page.getContent();
        } else {
            page = pages.createPage(peerId.toBase58());
            pages.storePage(page);
        }


        Ipns.Entry entry = ipfs.resolveName(name, sequence, closeable);
        if (entry == null) {

            if (cid != null) {
                resolves.put(peerId, cid);
                return cid;
            }

            throw new ResolveNameException(uri.toString());
        }

        addResolves(peerId, entry.getHash());
        pages.setPageContent(peerId.toBase58(), entry.getHash());
        pages.setPageSequence(peerId.toBase58(), entry.getSequence());
        return entry.getHash();
    }

    public void addResolves(@NonNull PeerId peerId, String hash) {
        resolves.put(peerId, hash);
    }

    public String generateDirectoryHtml(@NonNull Uri uri, @NonNull List<String> paths,
                                        @Nullable List<Link> links) {
        String title = uri.getHost();


        StringBuilder answer = new StringBuilder("<html>" +
                "<head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=2, user-scalable=yes\">" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
                "<title>" + title + "</title>");

        answer.append("</head><body>");


        answer.append("<div style=\"padding: 16px; word-break:break-word; background-color: #333333; color: white;\">Index of ").append(uri).append("</div>");

        if (links != null) {
            if (!links.isEmpty()) {
                answer.append("<form><table  width=\"100%\" style=\"border-spacing: 4px;\">");
                for (Link link : links) {

                    Uri.Builder builder = new Uri.Builder();
                    builder.scheme(uri.getScheme())
                            .authority(uri.getAuthority());
                    for (String path : paths) {
                        builder.appendPath(path);
                    }
                    builder.appendPath(link.getName());
                    builder.appendQueryParameter("download", "0");
                    Uri linkUri = builder.build();


                    answer.append("<tr>");
                    answer.append("<td>");

                    if (!link.isDirectory()) {
                        answer.append(
                                MimeTypeService.getSvgResource(link.getName(), darkMode.get()));
                    } else {
                        if (darkMode.get()) {
                            answer.append(MimeTypeService.SVG_FOLDER_DARK);
                        } else {
                            answer.append(MimeTypeService.SVG_FOLDER);
                        }
                    }

                    answer.append("</td>");

                    answer.append("<td width=\"100%\" style=\"word-break:break-word\">");
                    answer.append("<a href=\"");
                    answer.append(linkUri.toString());
                    answer.append("\">");
                    answer.append(link.getName());
                    answer.append("</a>");
                    answer.append("</td>");

                    answer.append("<td>");
                    answer.append(getFileSize(link.getSize()));
                    answer.append("</td>");


                    answer.append("<td align=\"center\">");
                    String text = "<button style=\"float:none!important;display:inline;\" name=\"download\" value=\"1\" formenctype=\"text/plain\" formmethod=\"get\" type=\"submit\" formaction=\"" +
                            linkUri + "\">" + MimeTypeService.getSvgDownload() + "</button>";
                    answer.append(text);
                    answer.append("</td>");
                    answer.append("</tr>");
                }
                answer.append("</table></form>");
            }

        }
        answer.append("</body></html>");


        return answer.toString();
    }

    private String getFileSize(long size) {

        String fileSize;

        if (size < 1000) {
            fileSize = String.valueOf(size);
            return fileSize.concat(" B");
        } else if (size < 1000 * 1000) {
            fileSize = String.valueOf((double) (size / 1000));
            return fileSize.concat(" KB");
        } else {
            fileSize = String.valueOf((double) (size / (1000 * 1000)));
            return fileSize.concat(" MB");
        }
    }

    @NonNull
    public WebResourceResponse getResponse(@NonNull Context context, @NonNull Uri uri,
                                           @NonNull Cid root, @NonNull List<String> paths,
                                           @NonNull Closeable closeable) throws Exception {

        if (paths.isEmpty()) {
            if (ipfs.isDir(root, closeable)) {
                List<Link> links = ipfs.getLinks(root, false, closeable);
                String answer = generateDirectoryHtml(uri, paths, links);
                return new WebResourceResponse(MimeTypeService.HTML_MIME_TYPE, Content.UTF8,
                        new ByteArrayInputStream(answer.getBytes()));
            } else {
                String mimeType = getContentMimeType(context, root, closeable);
                return getContentResponse(root, mimeType, closeable);
            }


        } else {
            Cid cid = ipfs.resolve(root, paths, closeable);
            if (cid == null) {
                throw new ContentException(uri.toString());
            }
            if (ipfs.isDir(cid, closeable)) {
                List<Link> links = ipfs.getLinks(cid, false, closeable);
                String answer = generateDirectoryHtml(uri, paths, links);
                return new WebResourceResponse(MimeTypeService.HTML_MIME_TYPE, Content.UTF8,
                        new ByteArrayInputStream(answer.getBytes()));

            } else {
                String mimeType = getMimeType(context, uri, cid, closeable);
                return getContentResponse(cid, mimeType, closeable);
            }
        }
    }

    @NonNull
    private WebResourceResponse getContentResponse(@NonNull Cid content, @NonNull String mimeType,
                                                   @NonNull Closeable closeable)
            throws ClosedException {

        try (InputStream in = ipfs.getLoaderStream(content, closeable)) {
            if (closeable.isClosed()) {
                throw new ClosedException();
            }

            Map<String, String> responseHeaders = new HashMap<>();

            return new WebResourceResponse(mimeType, Content.UTF8, 200,
                    "OK", responseHeaders, new BufferedInputStream(in));
        } catch (Throwable throwable) {
            if (closeable.isClosed()) {
                throw new ClosedException();
            }
            throw new RuntimeException(throwable);
        }


    }

    @NonNull
    public String getMimeType(@NonNull Context context, @NonNull Uri uri,
                              @NonNull Cid cid, @NonNull Closeable closeable)
            throws ClosedException {

        List<String> paths = uri.getPathSegments();
        if (!paths.isEmpty()) {
            String name = paths.get(paths.size() - 1);
            String mimeType = MimeTypeService.getMimeType(name);
            if (!mimeType.equals(MimeTypeService.OCTET_MIME_TYPE)) {
                return mimeType;
            } else {
                return getMimeType(context, cid, closeable);
            }
        } else {
            return getMimeType(context, cid, closeable);
        }

    }

    @NonNull
    public String getFileName(@NonNull Uri uri) {

        List<String> paths = uri.getPathSegments();
        if (!paths.isEmpty()) {
            return paths.get(paths.size() - 1);
        } else {
            return "" + uri.getHost();
        }

    }

    @Nullable
    public Cid getContent(@NonNull Uri uri, @NonNull Closeable closeable)
            throws InvalidNameException, ResolveNameException, ClosedException {

        String host = uri.getHost();
        Objects.requireNonNull(host);

        Cid root = getRoot(uri, closeable);
        Objects.requireNonNull(root);

        List<String> paths = uri.getPathSegments();

        return ipfs.resolve(root, paths, closeable);
    }


    @NonNull
    private String resolveHost(@NonNull Uri uri, @NonNull String host, @NonNull Closeable closeable)
            throws ResolveNameException, InvalidNameException, ClosedException {
        String link = DnsResolver.resolveDnsLink(host);
        if (link.isEmpty()) {
            // could not resolved, maybe no NETWORK
            String dnsLink = books.getDnsLink(uri.toString());
            if (dnsLink == null) {
                throw new DOCS.ResolveNameException(uri.toString());
            } else {
                return resolveDnsLink(uri, dnsLink, closeable);
            }
        } else {
            if (link.startsWith(IPFS.IPFS_PATH)) {
                // try to store value
                books.storeDnsLink(uri.toString(), link);
                return resolveDnsLink(uri, link, closeable);
            } else if (link.startsWith(IPFS.IPNS_PATH)) {
                return resolveHost(uri,
                        link.replaceFirst(IPFS.IPNS_PATH, ""),
                        closeable);

            } else {
                throw new DOCS.ResolveNameException(uri.toString());
            }
        }
    }

    @NonNull
    private String resolveUri(@NonNull Uri uri, @NonNull Closeable closeable)
            throws ResolveNameException, InvalidNameException, ClosedException {
        String host = uri.getHost();
        Objects.requireNonNull(host);

        if (!Objects.equals(uri.getScheme(), Content.IPNS)) {
            throw new RuntimeException();
        }

        if (ipfs.decodeName(host).isEmpty()) {
            return resolveHost(uri, host, closeable);
        } else {
            return resolveName(uri, host, closeable);
        }
    }

    @Nullable
    public Cid getRoot(@NonNull Uri uri, @NonNull Closeable closeable)
            throws ResolveNameException, InvalidNameException, ClosedException {
        String host = uri.getHost();
        Objects.requireNonNull(host);

        Cid root;
        if (Objects.equals(uri.getScheme(), Content.IPNS)) {
            root = Cid.decode(resolveUri(uri, closeable));
        } else {
            if (!ipfs.isValidCID(host)) {
                throw new InvalidNameException(uri.toString());
            }
            root = Cid.decode(host);
        }
        return root;
    }

    @NonNull
    public WebResourceResponse getResponse(@NonNull Context context, @NonNull Uri uri,
                                           @NonNull Closeable closeable) throws Exception {


        List<String> paths = uri.getPathSegments();

        Cid root = getRoot(uri, closeable);
        Objects.requireNonNull(root);

        return getResponse(context, uri, root, paths, closeable);

    }

    @NonNull
    public Uri redirectHttp(@NonNull Uri uri) {
        try {
            if (Objects.equals(uri.getScheme(), Content.HTTP)) {
                String host = uri.getHost();
                Objects.requireNonNull(host);
                if (Objects.equals(host, "localhost") || Objects.equals(host, "127.0.0.1")) {
                    List<String> paths = uri.getPathSegments();
                    if (paths.size() >= 2) {
                        String protocol = paths.get(0);
                        String authority = paths.get(1);
                        List<String> subPaths = new ArrayList<>(paths);
                        subPaths.remove(protocol);
                        subPaths.remove(authority);
                        if (ipfs.isValidCID(authority)) {
                            if (Objects.equals(protocol, Content.IPFS)) {
                                Uri.Builder builder = new Uri.Builder();
                                builder.scheme(Content.IPFS)
                                        .authority(authority);

                                for (String path : subPaths) {
                                    builder.appendPath(path);
                                }
                                return builder.build();
                            } else if (Objects.equals(protocol, Content.IPNS)) {
                                Uri.Builder builder = new Uri.Builder();
                                builder.scheme(Content.IPNS)
                                        .authority(authority);

                                for (String path : subPaths) {
                                    builder.appendPath(path);
                                }
                                return builder.build();
                            }
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        return uri;
    }

    @NonNull
    public Uri redirectHttps(@NonNull Uri uri) {
        try {
            if (isRedirectUrl && Objects.equals(uri.getScheme(), Content.HTTPS)) {


                List<String> paths = uri.getPathSegments();
                if (paths.size() >= 2) {
                    String protocol = paths.get(0);
                    if (Objects.equals(protocol, Content.IPFS) ||
                            Objects.equals(protocol, Content.IPNS)) {
                        String authority = paths.get(1);
                        List<String> subPaths = new ArrayList<>(paths);
                        subPaths.remove(protocol);
                        subPaths.remove(authority);
                        if (ipfs.isValidCID(authority)) {
                            if (Objects.equals(protocol, Content.IPFS)) {
                                Uri.Builder builder = new Uri.Builder();
                                builder.scheme(Content.IPFS)
                                        .authority(authority);

                                for (String path : subPaths) {
                                    builder.appendPath(path);
                                }
                                return builder.build();
                            } else if (Objects.equals(protocol, Content.IPNS)) {
                                Uri.Builder builder = new Uri.Builder();
                                builder.scheme(Content.IPNS)
                                        .authority(authority);

                                for (String path : subPaths) {
                                    builder.appendPath(path);
                                }
                                return builder.build();
                            }
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        return uri;
    }

    @NonNull
    private String resolveDnsLink(@NonNull Uri uri, @NonNull String link,
                                  @NonNull Closeable closeable)
            throws ClosedException, InvalidNameException, ResolveNameException {

        List<String> paths = uri.getPathSegments();
        if (link.startsWith(IPFS.IPFS_PATH)) {
            return link.replaceFirst(IPFS.IPFS_PATH, "");
        } else if (link.startsWith(IPFS.IPNS_PATH)) {
            String cid = link.replaceFirst(IPFS.IPNS_PATH, "");
            if (!ipfs.decodeName(cid).isEmpty()) {
                Uri.Builder builder = new Uri.Builder();
                builder.scheme(Content.IPNS)
                        .authority(cid);
                for (String path : paths) {
                    builder.appendPath(path);
                }
                return resolveUri(builder.build(), closeable);
            } else {
                // is is assume like /ipns/<dns_link> = > therefore <dns_link> is url
                return resolveName(uri, cid, closeable);
            }
        } else {
            // is is assume that links is  <dns_link> is url

            Uri dnsUri = Uri.parse(link);
            if (dnsUri != null) {
                Uri.Builder builder = new Uri.Builder();
                builder.scheme(Content.IPNS)
                        .authority(dnsUri.getAuthority());
                for (String path : paths) {
                    builder.appendPath(path);
                }
                return resolveUri(builder.build(), closeable);
            }
        }
        throw new ResolveNameException(uri.toString());
    }

    @NonNull
    public Uri redirectUri(@NonNull Uri uri, @NonNull Closeable closeable)
            throws ResolveNameException, InvalidNameException, ClosedException {


        if (Objects.equals(uri.getScheme(), Content.IPNS) ||
                Objects.equals(uri.getScheme(), Content.IPFS)) {
            List<String> paths = uri.getPathSegments();
            Cid root = getRoot(uri, closeable);
            Objects.requireNonNull(root);
            return redirect(uri, root, paths, closeable);
        }
        return uri;
    }

    @NonNull
    private Uri redirect(@NonNull Uri uri, @NonNull Cid root,
                         @NonNull List<String> paths, @NonNull Closeable closeable)
            throws ClosedException {


        // check first paths
        // if like this .../ipfs/Qa..../
        // THIS IS A BIG HACK AND SHOULD NOT BE SUPPORTED
        if (paths.size() >= 2) {
            String protocol = paths.get(0);
            if (Objects.equals(protocol, Content.IPFS) ||
                    Objects.equals(protocol, Content.IPNS)) {
                String authority = paths.get(1);
                List<String> subPaths = new ArrayList<>(paths);
                subPaths.remove(protocol);
                subPaths.remove(authority);
                if (ipfs.isValidCID(authority)) {
                    if (Objects.equals(protocol, Content.IPFS)) {
                        Uri.Builder builder = new Uri.Builder();
                        builder.scheme(Content.IPFS)
                                .authority(authority);

                        for (String path : subPaths) {
                            builder.appendPath(path);
                        }
                        return builder.build();
                    } else if (Objects.equals(protocol, Content.IPNS)) {
                        Uri.Builder builder = new Uri.Builder();
                        builder.scheme(Content.IPNS)
                                .authority(authority);

                        for (String path : subPaths) {
                            builder.appendPath(path);
                        }
                        return builder.build();
                    }
                }
            }
        }

        if (isRedirectIndex) {
            Cid cid = ipfs.resolve(root, paths, closeable);

            if (cid != null) {
                if (ipfs.isDir(cid, closeable)) {
                    boolean exists = ipfs.resolve(cid, IPFS.INDEX_HTML, closeable);

                    if (exists) {
                        Uri.Builder builder = new Uri.Builder();
                        builder.scheme(uri.getScheme())
                                .authority(uri.getAuthority());
                        for (String path : paths) {
                            builder.appendPath(path);
                        }
                        builder.appendPath(IPFS.INDEX_HTML);
                        return builder.build();
                    }
                }
            }
        }


        return uri;
    }


    public void cleanupResolver(@NonNull Uri uri) {
        try {
            String host = getHost(uri);
            if (host != null) {
                PeerId peerId = PeerId.decodeName(host);
                resolves.remove(peerId);
            }
        } catch (Throwable ignore) {
            // ignore common failure
        }
    }

    public static class ContentException extends Exception {

        public ContentException(@NonNull String name) {
            super("Content not found for " + name);
        }
    }

    public static class ResolveNameException extends Exception {

        public ResolveNameException(@NonNull String name) {
            super("Resolve name failed for " + name);
        }
    }

    public static class InvalidNameException extends Exception {

        public InvalidNameException(@NonNull String name) {
            super("Invalid name detected for " + name);
        }


    }
}
