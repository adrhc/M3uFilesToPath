package M3uFilesToPath.m3u;

import M3uFilesToPath.lucene.MusicIndex;
import M3uFilesToPath.util.Util;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: adrr
 * Date: Jan 12, 2011
 * Time: 7:58:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class M3uPlaylist {
    private static final String mp3FirstLine = "#EXTM3U";
    public static final String detailsHeaderPrefix = "#EXTINF:";
    private String m3uFilePath;
    /**
     * Nu se extrage si dimensiunea fisierului mp3.
     * In aceasta situatie valoarea lui dontThrowErrForMissingFilesWhenComputingSize nu mai are importanta.
     * Anuleaza efectul lui MusicIndexSearcher.useUniqueFileSign = true (cautarea dupa semnatura fiserului mp3) !
     */
    private boolean dontComputeFileSize;

    /**
     * Nu se extrage si dimensiunea fisierului mp3 daca M3uPlaylistSong.seconds > 0.
     * Anuleaza efectul lui MusicIndexSearcher.useUniqueFileSign = true (cautarea dupa semnatura fiserului mp3) !
     */
    private static final boolean DEFAULTdontComputeFileSizeIfExistsSeconds = true;
    private boolean dontComputeFileSizeIfExistsSeconds = DEFAULTdontComputeFileSizeIfExistsSeconds;

    private boolean writeFullMp3PathToM3u;
    /**
     * Are sens atunci cand se extrage dimensiunea fisierului mp3 si dontComputeFileSize = false.
     * false: crapa la incarcarea playlist-ului daca fisierul referit lipseste de pe HDD
     * true: incarca si fisierele ce lipsesc de pe HDD dar sunt referite din playlist-ul m3u/m3u8
     */
    private boolean dontThrowErrForMissingFilesWhenComputingSize;
    private List<M3uPlaylistSong> songs;
    /**
     * Arata daca songs este incarcat dintr-un playlist de tip winamp-device.
     */
    private boolean loadedFromDevice;
    private boolean dontReadTags = true;

    // #EXTINF:n,Artist - Title
    // path\fileName.mp3
//    #EXTINF:110,Corul "Madrigal" - Leagãnul lui Isus
//    New\Colinde Corul Madrigal\Leaganul lui Isus.mp3

    public M3uPlaylist(boolean initSongs) {
        this(null, initSongs);
    }

    public M3uPlaylist(String m3uFilePath) {
        this(m3uFilePath, false);
    }

    public M3uPlaylist(String m3uFilePath, boolean initSongs) {
        this.m3uFilePath = m3uFilePath;
        if (initSongs) {
            songs = new ArrayList<M3uPlaylistSong>();
        }
    }

    /**
     * Pt fiecare grup de melodii unic dpv al (seconds & words) se va lasa in this.songs
     * doar una (cea cu score cel mai mare). Celelalte din grup se vor pune in misc.duplicatesList
     * pt cea care s-a pastrat in this.songs. Astfel daca in final this.songs va avea o singura melodie
     * asta inseamna ca restul initial existente in this.songs au aceleas (seconds & words).
     * Pt fiecare melodie existenta initial in this.songs, song1, se va seta song1.misc.duplicates
     * care va specifica numarul de melodii cu aceleas (seconds & words) - se numara inclusiv song1,
     * deci song1.misc.duplicates va fi minim 1.
     */
    public void compactSongsBySecondsAndWords() {
        Map<String, M3uPlaylistSong> secondsAndWordsList = new TreeMap<String, M3uPlaylistSong>();
        String secondsAndWords;
        M3uPlaylistSong keptSong;
        Integer duplicatesCount;
        List<M3uPlaylistSong> duplicatesList;
        List<M3uPlaylistSong> songsToDelete = new ArrayList<M3uPlaylistSong>();
        String scoreForKeptSong, scoreForCurrentSong;
        for (M3uPlaylistSong song : songs) {
            secondsAndWords = String.valueOf(song.getSeconds()) + ' ' +
                    Util.concatenate(song.getWords(), " ");
            keptSong = secondsAndWordsList.get(secondsAndWords);
            if (keptSong == null) {
                secondsAndWordsList.put(secondsAndWords, song);
                song.getMisc().put("duplicatesCount", 1);
            } else {
                duplicatesCount = (Integer) keptSong.getMisc().get("duplicatesCount") + 1;
                duplicatesList = (List) keptSong.getMisc().get("duplicatesList");
                if (duplicatesList == null) {
                    duplicatesList = new ArrayList<M3uPlaylistSong>();
                }
                scoreForKeptSong = (String) keptSong.getMisc().get("score");
                scoreForCurrentSong = (String) song.getMisc().get("score");
                if (new Float(scoreForKeptSong) >= new Float(scoreForCurrentSong)) {
                    songsToDelete.add(song);
                    duplicatesList.add(song);
                    keptSong.getMisc().put("duplicatesCount", duplicatesCount);
                    keptSong.getMisc().put("duplicatesList", duplicatesList);
                } else {
                    songsToDelete.add(keptSong);
                    duplicatesList.add(keptSong);
                    song.getMisc().put("duplicatesCount", duplicatesCount);
                    song.getMisc().put("duplicatesList", duplicatesList);
                    keptSong.getMisc().remove("duplicatesCount");
                    keptSong.getMisc().remove("duplicatesList");
                }
            }
        }
        songs.removeAll(songsToDelete);
    }

    public String compareToOriginal(M3uPlaylist m3uPlaylistEXTERN, boolean warnAboutDiffFolders) {
        return compareToOriginal(m3uPlaylistEXTERN, warnAboutDiffFolders, null);
    }

    public void addSong(M3uPlaylistSong newSong) {
        newSong.setM3uPlaylist(this);
        songs.add(newSong);
    }

    public void addSongs(Collection<M3uPlaylistSong> newSongs) {
        addSongs(newSongs, null);
    }

    public void addSongs(Collection<M3uPlaylistSong> newSongs, SongFilter songFilter) {
        for (M3uPlaylistSong song : newSongs) {
            if (songFilter != null && !songFilter.accept(song)) {
                continue;
            }
            addSong(song);
        }
    }

    public String compareToOriginal(M3uPlaylist m3uPlaylistEXTERN, boolean warnAboutDiffFolders,
                                    SongFilter foundSongFilter) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        Map<String, Object> misc;
        M3uPlaylistSong searchedSong = null;
        String pathEXTERN, convertedSongPath;
        printWriter.println("Original: " + m3uPlaylistEXTERN.getM3uFilePath());
        printWriter.println("Generated: " + this.getM3uFilePath());
        printWriter.println("max in mem score dev:\t\t" + M3uPlaylistSong.MAX_IN_MEMORY_SCORE_DEV);
        printWriter.println("abs seconds dev:\t\t\t" + M3uPlaylistSong.MAX_ABSOLUTE_SECONDS_DEVIATION);
        printWriter.println("Extern:\t\t\t\t\t\t" + m3uPlaylistEXTERN.getSongs().size());
        printWriter.println("Found:\t\t\t\t\t\t" + this.getSongs().size());
        int idxFound = stringWriter.getBuffer().length();
        boolean diffPath, diffSongs, secondsDevNotZero, scoreDevNotZero;
        Float secondsDeviationPercent, scoreDeviationPercentAgainstMatch;
        String keep;
        int countFoundInIndexButDiff = 0, countFoundAndEq = 0;
        DecimalFormat df = new DecimalFormat("#,###,##0.00000");
        for (M3uPlaylistSong foundSong : this.getSongs()) {
            if (foundSongFilter != null && !foundSongFilter.accept(foundSong)) {
                continue;
            }
            misc = foundSong.getMisc();
            searchedSong = (M3uPlaylistSong) misc.get("searchedSong");

            if (!misc.get("idxSearchRezultOrder").equals("0")) {
                printWriter.println("\nidxSearchRezultOrder: " +
                        misc.get("idxSearchRezultOrder") + '\n' + misc.get("query"));
            }

            pathEXTERN = searchedSong.getMp3FilePathFull().toLowerCase();
            convertedSongPath = foundSong.getMp3FilePathFull().toLowerCase();
            diffPath = !pathEXTERN.equals(convertedSongPath);

            secondsDeviationPercent = (Float) foundSong.getMisc().get("secondsDeviationPercent");
            secondsDevNotZero = secondsDeviationPercent != null && secondsDeviationPercent > 0f;

            scoreDeviationPercentAgainstMatch = (Float) foundSong.getMisc()
                    .get("scoreDeviationPercentAgainstMatch");
            scoreDevNotZero = scoreDeviationPercentAgainstMatch != null && scoreDeviationPercentAgainstMatch > 0f;

            diffSongs = secondsDevNotZero || scoreDevNotZero || diffPath ||
                    foundSong.getFileSize() != searchedSong.getFileSize();

            if (!diffSongs) {
                continue;
            }

            pathEXTERN = new File(pathEXTERN).getName();
            convertedSongPath = new File(convertedSongPath).getName();

            printWriter.println("\n\tquery:\t\t" + misc.get("query"));
            printWriter.println("\tsearched:\t" + searchedSong);
            printWriter.println("\tfound:\t\t" + foundSong);
            if (warnAboutDiffFolders && diffPath && pathEXTERN.equals(convertedSongPath)) {
                printWriter.println("\tdoar folderele sunt diferite");
            }

            printWriter.print("\tscore:\t\t" + foundSong.getMisc("score"));
            if (foundSong.getMisc("inMemScoreDev") != null) {
                printWriter.print("\tinMemScoreDev:\t" + foundSong.getMisc("inMemScoreDev"));
            }

            keep = (String) foundSong.getMisc().get("keep");
            if (keep == null) {
                countFoundInIndexButDiff++;
                printWriter.println("\tmelodii diferite");
            } else {
                countFoundAndEq += keep.equals("any") ? 1 : 0;
                printWriter.println("\tkeep:\t" + keep);
            }

            if (secondsDevNotZero) {
                printWriter.print("\tdiff:\t\tsec dev = ");
                printWriter.print(df.format(secondsDeviationPercent));
                printWriter.println("%");
            }

            if (scoreDevNotZero) {
                printWriter.print("\tdiff:\t\tscore dev = ");
                printWriter.print(df.format(scoreDeviationPercentAgainstMatch));
                printWriter.println("%");
            }
        }
        printWriter.println();
        for (M3uPlaylistSong songEXTERN : m3uPlaylistEXTERN.getSongs()) {
            for (M3uPlaylistSong songLOCAL : this.getSongs()) {
                searchedSong = (M3uPlaylistSong) songLOCAL.getMisc().get("searchedSong");
                if (searchedSong == songEXTERN) {
                    break;
                }
            }
            if (searchedSong != songEXTERN) {
                printWriter.print("Nu s-a gasit: " + songEXTERN.toString());
                printWriter.print(" size=" + songEXTERN.getFileSize());
                printWriter.println(" query=" + songEXTERN.getMisc("query"));
            }
        }
        stringWriter.getBuffer().insert(idxFound, "Found in index but diff:\t" +
                countFoundInIndexButDiff + "\nFound and eq:\t\t\t\t" + countFoundAndEq + '\n');
        return stringWriter.getBuffer().toString();
    }

    public List<M3uPlaylist> getDuplicates(String duplicatesBasePath, boolean searchDupForMissingFiles,
                                           boolean foundSongCanMiss,
                                           float maxScoreDeviationPercentAgainstMatch,
                                           boolean dontReturnForOverMaxScoreDeviation) {
        if (songs == null || songs.isEmpty()) {
            dontComputeFileSizeIfExistsSeconds = false;
            dontReadTags = false;
            loadPlayList();
        }
        M3uPlaylist playListWithDuplicates;
        List<M3uPlaylist> duplicatesPlayLists = new ArrayList<M3uPlaylist>();
        Set<M3uPlaylistSong> duplicatesAlreadyFound = new HashSet<M3uPlaylistSong>();
        for (M3uPlaylistSong songToSearchForDups : songs) {
            if (!searchDupForMissingFiles && !songToSearchForDups.exists()) {
                continue;
            }

//            if (songToSearchForDups.getMp3FileName().equals("0475. The Platters - Only You.mp3")) {
//                System.out.println("");
//            }

            if (songToSearchForDups.getSeconds() == 0 || songToSearchForDups.getFileSize() == 0L) {
                // nu se cauta duplicate pt fisiere cu dimensiune 0 bytes
                continue;
            }

            if (songAlreadyFoundAsDuplicate(songToSearchForDups, duplicatesAlreadyFound)) {
                // acest fisier deja s-a cautat
                continue;
            }

            playListWithDuplicates = songToSearchForDups.getDupPlayList(duplicatesBasePath,
                                                                        maxScoreDeviationPercentAgainstMatch,
                                                                        dontReturnForOverMaxScoreDeviation,
                                                                        foundSongCanMiss);
            if (playListWithDuplicates == null) {
                // nu s-au gasit duplicate pt songToSearchForDups
                continue;
            }
            duplicatesPlayLists.add(playListWithDuplicates);

            // E f.posibil ca melodiile din songs sa se afle printre duplicatele gasite
            // si in plus sa nu fi fost inca analizate dpv al duplicabilitatii. Nici nu
            // are sens a mai fi analizate de vreme ce s-au gasit deja ca fiind duplicatele
            // lui songToSearchForDups asa ca se vor adauga la duplicatesAlreadyFound.
            for (M3uPlaylistSong duplicateSong : playListWithDuplicates.getSongs()) {
                duplicatesAlreadyFound.add(duplicateSong);
            }
        }
        return duplicatesPlayLists;
    }

    /**
     * Se verifica daca are sens cautarea duplicatelor pt songToSearchForDups; de ex daca song1
     * a fost deja gasit ca duplicat al altui song2 cautat anterior pt duplicate atunci nu mai
     * are sens sa se caute si song1 de duplicate.
     * <p/>
     * Duplicatele gasite isi sunt provenite din MusicIndex in care s-au gasit deci mp3FilePath = mp3FilePathFull !
     * Melodia cautata, songToSearchForDups, ar trebuie sa fie preluata dintr-un playlist in care sa fie stocata
     * cu calea identica (total sau partial) cu cea de la momentul creerii indexului lucene pt ca aceasta metoda
     * sa aiba sens; altfel nici o melodie songToSearchForDups nu se va identifica drept deja gasita ca duplicat si
     * deci nenecesar a se mai cauta.
     *
     * @param songToSearchForDups
     * @param duplicatesAlreadyFound
     * @return
     */
    private boolean songAlreadyFoundAsDuplicate(M3uPlaylistSong songToSearchForDups,
                                                Set<M3uPlaylistSong> duplicatesAlreadyFound) {
        String path = songToSearchForDups.getMp3FilePath().toLowerCase();
        // existingPath poate fi o cale a unei melodii-duplicat, adica o cale relativa
        for (M3uPlaylistSong duplicateSong : duplicatesAlreadyFound) {
            if (duplicateSong.getMp3FilePath().toLowerCase().indexOf(path) >= 0) {
                if (duplicateSong.getSeconds() != songToSearchForDups.getSeconds()) {
                    // secunde diferite
                    continue;
                }
                if (duplicateSong.getRawMp3Details() == null &&
                        songToSearchForDups.getRawMp3Details() != null ||
                        duplicateSong.getRawMp3Details() != null &&
                                songToSearchForDups.getRawMp3Details() == null) {
                    // rawMp3Details diferite
                    continue;
                }
                if (duplicateSong.getRawMp3Details().equals(songToSearchForDups.getRawMp3Details())) {
                    // rawMp3Details egale
                    return true;
                }
            }
        }
        return false;
    }

    public void index(boolean forceReCreateIndex) {
        if (songs.isEmpty()) {
            loadPlayList();
        }
        MusicIndex musicIndex = new MusicIndex(this, forceReCreateIndex);
        musicIndex.indexM3uPlaylist();
    }

    public void write() {
        write(null);
    }

    public void write(SongFilter songFilter) {
        PrintWriter writer = null;
        try {
            (new File(m3uFilePath)).getParentFile().mkdirs();
//            if (m3uFilePath.endsWith("m3u8")) {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(m3uFilePath), "utf-8"));
//            } else {
//                writer = new PrintWriter(new FileOutputStream(m3uFilePath));
//            }
            writer.println(mp3FirstLine);
            for (M3uPlaylistSong song : songs) {
                if (songFilter != null && !songFilter.accept(song)) {
                    continue;
                }
                writer.print(detailsHeaderPrefix);
                writer.print(song.getSeconds());
                if (song.getRawMp3Details() != null) {
                    writer.print(',');
                    writer.println(song.getRawMp3Details());
                }
                if (writeFullMp3PathToM3u) {
                    writer.println(song.getMp3FilePathFull());
                } else {
                    writer.println(song.getMp3FilePath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void loadWinampDevicePlayList(SongFilter songFilter) {
        loadedFromDevice = true;
        LineNumberReader lineNumberReader = null;
        try {
            if (m3uFilePath.endsWith("m3u8")) {
                lineNumberReader = new LineNumberReader(
                        new BufferedReader(new InputStreamReader(new FileInputStream(m3uFilePath), "utf-8")));
            } else {
                lineNumberReader = new LineNumberReader(
                        new BufferedReader(new InputStreamReader(new FileInputStream(m3uFilePath))));
            }
            String sLine;
            boolean foundDetails = false;
            songs = new ArrayList<M3uPlaylistSong>();
            M3uPlaylistSong song;
            boolean mp3FirstLineRead = false;
            while ((sLine = lineNumberReader.readLine()) != null) {
                if (sLine.trim().length() == 0) {
                    continue;
                }
                if (!mp3FirstLineRead &&
                        (sLine.startsWith(mp3FirstLine) || sLine.substring(1).startsWith(mp3FirstLine))) {
                    // sLine.substring(0) -> poate reprezenta BOM de la UTB-8 with BOM
                    mp3FirstLineRead = true;
                    continue;
                }
                if (sLine.startsWith(detailsHeaderPrefix)) {
                    foundDetails = true;
                    songs.add(new M3uPlaylistSong(sLine, this));
                } else {
                    if (foundDetails) {
                        song = songs.get(songs.size() - 1);
                        song.setDeviceSongPath(sLine);
                        foundDetails = false;
                    } else {
                        song = new M3uPlaylistSong(null, null, this);
                        song.setDeviceSongPath(sLine);
                        songs.add(song);
                    }
                }
            }
            List<M3uPlaylistSong> songsToRemove = new ArrayList<M3uPlaylistSong>();
            if (songFilter != null) {
                for (M3uPlaylistSong song1 : songs) {
                    if (!songFilter.accept(song1)) {
                        songsToRemove.add(song1);
                    }
                }
            }
            songs.removeAll(songsToRemove);
        } catch (IOException e) {
            e.printStackTrace();
            songs = null;
        } finally {
            if (lineNumberReader != null) {
                try {
                    lineNumberReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadAudioTags() {
        for (M3uPlaylistSong song : songs) {
//            System.out.println(song);
            if (song.exists() && song.getSeconds() > 0) {
                song.fillWithTags();
            } else {
                System.out.println("Tags not loaded for: " + song);
            }
        }
    }

    /**
     * Media player:
     * \MUSIC\albumartistest12\albumtest123\08-07-18_113941.mp3
     */
    public void loadPlayList() {
        loadM3uPlayList();
        if (!dontReadTags) {
            loadAudioTags();
        }
    }

    public void loadM3uPlayList() {
        LineNumberReader lineNumberReader = null;
        try {
            if (m3uFilePath.endsWith("m3u8")) {
                lineNumberReader = new LineNumberReader(
                        new BufferedReader(new InputStreamReader(new FileInputStream(m3uFilePath), "utf-8")));
            } else {
                lineNumberReader = new LineNumberReader(
                        new BufferedReader(new InputStreamReader(new FileInputStream(m3uFilePath))));
            }
            String sLine;
            boolean foundDetails = false;
            songs = new ArrayList<M3uPlaylistSong>();
            M3uPlaylistSong song;
            boolean mp3FirstLineRead = false;
            while ((sLine = lineNumberReader.readLine()) != null) {
                if (sLine.trim().length() == 0) {
                    continue;
                }
                if (!mp3FirstLineRead &&
                        (sLine.startsWith(mp3FirstLine) || sLine.substring(1).startsWith(mp3FirstLine))) {
                    // sLine.substring(0) -> poate reprezenta BOM de la UTB-8 with BOM
                    mp3FirstLineRead = true;
                    continue;
                }
                if (sLine.startsWith(detailsHeaderPrefix)) {
                    foundDetails = true;
                    songs.add(new M3uPlaylistSong(sLine, this));
                } else {
                    if (foundDetails) {
                        song = songs.get(songs.size() - 1);
                        song.setMp3FilePath(sLine);
                        foundDetails = false;
                    } else {
                        song = new M3uPlaylistSong(sLine, null, this);
                        songs.add(song);
                    }
                    if (!song.isSupportedAudio()) {
                        songs.remove(song);
                        continue;
                    }
                    if (!this.dontComputeFileSize &&
                            !(this.dontComputeFileSizeIfExistsSeconds && song.getSeconds() > 0)) {
                        song.computeFileSize();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            songs = null;
        } finally {
            if (lineNumberReader != null) {
                try {
                    lineNumberReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public M3uPlaylist substract(M3uPlaylist m3uPlaylistToSubstract) {
        M3uPlaylist m3uPlaylist = new M3uPlaylist(true);
        m3uPlaylist.setLoadedFromDevice(loadedFromDevice);
        for (M3uPlaylistSong song : songs) {
            if (!m3uPlaylistToSubstract.getSongs().contains(song)) {
                m3uPlaylist.addSong(song);
            }
        }
        return m3uPlaylist;
    }

    public M3uPlaylistSong getSong(int idx) {
        return songs.get(idx);
    }

    public List<M3uPlaylistSong> getSongs() {
        return songs;
    }

    public String toString(boolean showDetails, boolean showFilePath,
                           boolean showMp3FilePathParts, String[] miscToShow) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, songsSize = songs.size(); i < songsSize; i++) {
            M3uPlaylistSong song = songs.get(i);
            sb.append(song.toString(showDetails, showFilePath, showMp3FilePathParts, miscToShow));
            if (i < songsSize - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    public String toString() {
        return toString(false, false, false, null);
    }

    public void setM3uFilePath(String m3uFilePath) {
        this.m3uFilePath = m3uFilePath;
    }

    public boolean getDontComputeFileSize() {
        return dontComputeFileSize;
    }

    public boolean getDontThrowErrForMissingFilesWhenComputingSize() {
        return dontThrowErrForMissingFilesWhenComputingSize;
    }

    public void setDontThrowErrForMissingFilesWhenComputingSize(
            boolean dontThrowErrForMissingFilesWhenComputingSize) {
        this.dontThrowErrForMissingFilesWhenComputingSize = dontThrowErrForMissingFilesWhenComputingSize;
    }

    public void setDontComputeFileSize(boolean dontComputeFileSize) {
        this.dontComputeFileSize = dontComputeFileSize;
    }

    public String getM3uFilePath() {
        return m3uFilePath;
    }

    public boolean getWriteFullMp3PathToM3u() {
        return writeFullMp3PathToM3u;
    }

    public void setWriteFullMp3PathToM3u(boolean writeFullMp3PathToM3u) {
        this.writeFullMp3PathToM3u = writeFullMp3PathToM3u;
    }

    public boolean getDontComputeFileSizeIfExistsSeconds() {
        return dontComputeFileSizeIfExistsSeconds;
    }

    public void setDontComputeFileSizeIfExistsSeconds(boolean dontComputeFileSizeIfExistsSeconds) {
        this.dontComputeFileSizeIfExistsSeconds = dontComputeFileSizeIfExistsSeconds;
    }

    public boolean getLoadedFromDevice() {
        return loadedFromDevice;
    }

    public void setLoadedFromDevice(boolean loadedFromDevice) {
        this.loadedFromDevice = loadedFromDevice;
    }

    public boolean isDontReadTags() {
        return dontReadTags;
    }

    public void setDontReadTags(boolean dontReadTags) {
        this.dontReadTags = dontReadTags;
    }
}
