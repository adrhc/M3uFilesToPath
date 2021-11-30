package M3uFilesToPath.lucene;

import M3uFilesToPath.m3u.M3uPlaylist;
import M3uFilesToPath.m3u.M3uPlaylistSong;
import M3uFilesToPath.util.Util;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: adr
 * Date: Jan 14, 2011
 * Time: 10:20:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class MusicIndexSearcher {
    private static final String CONVERTED_PLAYLIST_SUFIX = " - GENERATED";
    private static final int DEFAULTresultPerSong = 1;
    private int resultPerSong = DEFAULTresultPerSong;
    private File indexPath;
    /**
     * true: cauta in index si mp3 ce nu exista pe HDD dar sunt referite din playlist-ul mp3/mp3u
     * Se refera la mp3 care se vor cauta ci nu la mp3 rezultat in urma cautarii ! spre deosebire de foundSongCanMiss
     */
    private static final boolean DEFAULTsearchMissingFilesToo = true;
    private boolean searchMissingFilesToo = DEFAULTsearchMissingFilesToo;
    /**
     * true: are sens true cand se converteste playlist local/extern folosind un MusicIndex.INDEX_DIR extern
     * false: are sens false cand se converteste playlist local/extern folosind un MusicIndex.INDEX_DIR local
     * Se refera la mp3 care s-au gasit ci nu la mp3 ce se cauta ! spre deosebire de searchMissingFilesToo
     */
    private static final boolean DEFAULTfoundSongCanMiss = false;
    private boolean foundSongCanMiss = DEFAULTfoundSongCanMiss;
    /**
     * Are sens impreuna cu useFirstFoundOnNullPerfectMatch.
     * true: nu are sens cu maxSecondsDeviationPercent > 0.
     */
    private boolean dontSearchBySeconds;
    /**
     * In caz ca dupa o cautare intre rezultate nu se gaseste
     * si perfectMatch atunci se va arunca eroare.
     * true: are sens doar daca acceptam ca unele melodii cautate sa nu se gaseasca
     */
    private boolean throwErrorOnPerfectMatchNotFound = true;
    /**
     * Daca
     * max(
     * abs(songFound.seconds - songSearched.seconds) / min(songFound.seconds, songSearched.seconds) * 100
     * abs(songFound.fileSize - songSearched.fileSize) / min(songFound.fileSize, songSearched.fileSize) * 100
     * ) > maxSecondsDeviationPercent
     * atunci se considera ca songFound != songSearched !!!
     */
    private float maxSecondsDeviationPercent = 0f;
    private static final boolean DEFAULTdontReturnForOverMaxSecondsDeviation = true;
    private boolean dontReturnForOverMaxSecondsDeviation = DEFAULTdontReturnForOverMaxSecondsDeviation;
    /**
     * Deviatia maxima a score-ului fata de score-ul melodiei ce se potriveste perfect.
     * Daca deviatia maxima > maxScoreDeviationPercentAgainstMatch atunci melodiile sunt diff.
     */
    private float maxScoreDeviationPercentAgainstMatch = 50f;
    private static final boolean DEFAULTdontReturnForOverMaxScoreDeviation = true;
    private boolean dontReturnForOverMaxScoreDeviation = DEFAULTdontReturnForOverMaxScoreDeviation;

    public MusicIndexSearcher(boolean searchMissingFilesToo) {
        this(searchMissingFilesToo, MusicIndex.INDEX_DIR);
    }

    public MusicIndexSearcher() {
        this(DEFAULTsearchMissingFilesToo, MusicIndex.INDEX_DIR);
    }

    public MusicIndexSearcher(boolean searchMissingFilesToo, String indexPath) {
        this(searchMissingFilesToo, new File(indexPath));
    }

    public MusicIndexSearcher(boolean searchMissingFilesToo, File indexPath) {
        this.searchMissingFilesToo = searchMissingFilesToo;
        this.indexPath = indexPath;
    }

    public MusicIndexSearcher(File indexPath) {
        this.indexPath = indexPath;
    }

    public static boolean isM3uGenerated(String m3uFileName) {
        return m3uFileName.indexOf(CONVERTED_PLAYLIST_SUFIX) > 0;
    }

    public M3uPlaylist searchSongs(M3uPlaylist m3uPlaylistToImport) {
        M3uPlaylist m3uPlaylistLocal = new M3uPlaylist(
            Util.suffixPath(m3uPlaylistToImport, CONVERTED_PLAYLIST_SUFIX), true);
        List<M3uPlaylistSong> foundSongs;
        for (M3uPlaylistSong songSearched : m3uPlaylistToImport.getSongs()) {
            if (!searchMissingFilesToo && !songSearched.exists()) {
                continue;
            }
            foundSongs = searchSong(songSearched);
            m3uPlaylistLocal.addSongs(foundSongs);
        }
        return m3uPlaylistLocal;
    }

    public List<M3uPlaylistSong> searchSong(M3uPlaylistSong songToSearch) {
        List<M3uPlaylistSong> songs = new ArrayList<>();
//        if (songToSearch.getMp3FilePathFull().indexOf("Beat6324.mp3") > 0) {
//            System.out.println("");
//        }
        List<Map<String, String>> resultSongs = searchSongDocIndex(songToSearch);
        if (resultSongs == null || resultSongs.isEmpty()) {
            return songs;
        }
        float secondsDeviationPercent;
        Map<String, String> resultSong;
        M3uPlaylistSong songFound;
        for (int i = 0, resultSongsSize = resultSongs.size(); i < resultSongsSize; i++) {
            resultSong = resultSongs.get(i);
            if (!foundSongCanMiss && !new File(resultSong.get("mp3FilePath")).exists()) {
                continue;
            }

            // avand in vedere ca resultSong este extras dintr-un index lucene acesta
            // ar trebui sa cuprinda toate informatiile, adica si fileSize respectiv seconds
            songFound = new M3uPlaylistSong(resultSong);
            songFound.putMisc("searchedSong", songToSearch);
            songFound.putMisc("idxSearchRezultOrder", String.valueOf(i));
            songs.add(songFound);
        }

        // Elimina sau NU marcheaza cu keep melodiile
        // cu peste deviatia maxima de secunde.
        List<M3uPlaylistSong> songsToRemove = new ArrayList<M3uPlaylistSong>();
        for (M3uPlaylistSong songFound1 : songs) {
            if (!this.dontSearchBySeconds && this.maxSecondsDeviationPercent > 0f) {
                secondsDeviationPercent = computeSecondsDeviationPercent(songFound1, songToSearch);
                songFound1.getMisc().put("secondsDeviationPercent", secondsDeviationPercent);
                if (secondsDeviationPercent > this.maxSecondsDeviationPercent) {
                    // songFound1 != songToSearch
                    // Se salveaza totusi in songFound1 pt raportari detaliate ulterioare ca de ex:
                    // pt songToSearch exista corespondenti in MusicIndex dar seconds difera prea mult.
                    if (dontReturnForOverMaxSecondsDeviation) {
                        songsToRemove.add(songFound1);
                        continue;
                    }
                }
            }
            if (songFound1.getSeconds() > songToSearch.getSeconds()) {
                songFound1.putMisc("keep", "found");
            } else if (songFound1.getSeconds() < songToSearch.getSeconds()) {
                songFound1.putMisc("keep", "searched");
            } else if (songFound1.getFileSize() > songToSearch.getFileSize()) {
                songFound1.putMisc("keep", "found");
            } else if (songFound1.getFileSize() < songToSearch.getFileSize()) {
                songFound1.putMisc("keep", "searched");
            } else {
                int wordsForFoundSize = Util.concatenate(songFound1.getWords(), null).length();
                int wordsForSearchedSize = Util.concatenate(songToSearch.getWords(), null).length();
                if (wordsForFoundSize > wordsForSearchedSize) {
                    songFound1.putMisc("keep", "found");
                } else if (wordsForFoundSize < wordsForSearchedSize) {
                    songFound1.putMisc("keep", "searched");
                } else {
                    songFound1.putMisc("keep", "any");
                }
            }
        }
        songs.removeAll(songsToRemove);

        // Se cauta EXACT songToSearch printre songs gasite cu lucene.
        // Este posibil ca unele duplicata sa aiba score > EXACT songToSearch !
        M3uPlaylistSong songMatch;
        if (songToSearch.getSeconds() > 0) {
            // s-a cautat dupa seconds & words
            songMatch = songToSearch.getPerfectMatchSong(songs, this);
            if (songMatch == null) {
                if (throwErrorOnPerfectMatchNotFound) {
                    throw new UnsupportedOperationException("No match!");
                } else {
                    return new ArrayList<>();
                }
            }
        } else {
            // practic s-a cautat doar dupa words
            songMatch = songs.get(0);
        }

        // Se pune scoreDeviationPercentAgainstMatch si daca e permis se elimina
        // songs cu deviatia > maxScoreDeviationPercentAgainstMatch.
        Float scoreForMatch = new Float((String) songMatch.getMisc("score"));
        Float scoreForFound, scoreDeviationPercentAgainstMatch;
        songsToRemove = new ArrayList<M3uPlaylistSong>();
        for (M3uPlaylistSong songFound1 : songs) {
            scoreForFound = new Float((String) songFound1.getMisc("score"));
            if (scoreForFound.compareTo(scoreForMatch) >= 0) {
                // melodie gasita ce pare un mult mai potrivit duplicat pt songToSearch decat chiar songMatch !
                continue;
            }
            scoreDeviationPercentAgainstMatch = computeScoreDeviationPercentAgainstMatch(scoreForFound,
                                                                                         scoreForMatch);
            songFound1.putMisc("scoreDeviationPercentAgainstMatch", scoreDeviationPercentAgainstMatch);
            if (scoreDeviationPercentAgainstMatch > maxScoreDeviationPercentAgainstMatch) {
                if (dontReturnForOverMaxScoreDeviation) {
                    songsToRemove.add(songFound1);
                } else {
                    songFound1.getMisc().remove("keep");
                }
            }
        }
        songs.removeAll(songsToRemove);
        return songs;
    }

    private float computeScoreDeviationPercentAgainstMatch(Float scoreForFound, Float scoreForMatch) {
        assert scoreForMatch > scoreForFound;
        return (scoreForMatch - scoreForFound) * 100f / scoreForMatch;
    }

    private float computeSecondsDeviationPercent(M3uPlaylistSong songFound, M3uPlaylistSong songSearched) {
        if (!songSearched.getM3uPlaylist().getLoadedFromDevice()) {
            if (!songFound.getMp3FileExtension().equalsIgnoreCase(songSearched.getMp3FileExtension())) {
                return maxSecondsDeviationPercent + 1f;
            }
        }

        // se calculeaza folosind dimensiunea in secunde
        if (songFound.getSeconds() > 0 && songSearched.getSeconds() > 0) {
            return Math.abs(songFound.getSeconds() - songSearched.getSeconds()) * 100f /
                Math.min(songFound.getSeconds(), songSearched.getSeconds());
        }

        return maxSecondsDeviationPercent + 1f;
    }

    public List<M3uPlaylistSong> searchSongInMemory(M3uPlaylistSong originalSong,
                                                    M3uPlaylistSong duplicateSong, int resultPerSong) {
        List<M3uPlaylistSong> songs = new ArrayList<M3uPlaylistSong>(2);
        songs.add(originalSong);
        songs.add(duplicateSong);
        Directory directory = MusicIndex.createInMemoryIndex(songs);
        List<Map<String, String>> songsFoundInMem = searchSongDocIndex(originalSong, directory,
                                                                       resultPerSong);
        List<M3uPlaylistSong> songsFound = new ArrayList<M3uPlaylistSong>();
        M3uPlaylistSong song;
        for (Map<String, String> song1 : songsFoundInMem) {
            song = new M3uPlaylistSong(song1);
            songsFound.add(song);
            song.putMisc("searchedSong", originalSong);
        }
        return songsFound;
    }

    public List<Map<String, String>> searchSongDocIndex(M3uPlaylistSong songToSearch) {
        try {
            Directory directory = FSDirectory.open(this.indexPath);
            return searchSongDocIndex(songToSearch, directory, resultPerSong);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Map<String, String>> searchSongDocIndex(M3uPlaylistSong songToSearch,
                                                        Directory directory, int resultPerSong) {
        IndexReader reader = null;
        String expression = null;
        try {
//            expression = createExpression(songToSearch);
//            expression = MusicIndex.removeAccents(expression);
//            songToSearch.putMisc("query", expression);
//            QueryParser parser = new QueryParser(expression, new StandardAnalyzer(MusicIndex.LUCENE_VERSION));
//            Query query = parser.parse(expression);

//            BooleanQuery booleanQuery = new BooleanQuery();
//            BooleanClause booleanClause = new BooleanClause(query, BooleanClause.Occur.SHOULD);
//            booleanQuery.add(booleanClause);
//
//            if (!songToSearch.isEmptyTag("date")) {
//                Query query1 = new TermQuery(new Term("date", songToSearch.getTag("date")));
//                booleanClause = new BooleanClause(query1, BooleanClause.Occur.SHOULD);
//                booleanQuery.add(booleanClause);
//            }
//
//            if (!songToSearch.isEmptyTag("duration")) {
//                Query query1 = new TermQuery(new Term("duration", songToSearch.getTag("duration")));
//                booleanClause = new BooleanClause(query1, BooleanClause.Occur.SHOULD);
//                booleanQuery.add(booleanClause);
//            }

            Query booleanQuery;
            if (songToSearch.isExtractedFromDevice()) {
                booleanQuery = createAAlTQuery(songToSearch);
            } else {
                booleanQuery = createQuery(songToSearch);
            }
            expression = booleanQuery.toString();
            songToSearch.putMisc("query", expression);

            reader = IndexReader.open(directory, true);
            Searcher searcher = new IndexSearcher(reader);

            TopDocs topDocs = searcher.search(booleanQuery, resultPerSong);
            if (topDocs.totalHits == 0) {
                if (!songToSearch.getTags().isEmpty()) {
//                System.out.println("\n" + songToSearch);
//                System.out.println(expression);
                    // cautare dupa artist="", album="", +rawMp3Details
                    booleanQuery = createEAEAlRQuery(songToSearch);
                    songToSearch.putMisc("EAEAlRQuery", booleanQuery.toString());
                    topDocs = searcher.search(booleanQuery, resultPerSong);
                    if (topDocs.totalHits > 0) {
//                    System.out.println("Found with (EAEAlRQuery): " + songToSearch.getMisc("EAEAlR"));
                    } else {
//                    System.out.println(topDocs.totalHits + " rezults (EAEAlR): " + booleanQuery.toString());
                        booleanQuery = createRQuery(songToSearch);
                        songToSearch.putMisc("RQuery", booleanQuery.toString());
                        topDocs = searcher.search(booleanQuery, resultPerSong);
                        if (topDocs.totalHits > 0) {
//                        System.out.println("Found with (RQuery): " + songToSearch.getMisc("RQuery"));
                        } else {
//                        System.out.println("\n" + songToSearch);
//                        System.out.println(expression);
//                        System.out.println(topDocs.totalHits + " rezults (RQuery): " + booleanQuery.toString());
                            booleanQuery = createTQuery(songToSearch);
                            songToSearch.putMisc("TQuery", booleanQuery.toString());
                            topDocs = searcher.search(booleanQuery, resultPerSong);
                            if (topDocs.totalHits > 0) {
//                            System.out.println("Found with (TQuery): " + songToSearch.getMisc("TQuery"));
                            } else {
                                System.out.println("\n" + songToSearch);
                                System.out.println(expression);
                                System.out.println(topDocs.totalHits + " rezults (query): " + booleanQuery.toString());
                                return null;
                            }
                        }
                    }
                } else {
                    System.out.println("\n" + songToSearch);
                    System.out.println(expression);
                    System.out.println(topDocs.totalHits + " rezults (TQuery): " + booleanQuery);
                    return null;
                }
            }
//            if (topDocs.totalHits > 1) {
//                System.out.println("totalHits = " + topDocs.totalHits + ": " + songToSearch);
//            }
            List<Map<String, String>> rez = new ArrayList<>();
            Document doc;
            Map<String, String> map;
            for (int j = 0; j < topDocs.totalHits && j < resultPerSong; j++) {
                doc = searcher.doc(topDocs.scoreDocs[j].doc);
                map = new HashMap<>();
                map.put("score", String.valueOf(topDocs.scoreDocs[j].score));
                map.put("query", expression);
                Field field;
                List fields = doc.getFields();
                for (Object o : fields) {
                    field = (Field) o;
                    map.put(field.name(), field.stringValue());
                }
                rez.add(map);
//                map.put("mp3FilePath", doc.get("mp3FilePath"));
//                map.put("rawMp3Details", doc.get("rawMp3Details"));
//                map.put("fileSize", doc.get("fileSize"));
//                map.put("seconds", doc.get("seconds"));
//                map.put("author", doc.get("author"));
//                if (doc.get("album") != null) {
//                    map.put("album", doc.get("album"));
//                }
//                if (doc.get("title") != null) {
//                    map.put("title", doc.get("title"));
//                }
//                if (doc.get("date") != null) {
//                    map.put("date", doc.get("date"));
//                }
//                if (doc.get("duration") != null) {
//                    map.put("duration", doc.get("duration"));
//                }
            }
            return rez;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("expression: " + expression);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private Query createAAlTQuery(M3uPlaylistSong song) {
        BooleanQuery booleanQuery = new BooleanQuery();

        // +author
        String author = song.getTag("author");
        author = author == null ? "" : author;
        BooleanQuery booleanQueryAuthor = new BooleanQuery();
        PhraseQuery phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("author", author));
        booleanQueryAuthor.add(phraseQuery, BooleanClause.Occur.SHOULD);
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("id3v1.author", author));
        booleanQueryAuthor.add(phraseQuery, BooleanClause.Occur.SHOULD);
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("id3v2.author", author));
        booleanQueryAuthor.add(phraseQuery, BooleanClause.Occur.SHOULD);
        booleanQuery.add(booleanQueryAuthor, BooleanClause.Occur.MUST);

        // +album
        String album = song.getTag("album");
        album = album == null ? "" : album;
        BooleanQuery booleanQuery1 = new BooleanQuery();
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("album", album));
        booleanQuery1.add(phraseQuery, BooleanClause.Occur.SHOULD);
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("id3v1.album", album));
        booleanQuery1.add(phraseQuery, BooleanClause.Occur.SHOULD);
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("id3v2.album", album));
        booleanQuery1.add(phraseQuery, BooleanClause.Occur.SHOULD);
        booleanQuery.add(booleanQuery1, BooleanClause.Occur.MUST);

        // +title
        booleanQuery.add(createTQuery(song), BooleanClause.Occur.MUST);

        return booleanQuery;
    }

    /**
     * Cateodata, mai ales pt wma (Ex: Florin Chilian-Chiar daca.wma), nu se poate
     * extrage author si album in vederea indexarii (adica author, album = "").
     * Totusi winamp va extrage de pe sony un url al melodiei in cauza care va contine
     * author si album corespunzatoare. In acest caz ar trebui sa se caute dupa
     * rawMp3Details = concatenate(artist, '-', title) SI album = "" !
     *
     * @param song
     * @return
     */
    private Query createEAEAlRQuery(M3uPlaylistSong song) {
        BooleanQuery booleanQuery = new BooleanQuery();

        // +author = ""
        String author = "";
        BooleanQuery booleanQueryAuthor = new BooleanQuery();
        PhraseQuery phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("author", author));
        booleanQueryAuthor.add(phraseQuery, BooleanClause.Occur.SHOULD);
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("id3v1.author", author));
        booleanQueryAuthor.add(phraseQuery, BooleanClause.Occur.SHOULD);
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("id3v2.author", author));
        booleanQueryAuthor.add(phraseQuery, BooleanClause.Occur.SHOULD);
        booleanQuery.add(booleanQueryAuthor, BooleanClause.Occur.MUST);

        // +album = ""
        String album = "";
        BooleanQuery booleanQuery1 = new BooleanQuery();
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("album", album));
        booleanQuery1.add(phraseQuery, BooleanClause.Occur.SHOULD);
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("id3v1.album", album));
        booleanQuery1.add(phraseQuery, BooleanClause.Occur.SHOULD);
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("id3v2.album", album));
        booleanQuery1.add(phraseQuery, BooleanClause.Occur.SHOULD);
        booleanQuery.add(booleanQuery1, BooleanClause.Occur.MUST);

        // +rawMp3Details
        assert !song.getTags().isEmpty();
        booleanQuery.add(createRQuery(song), BooleanClause.Occur.MUST);

        return booleanQuery;
    }

    /**
     * Cautare doar dupa rawMp3Details.
     * Cateodata windows media player citeste eronat album iar album != "" (deci este indexat).
     *
     * @param song
     * @return
     */
    private Query createRQuery(M3uPlaylistSong song) {
        // +rawMp3Details
        if (song.isEmptyTag("author")) {
            // rawMp3Details: +title
            PhraseQuery phraseQuery = new PhraseQuery();
            phraseQuery.add(new Term("rawMp3Details", song.getTag("title")));
            return phraseQuery;
        } else {
            // rawMp3Details: +(artist - title)
            PhraseQuery phraseQuery = new PhraseQuery();
            phraseQuery.add(new Term("rawMp3Details", song.getTag("author") + " - " + song.getTag("title")));
            return phraseQuery;
        }
    }

    private Query createTQuery(M3uPlaylistSong song) {
        // +title
        String title = song.getTag("title");
        BooleanQuery booleanQueryTitle = new BooleanQuery();
        PhraseQuery phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("title", title));
        booleanQueryTitle.add(phraseQuery, BooleanClause.Occur.SHOULD);
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("id3v1.title", title));
        booleanQueryTitle.add(phraseQuery, BooleanClause.Occur.SHOULD);
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("id3v2.title", title));
        booleanQueryTitle.add(phraseQuery, BooleanClause.Occur.SHOULD);
        phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("mp3FileNameNoExt", title));
        booleanQueryTitle.add(phraseQuery, BooleanClause.Occur.SHOULD);
        return booleanQueryTitle;
    }

    /**
     * Search by seconds & words.
     *
     * @param song
     * @return
     */
    private Query createQuery(M3uPlaylistSong song) {
//        if (song.getFileSize() > 0) {
//            BooleanQuery booleanQuery1 = new BooleanQuery();
//            for (int i = -1 * M3uPlaylistSong.MAX_ABSOLUTE_SIZE_DEVIATION;
//                 i <= M3uPlaylistSong.MAX_ABSOLUTE_SIZE_DEVIATION; i++) {
//                booleanQuery1.add(new BooleanClause(new TermQuery(
//                        new Term("fileSize", String.valueOf(song.getFileSize() + i))), BooleanClause.Occur.SHOULD));
//            }
//            if (searchedFileSizeMustBeTheSame) {
//                booleanQuery.add(booleanQuery1, BooleanClause.Occur.MUST);
//            } else {
//                booleanQuery.add(booleanQuery1, BooleanClause.Occur.SHOULD);
//            }
////            if (searchedFileSizeMustBeTheSame) {
////                booleanQuery.add(new BooleanClause(new TermQuery(
////                        new Term("fileSize", String.valueOf(song.getFileSize()))), BooleanClause.Occur.MUST));
////            } else {
////                booleanQuery.add(new BooleanClause(new TermQuery(
////                        new Term("fileSize", String.valueOf(song.getFileSize()))), BooleanClause.Occur.SHOULD));
////            }
//        }

        BooleanQuery booleanQuery = new BooleanQuery();

        // seconds
        if (song.getSeconds() > 0 && !dontSearchBySeconds) {
            BooleanQuery booleanQuery1 = new BooleanQuery();
            // lasam o variatie de 1s !!!
            for (int i = -1 * M3uPlaylistSong.MAX_ABSOLUTE_SECONDS_DEVIATION;
                 i <= M3uPlaylistSong.MAX_ABSOLUTE_SECONDS_DEVIATION; i++) {
                booleanQuery1.add(new BooleanClause(new TermQuery(
                    new Term("seconds", String.valueOf(song.getSeconds() + i))),
                                                    BooleanClause.Occur.SHOULD));
            }
            if (maxSecondsDeviationPercent <= 0f) {
                booleanQuery.add(booleanQuery1, BooleanClause.Occur.MUST);
            } else {
                booleanQuery.add(booleanQuery1, BooleanClause.Occur.SHOULD);
            }
        }

        // words
        booleanQuery.add(createWordsQuery(song), BooleanClause.Occur.MUST);

        return booleanQuery;
    }

    private BooleanQuery createWordsQuery(M3uPlaylistSong song) {
        Collection<String> words = song.getWords();
        BooleanQuery booleanQuery1 = new BooleanQuery();
        for (String word : words) {
            booleanQuery1.add(new BooleanClause(new TermQuery(
                new Term("words", word)), BooleanClause.Occur.SHOULD));
        }
        return booleanQuery1;
    }

    private String createExpression(M3uPlaylistSong song) {
        StringBuilder sb = new StringBuilder();

//        if (song.getFileSize() > 0) {
//            if (searchedFileSizeMustBeTheSame) {
//                sb.append('+');
//            }
//            sb.append("fileSize:").append(song.getFileSize());
//        }

        if (song.getSeconds() > 0 && !dontSearchBySeconds) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            if (maxSecondsDeviationPercent <= 0f) {
                sb.append('+');
            }
            // lasam o variatie de 1s !!!
            sb.append("seconds:(");
            for (int i = -1 * M3uPlaylistSong.MAX_ABSOLUTE_SECONDS_DEVIATION; i <= M3uPlaylistSong.MAX_ABSOLUTE_SECONDS_DEVIATION; i++) {
                sb.append(song.getSeconds() + i).append(' ');
            }
            sb.deleteCharAt(sb.length() - 1).append(')');
        }

        Collection<String> words = song.getWords();
        if (!words.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("+words:(");
            for (String word : words) {
                sb.append(word);
                sb.append(' ');
            }
            sb.deleteCharAt(sb.length() - 1).append(')');
        }

//        if (!song.isEmptyTag("date")) {
//            if (sb.length() > 0) {
//                sb.append(" ");
//            }
//            sb.append("date:");
//            sb.append('\"').append(song.getTag("date")).append('\"');
//        }

//        if (!song.isEmptyTag("duration")) {
//            if (sb.length() > 0) {
//                sb.append(" ");
//            }
//            sb.append("duration:");
//            sb.append('\"').append(song.getTag("duration")).append('\"');
//        }

        return sb.toString();
    }

    public int getResultPerSong() {
        return resultPerSong;
    }

    public void setResultPerSong(int resultPerSong) {
        this.resultPerSong = resultPerSong;
    }

    public boolean getFoundSongCanMiss() {
        return foundSongCanMiss;
    }

    public void setFoundSongCanMiss(boolean foundSongCanMiss) {
        this.foundSongCanMiss = foundSongCanMiss;
    }

    public float getMaxSecondsDeviationPercent() {
        return maxSecondsDeviationPercent;
    }

    public void setMaxSecondsDeviationPercent(float maxSecondsDeviationPercent) {
        this.maxSecondsDeviationPercent = maxSecondsDeviationPercent;
    }

    public File getIndexPath() {
        return indexPath;
    }

    public void setIndexPath(File indexPath) {
        this.indexPath = indexPath;
    }

    public boolean getSearchMissingFilesToo() {
        return searchMissingFilesToo;
    }

    public void setSearchMissingFilesToo(boolean searchMissingFilesToo) {
        this.searchMissingFilesToo = searchMissingFilesToo;
    }

    public boolean getDontReturnForOverMaxSecondsDeviation() {
        return dontReturnForOverMaxSecondsDeviation;
    }

    public void setDontReturnForOverMaxSecondsDeviation(boolean dontReturnForOverMaxSecondsDeviation) {
        this.dontReturnForOverMaxSecondsDeviation = dontReturnForOverMaxSecondsDeviation;
    }

    public float getMaxScoreDeviationPercentAgainstMatch() {
        return maxScoreDeviationPercentAgainstMatch;
    }

    public void setMaxScoreDeviationPercentAgainstMatch(float maxScoreDeviationPercentAgainstMatch) {
        this.maxScoreDeviationPercentAgainstMatch = maxScoreDeviationPercentAgainstMatch;
    }

    public boolean getDontReturnForOverMaxScoreDeviation() {
        return dontReturnForOverMaxScoreDeviation;
    }

    public void setDontReturnForOverMaxScoreDeviation(boolean dontReturnForOverMaxScoreDeviation) {
        this.dontReturnForOverMaxScoreDeviation = dontReturnForOverMaxScoreDeviation;
    }

    public boolean getDontSearchBySeconds() {
        return dontSearchBySeconds;
    }

    public void setDontSearchBySeconds(boolean dontSearchBySeconds) {
        this.dontSearchBySeconds = dontSearchBySeconds;
    }

    public boolean getThrowErrorOnPerfectMatchNotFound() {
        return throwErrorOnPerfectMatchNotFound;
    }

    public void setThrowErrorOnPerfectMatchNotFound(boolean throwErrorOnPerfectMatchNotFound) {
        this.throwErrorOnPerfectMatchNotFound = throwErrorOnPerfectMatchNotFound;
    }
}
