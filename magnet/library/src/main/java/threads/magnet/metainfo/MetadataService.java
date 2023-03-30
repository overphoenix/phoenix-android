package threads.magnet.metainfo;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import threads.magnet.LogUtils;
import threads.magnet.bencoding.BEList;
import threads.magnet.bencoding.BEMap;
import threads.magnet.bencoding.BEObject;
import threads.magnet.bencoding.BEParser;
import threads.magnet.bencoding.BEString;
import threads.magnet.bencoding.BEType;
import threads.magnet.service.CryptoUtil;


public final class MetadataService {
    private static final String TAG = MetadataService.class.getSimpleName();
    private static final String ANNOUNCE_KEY = "announce";
    private static final String ANNOUNCE_LIST_KEY = "announce-list";
    private static final String INFOMAP_KEY = "info";
    private static final String TORRENT_NAME_KEY = "name";
    private static final String CHUNK_SIZE_KEY = "piece length";
    private static final String CHUNK_HASHES_KEY = "pieces";
    private static final String TORRENT_SIZE_KEY = "length";
    private static final String FILES_KEY = "files";
    private static final String FILE_SIZE_KEY = "length";
    private static final String FILE_PATH_ELEMENTS_KEY = "path";
    private static final String PRIVATE_KEY = "private";
    private static final String CREATION_DATE_KEY = "creation date";
    private static final String CREATED_BY_KEY = "created by";
    private final Charset defaultCharset;

    public MetadataService() {
        this.defaultCharset = StandardCharsets.UTF_8;
    }


    public Torrent fromByteArray(byte[] bs) {
        return buildTorrent(bs);
    }


    private Torrent buildTorrent(byte[] bs) {
        try (BEParser parser = new BEParser(bs)) {
            if (parser.readType() != BEType.MAP) {
                throw new RuntimeException("Invalid metainfo format -- expected a map, got: "
                        + parser.readType().name().toLowerCase());
            }

            BEMap metadata = parser.readMap();
            BEMap infoDictionary;
            Map<String, BEObject<?>> root = metadata.getValue();
            if (root.containsKey(INFOMAP_KEY)) {
                // standard BEP-3 format
                infoDictionary = (BEMap) root.get(INFOMAP_KEY);
            } else {
                // BEP-9 exchanged metadata (just the info dictionary)
                infoDictionary = metadata;
            }
            Objects.requireNonNull(infoDictionary);
            TorrentSource source = infoDictionary::getContent;


            TorrentId torrentId = TorrentId.fromBytes(CryptoUtil.getSha1Digest(
                    infoDictionary.getContent()));

            Map<String, BEObject<?>> infoMap = infoDictionary.getValue();

            String name = "";
            if (infoMap.get(TORRENT_NAME_KEY) != null) {
                byte[] data = (byte[]) infoMap.get(TORRENT_NAME_KEY).getValue();
                name = new String(data, defaultCharset);
            }

            BigInteger chunkSize = (BigInteger) infoMap.get(CHUNK_SIZE_KEY).getValue();


            byte[] chunkHashes = (byte[]) infoMap.get(CHUNK_HASHES_KEY).getValue();


            List<TorrentFile> torrentFiles = new ArrayList<>();
            long size;
            if (infoMap.get(TORRENT_SIZE_KEY) != null) {
                BigInteger torrentSize = (BigInteger) infoMap.get(TORRENT_SIZE_KEY).getValue();
                size = torrentSize.longValue();

            } else {
                List<BEMap> files = (List<BEMap>) infoMap.get(FILES_KEY).getValue();
                BigInteger torrentSize = BigInteger.ZERO;
                for (BEMap file : files) {

                    Map<String, BEObject<?>> fileMap = file.getValue();

                    BigInteger fileSize = (BigInteger) fileMap.get(FILE_SIZE_KEY).getValue();
                    TorrentFile torrentFile = new TorrentFile(fileSize.longValue());


                    torrentSize = torrentSize.add(fileSize);

                    List<BEString> pathElements = (List<BEString>)
                            fileMap.get(FILE_PATH_ELEMENTS_KEY).getValue();

                    torrentFile.setPathElements(pathElements.stream()
                            .map(bytes -> bytes.getValue(defaultCharset))
                            .collect(Collectors.toList()));

                    torrentFiles.add(torrentFile);
                }

                size = torrentSize.longValue();
            }
            Torrent torrent = Torrent.createTorrent(torrentId, name, source, torrentFiles,
                    chunkHashes, size, chunkSize.longValue());


            boolean isPrivate = false;
            if (infoMap.get(PRIVATE_KEY) != null) {
                if (BigInteger.ONE.equals(infoMap.get(PRIVATE_KEY).getValue())) {
                    torrent.setPrivate(true);
                    isPrivate = true;
                }
            }

            if (root.get(CREATION_DATE_KEY) != null) {

                // TODO: some torrents contain bogus values here (like 101010101010), which causes an exception
                try {
                    BigInteger epochMilli = (BigInteger) root.get(CREATION_DATE_KEY).getValue();
                    torrent.setCreationDate(epochMilli.intValue());
                } catch (Throwable e) {
                    LogUtils.error(TAG, e);
                }
            }

            if (root.get(CREATED_BY_KEY) != null) {
                byte[] createdBy = (byte[]) root.get(CREATED_BY_KEY).getValue();
                torrent.setCreatedBy(new String(createdBy, defaultCharset));
            }

            // TODO: support for private torrents with multiple trackers
            if (!isPrivate && root.containsKey(ANNOUNCE_LIST_KEY)) {

                List<List<String>> trackerUrls;

                BEList announceList = (BEList) root.get(ANNOUNCE_LIST_KEY);
                List<BEList> tierList = (List<BEList>) announceList.getValue();
                trackerUrls = new ArrayList<>(tierList.size() + 1);
                for (BEList tierElement : tierList) {

                    List<String> tierTackerUrls;

                    List<BEString> trackerUrlList = (List<BEString>) tierElement.getValue();
                    tierTackerUrls = new ArrayList<>(trackerUrlList.size() + 1);
                    for (BEString trackerUrlElement : trackerUrlList) {
                        tierTackerUrls.add(trackerUrlElement.getValue(defaultCharset));
                    }
                    trackerUrls.add(tierTackerUrls);
                }

            } else if (root.containsKey(ANNOUNCE_KEY)) {
                byte[] trackerUrl = (byte[]) root.get(ANNOUNCE_KEY).getValue();
                LogUtils.error(TAG, new String(trackerUrl));
            }

            return torrent;

        } catch (Exception e) {
            throw new RuntimeException("Invalid metainfo format", e);
        }
    }
}
