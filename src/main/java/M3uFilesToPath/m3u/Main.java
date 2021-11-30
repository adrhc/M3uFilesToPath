package M3uFilesToPath.m3u;

import M3uFilesToPath.lucene.MusicIndex;
import M3uFilesToPath.lucene.MusicIndexSearcher;
import M3uFilesToPath.util.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static M3uFilesToPath.lucene.MusicIndex.ymd;

/**
 * Created by IntelliJ IDEA.
 * User: adrr
 * Date: Jan 13, 2011
 * Time: 5:59:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    public static final boolean WARN_ABOUT_DIFF_FOLDERS = false;

    //    public static final String M3UFile = "D:\\MUZICA\\2011-03-01 JOB d-muzica.m3u8";
    //    public static final String duplicatesList = "D:\\MUZICA\\Temp\\" + ymd +
    //            " duplicated songs\\" + ymd + " duplicated songs.m3u8";
    //    public static final String originalList = "D:\\MUZICA\\Temp\\" + ymd +
    //            " duplicated songs\\" + ymd + " original songs.m3u8";
    //    public static final String MyPlaylistsPath = "c:\\Users\\adr\\Music\\Playlists\\";
    //    public static final String ARCHIVE_DIR = "d:\\Projects\\Archives\\";
    //    public static final String CURRENT_ARCHIVE_DIR = ARCHIVE_DIR + "M3uFilesToPath home 2011-03-05\\";

    public static final String MyPlaylistsPath = "C:\\Users\\adpetre\\Music\\Playlists\\";
    public static final String MUSIC_TEMP_DIR = "C:\\Users\\adpetre\\Temp\\Muzica\\";
    public static final String M3UFile = MyPlaylistsPath + "MUZICA.m3u8";
    public static final String duplicatesList = MUSIC_TEMP_DIR + ymd +
            " duplicated songs\\" + ymd + " duplicated songs.m3u8";
    public static final String originalList = MUSIC_TEMP_DIR + ymd +
            " duplicated songs\\" + ymd + " original songs.m3u8";
    public static final String ARCHIVE_DIR = MUSIC_TEMP_DIR + "Archives\\";
    public static final String CURRENT_ARCHIVE_DIR = ARCHIVE_DIR + "M3uFilesToPath home 2021-11-30\\";
    /**
     * Are sens a se lista aici:
     * - folderele ce contin doar melodii bine documentate (tag-uri) printre care nu exista duplicate
     * - folderele din care se doreste a nu se sterge melodii
     * - melodii ce se doreste a nu se sterge
     */
    public static final String[] TO_DELETE_EXCEPTED_SONG = new String[] {
            //            " India - Track ",
            //            "100 Super Oldies - To Good To Be Forgotten ResourceRG Music Reidy\\02 - Gerry & The Pacemakers - How Do You Do It.mp3",
            "All Time Top 1000\\",
            "Beatles\\",
            //            "Car032008\\07-Pet Shop Boys-Actually-It's A Sin.mp3",
            //            "Diverse10\\Dirty Dancing\\Be my baby.MP3",
            //            "Diverse10\\Elvis Presley\\Elvis Presley - Paralized.mp3",
            "Diverse10\\Goran Bregovic\\",
            //            "Diverse11\\Depeche Mode - Don't.mp3",
            //            "Diverse12\\House\\DJ Tomcraft - The mission.mp3",
            //            "Diverse12\\S.O.A.P. - Soap Is In The Air.mp3",
            "Diverse13\\Yngwie Malmsteen\\",
            //            "Diverse14\\Dj Sakin & Friends -Nomansl.mp3",
            "Diverse15\\A B B A\\",
            "Diverse15\\Arabian Dance\\",
            "Diverse15\\Avril Lavigne\\",
            //            "Diverse15\\Azido Da Bass\\Azido Da Bass - Track 11.mp3",
            "Diverse15\\Boney M\\",
            "Diverse16\\Enrique Iglesias\\",
            //            "Diverse17\\George Michel\\George Michael - Fast love.mp3",
            "Diverse18\\John Lee Hooker\\",
            //            "Diverse18\\Julio Iglesias\\Julio Iglesias - Momentos.mp3",
            //            "Diverse18\\Julio Iglesias\\Julio Iglesias - My Love.mp3",
            //            "Diverse18\\Julio Iglesias\\Julio Iglesias - When I Need You.mp3",
            //            "Diverse19\\Nana\\NANA___I_REMEMBER_THE_TIME.MP3",
            //            "Diverse1\\Depeche Mode - It's no good.mp3",
            //            "Diverse1\\Michael Jackson - Remember The Time.mp3",
            //            "Diverse20\\Santana\\Santana - Love is you.mp3",
            //            "Diverse21\\The Kelly Family\\THE_KELLY_FAMILY___ROSES__O.MP3",
            //            "Diverse21\\UB-40\\",
            //            "Diverse23\\Elvis Presley\\",
            //            "Diverse23\\Harry Belafonte\\",
            //            "Diverse25\\Loreena McKennitt\\",
            //            "Diverse26\\Santana & Eric\\",
            "Diverse3\\GEORGE MICHAEL-Jesus To A Child.mp3",
            //            "Diverse4\\Queen\\",
            //            "Diverse4\\Vama Veche\\",
            "Diverse6\\Gherghe Zamfir\\",
            //            "Diverse6\\Iris\\Iris - Baby.mp3",
            //            "Diverse7\\Bitech\\Christmas - Frank Sinatra.mp3",
            //            "Diverse7\\Bitech\\Frank Sinatra - Stranger In The Night.MP3",
            //            "Diverse7\\Bitech\\Goo Goo Dols - Iris.mp3",
            "Diverse7\\Jon Bon Jovi\\",
            //            "Diverse7\\Julio Iglesias - Moralito.mp3",
            "Diverse7\\Patricia Kass\\Patricia Kass - Entre Dans La Lumiere.MP3",
            "Diverse7\\Patricia Kass\\Patricia Kass - LA_LIBERTE.MP3",
            //            "Diverse8\\Loreena MC Kennitt\\",
            "Diverse8\\Nat King Cole\\",
            //            "Diverse8\\Pet Shop Boys - Go West.mp3",
            "Diverse9\\Scorpions\\Scorpions - you and i.mp3",
            //            "Eugen\\06-ayo-watching_you.mp3",
            //            "Eugen\\07-ayo-only_you.mp3",
            //            "Geo\\Coldplay",
            //            "Geo\\Guns'n Roses - One In A Million",
            "Muzica Romaneasca 16 CDs\\",
            //            "NewMusic\\Alphaville\\For A Million",
            //            "NewMusic\\Ambientala\\",
            //            "NewMusic\\Colinde Stefan Hrusca\\Stefan_Hrusca_-_Colinde_-_13.mp3",
            //            "NewMusic\\Morcheeba\\Morcheeba - Big Calm - 1998\\07 - Over And Over.mp3",
            "NewMusic\\Tina Dickow - Count to ten\\",
            "NewMusic\\Within Temptation\\",
            //            "Oldies But Goodies Vol.1-15\\Bobby Day - Over And Over.mp3",
            //            "Others\\George Michael - Last Christmas.mp3",
            //            "Others\\Mariah Carey - All I want for Christmas is you.mp3",
            "Others\\Vaya Con Dios - Mothers And Daughters.mp3",
            "Savage Garden - To The Moon And Back (alien).mp3",
            "Savage Garden - To The Moon And Back.mp3",
            //            "The Ultimate Oldies but Goodies Collection (Time-Life)\\Bobby Vee - Take Good Care Of My Baby.mp3",
            //            "The Ultimate Oldies but Goodies Collection (Time-Life)\\Bruce Channel - Hey! Baby.mp3",
            //            "The Ultimate Oldies but Goodies Collection (Time-Life)\\Jackie Wilson - That's Why (I Love You So).mp3",
            "Top 500 Rock And Roll Songs\\",
            "Vangelis\\"

            //            "Diverse5\\Pasarea Colibri\\",
            //            "Car032008\\Iron Maiden - Strange World.mp3",
            //            "Car032008\\Lords of the Realm",
            //            "Diverse10\\Apocaliptica",
            //            "Diverse16\\Enrique Iglesias\\Enrique Iglesias - Hero (english",
            //            "Diverse16\\Enrique Iglesias\\Enrique Iglesias - Hero (spanish",
            //            "Diverse16\\Enrique Iglesias\\Enrique Iglesias - I Can Be Your Hero",
            //            "Diverse17\\Gipsy Kings\\Gipsy Kings - Trista Pena",
            //            "Diverse17\\Gipsy Kings\\Gipsy Kings - Volare",
            //            "Diverse18\\Kelly Family\\Kelly Family - Sometimes.mp3",
            //            "Diverse21\\Suzanne Vega\\Suzanne Vega - Tom's Dinner.mp3",
            //            "Diverse25\\Kathy Garby - Eleftheria",
            //            "Diverse4\\Mylene Farmer",
            //            "Diverse4\\Pasarea Colibri\\",
            //            "Diverse5\\Phoenix - Cei ce ne-au dat nume\\",
    };

    public static void main(String[] args) throws Exception {
        System.out.println("BEGIN main");
        //        BooleanQuery.setMaxClauseCount(M3uPlaylistSong.MAX_ABSOLUTE_SIZE_DEVIATION * 2 + 1);

        //        MusicIndex.INDEX_DIR = new File("..\\MusicIndex\\MusicIndex home 2011-02-27");
        //        MusicIndex.INDEX_DIR = new File("..\\MusicIndex\\MusicIndex job " + ymd);
        //        MusicIndex.INDEX_DIR = new File("..\\MusicIndex\\MusicIndex home " + ymd);
        //        MusicIndex.INDEX_DIR = new File("..\\MusicIndex\\MusicIndex home 2012-05-10");
        MusicIndex.INDEX_DIR = new File(MUSIC_TEMP_DIR + "MusicIndex home " + ymd);
        //        MusicIndex.INDEX_DIR = new File(CURRENT_ARCHIVE_DIR + "MusicIndex home 2011-03-07");

        //        saveFullPathInPlayList(MyPlaylistsPath + "2010-03-04 selected teac.m3u8",
        //                               getFullPathPlayListNameFor(MyPlaylistsPath +
        //                                       "2010-03-04 selected teac.m3u8"), true);
        //        saveFullPathInPlayList(M3UFile, getFullPathPlayListNameFor(M3UFile), true);
        //        saveFullPathInPlayList("F:\\MUZICA\\2011-03-02 all pmp.m3u8",
        //                               getFullPathPlayListNameFor("F:\\MUZICA\\2011-03-02 all pmp.m3u8"), true);
        //        saveFullPathInPlayList("F:\\MUZICA\\2011-03-05 home f-muzica.m3u8",
        //                               getFullPathPlayListNameFor("F:\\MUZICA\\2011-03-05 home f-muzica.m3u8"), true);
        //        saveFullPathInPlayList("F:\\MUZICA\\2012-05-10 home all.m3u8",
        //                               getFullPathPlayListNameFor("F:\\MUZICA\\2012-05-10 home all.m3u8"), true);

        // INDEXARE
        /*M3uPlaylist m3uPlaylist = new M3uPlaylist(M3UFile, true);
        m3uPlaylist.setDontThrowErrForMissingFilesWhenComputingSize(false);
        m3uPlaylist.setDontComputeFileSizeIfExistsSeconds(false);
        m3uPlaylist.setDontReadTags(false);
        m3uPlaylist.index(true);*/

        //        showDiffOrchestra(m3uPlaylist, true);

        //        convertPlayList("D:\\MUZICA\\2011-03-01 JOB d-muzica.m3u8", true, null, true, 0.1f, false);
        //        convertPlayList("F:\\MUZICA\\cd-14-02-2009.m3u", true, null, true, 5f, false);
        //        convertPlayList("F:\\MUZICA\\Sony MP3 playlists\\2012-01-12\\" +
        //                            "Understanding the Universe Introduction to Astronomy - Alex Filippenko.m3u",
        //                        false, null, true, 5f, false);
        //        convertPlayList("F:\\MUZICA\\Sony MP3 playlists\\2012-01-12\\STALKER.m3u",
        //                        false, null, true, 5f, false);
        //        saveNewSongsList("D:\\MUZICA\\2011-02-09 job NewMuzic selection.m3u8", false);

        //        convertPlayLists(
        //                "f:\\MUZICA\\Temp\\Archives\\M3uFilesToPath home 2011-03-05\\My Playlists home 2011-03-05\\",
        //                1f, false, false, false);
        //        convertPlayList(MyPlaylistsPath + "2010-03-04 selected teac.m3u", true, null, true, 5f, true);
        //        convertPlayList(MyPlaylistsPath + "New Music Home.m3u", true, null, false, 1f, false);
        //        convertPlayList(MyPlaylistsPath + "sport+uti - test.m3u8", true, null, false, 1f, false);
        convertPlayList(MyPlaylistsPath + "sport+uti.m3u8", true, null, false, 1f, false);

        //        extractDuplicatesFromPlayList("d:\\Projects\\M3uFilesToPath\\MusicIndex\\2011-02-09 HOME f-muzica.m3u8", 10f);
        //        extractDuplicatesFromPlayList(getFullPathPlayListNameFor(M3UFile), 40f);

        //        M3uPlaylist m3uPlaylist = new M3uPlaylist("d:\\Muzica\\2011-03-01 JOB d-muzica - not found.m3u8");
        //        m3uPlaylist.setDontReadTags(true);
        //        m3uPlaylist.loadPlayList();
        //        moveSongsToPath("d:\\Muzica\\Temp\\", m3uPlaylist);

        //        moveSongsToPath(duplicatesList);

        System.out.println("END main");
    }

    /**
     * Afiseaza diferentele dintre artist (author) si album artist (orchestra).
     * Actualizeaza artist / album artist ce este null cu valoarea celuilalt.
     *
     * @param m3uPlaylist
     * @param updateArtistOrOrchestraIfNull
     */
    public static void showDiffOrchestra(M3uPlaylist m3uPlaylist, boolean updateArtistOrOrchestraIfNull) {
        String author, orchestra;
        int size = m3uPlaylist.getSongs().size();
        for (M3uPlaylistSong song : m3uPlaylist.getSongs()) {
            author = song.getTag("author");
            orchestra = song.getTag("mp3.id3tag.orchestra");
            if (author == null && orchestra == null) {
                continue;
            }
            if (author != null && orchestra == null) {
                System.out.println("\n" + m3uPlaylist.getSongs().indexOf(song) + " / " + size);
                System.out.println("author: " + author);
                System.out.println("orchestra: " + orchestra);
                System.out.println(song);
                if (updateArtistOrOrchestraIfNull) {
                    song.putTag("mp3.id3tag.orchestra", author, true);
                    song.updateAlbumArtist(false);
                }
            } else if (author == null) {
                System.out.println("\n" + m3uPlaylist.getSongs().indexOf(song) + " / " + size);
                System.out.println("author: " + author);
                System.out.println("orchestra: " + orchestra);
                System.out.println(song);
                if (updateArtistOrOrchestraIfNull) {
                    song.putTag("author", orchestra, true);
                    song.updateArtist(false);
                }
            } else if (author.indexOf(orchestra) < 0) {
                if (author.toLowerCase().indexOf(orchestra.toLowerCase()) >= 0) {
                    // only character-case is diff
                    continue;
                }
                System.out.println("\n" + m3uPlaylist.getSongs().indexOf(song) + " / " + size);
                System.out.println("author: " + author);
                System.out.println("orchestra: " + orchestra);
                System.out.println(song);
            }
        }
    }

    public static void extractDuplicatesFromPlayList(String m3uFilePath,
            float maxScoreDeviationPercentAgainstMatch) {
        M3uPlaylist m3uPlaylist = new M3uPlaylist(m3uFilePath);
        m3uPlaylist.setDontComputeFileSize(false);// ca sa aleaga spre pastrare fis cel mai mare
        m3uPlaylist.setDontReadTags(false);
        m3uPlaylist.loadPlayList();
        if (!m3uPlaylist.getSongs().get(0).getMp3FilePath().equals(
                m3uPlaylist.getSongs().get(0).getMp3FilePathFull())) {
            System.out.println(m3uFilePath + " nu contine full path ci cai relative !");
            return;
        }

        String duplicatesBasePath = new File(m3uFilePath).getParent();
        boolean dontReturnForOverMaxScoreDeviation = true;
        List<M3uPlaylist> duplicatesPlayLists = m3uPlaylist.getDuplicates(duplicatesBasePath, false, false,
                maxScoreDeviationPercentAgainstMatch,
                dontReturnForOverMaxScoreDeviation);
        System.out.println("\nmaxScoreDev%:\t" + maxScoreDeviationPercentAgainstMatch);
        System.out.println("dontReturnForOverMaxScoreDeviation:\t" + dontReturnForOverMaxScoreDeviation);
        System.out.println("files analyzed:\t" + m3uPlaylist.getSongs().size());
        System.out.println("duplicates:\t\t" + duplicatesPlayLists.size() + '\n');

        M3uPlaylist m3uPlaylistWithDupSongs = computeDuplicatesList(duplicatesPlayLists);
        M3uPlaylist m3uPlaylistWithOriginalSongs = computeOriginalSongs(m3uPlaylistWithDupSongs,
                duplicatesPlayLists);

        //        int sameSecondsAndWords = 0;
        M3uPlaylistSong searchedSong;
        boolean allSongsAreExceptedFromRemoval;
        for (M3uPlaylist m3uPlaylistDup : duplicatesPlayLists) {
            //            m3uPlaylistDup.compactSongsBySecondsAndWords();
            //            if (m3uPlaylistDup.getSongs().size() == 1) {
            //                sameSecondsAndWords++;
            //                continue;
            //            }

            searchedSong = (M3uPlaylistSong) m3uPlaylistDup.getSongs().get(0).getMisc("searchedSong");

            allSongsAreExceptedFromRemoval = true;
            for (M3uPlaylistSong song : m3uPlaylistDup.getSongs()) {
                if (!isExceptionFromRemoval(song)) {
                    allSongsAreExceptedFromRemoval = false;
                    break;
                }
            }
            if (allSongsAreExceptedFromRemoval && isExceptionFromRemoval(searchedSong)) {
                // nu are sens afisarea lui m3uPlaylistDup pt ca toate
                // melodiile sale + searchedSong sunt exceptate de la stergere
                continue;
            }

            System.out.println(searchedSong.toString(false, false, false,
                    new String[] { "score" }));
            System.out.println("query:\t" + m3uPlaylistDup.getSongs().get(0).getMisc("query"));
            //            System.out.println(m3uPlaylistDup.toString(false, false, false, null));
            System.out.println(m3uPlaylistDup.toString(false, false, false,
                    new String[] { "scoreDeviationPercentAgainstMatch", "score",
                            "keep" }));
            System.out.println("----------------------------------------------------------");
        }
        //        System.out.println("sameSecondsAndWords: " + sameSecondsAndWords);

        System.out.println('\n' + m3uPlaylistWithOriginalSongs.getM3uFilePath() + ":\n");
        System.out.println(m3uPlaylistWithOriginalSongs);
        m3uPlaylistWithOriginalSongs.write();

        System.out.println('\n' + m3uPlaylistWithDupSongs.getM3uFilePath() + ":\n");
        System.out.println(m3uPlaylistWithDupSongs);
        m3uPlaylistWithDupSongs.write();

        /*
        System.out.println("compactSongsBySecondsAndWords:");
        List<M3uPlaylistSong> duplicatesList;
        boolean hasDuplicatesList;
        for (M3uPlaylist m3uPlaylistDup : duplicatesPlayLists) {
            hasDuplicatesList = false;
            for (M3uPlaylistSong song : m3uPlaylistDup.getSongs()) {
                duplicatesList = (List) song.getMisc().get("duplicatesList");
                if (duplicatesList == null) {
                    // dupa compactare song nu pare a fi similar cu nici unul din duplicatele gasite in m3uPlaylistDup
                    continue;
                }
                System.out.println("");
                // dupa compactare song pare a fi similar cu duplicatesList (alese dintre m3uPlaylistDup)
                for (M3uPlaylistSong duplicatedSong : duplicatesList) {
                    assert !song.getMp3FilePathFull().toLowerCase().equals(
                            duplicatedSong.getMp3FilePathFull().toLowerCase());
                    System.out.println(duplicatedSong.getMp3FilePathFull());
                }
                hasDuplicatesList = true;
            }
            if (hasDuplicatesList) {
                System.out.print("----------------------------------------------------------");
            }
        }
        */
    }

    private static M3uPlaylist computeOriginalSongs(final M3uPlaylist m3uPlaylistWithDupSongs,
            List<M3uPlaylist> duplicatesPlayLists) {
        M3uPlaylist m3uPlaylistWithOriginalSongs = new M3uPlaylist(originalList, true);
        M3uPlaylistSong searchedSong;
        for (M3uPlaylist m3uPlaylistDup : duplicatesPlayLists) {
            m3uPlaylistWithOriginalSongs.addSongs(m3uPlaylistDup.getSongs(), new SongFilter() {
                public boolean accept(M3uPlaylistSong song) {
                    return !m3uPlaylistWithDupSongs.getSongs().contains(song);
                }
            });
            searchedSong = (M3uPlaylistSong) m3uPlaylistDup.getSongs().get(0).getMisc("searchedSong");
            if (m3uPlaylistWithDupSongs.getSongs().contains(searchedSong)) {
                continue;
            }
            if (!m3uPlaylistWithOriginalSongs.getSongs().contains(searchedSong)) {
                m3uPlaylistWithOriginalSongs.addSong(searchedSong);
            }
        }
        return m3uPlaylistWithOriginalSongs;
    }

    private static M3uPlaylist computeDuplicatesList(List<M3uPlaylist> duplicatesPlayLists) {
        M3uPlaylist m3uPlaylistWithDupSongs = new M3uPlaylist(duplicatesList, true);
        M3uPlaylistSong searchedSong;
        M3uPlaylistSong songToKeep;
        boolean existsExcep;
        for (M3uPlaylist m3uPlaylistDup : duplicatesPlayLists) {
            searchedSong = (M3uPlaylistSong) m3uPlaylistDup.getSongs().get(0).getMisc("searchedSong");
            existsExcep = existsExceptionForRemoval(searchedSong, m3uPlaylistDup.getSongs());

            if (existsExcep) {
                // printre duplicate se afla exceptii de la
                // stergere asa ca doar acestea se vor pastra
                songToKeep = null;
            } else {
                // se va pastra doar cea mai buna varianta dintre duplicate
                songToKeep = findDuplicateToKeep(searchedSong, m3uPlaylistDup.getSongs());
            }

            addDuplicatesForRemoval(searchedSong, songToKeep, m3uPlaylistDup, m3uPlaylistWithDupSongs);
        }
        return m3uPlaylistWithDupSongs;
    }

    private static boolean isExceptionFromRemoval(M3uPlaylistSong song) {
        String path = song.getMp3FilePath().toLowerCase();
        for (String exc : TO_DELETE_EXCEPTED_SONG) {
            if (path.indexOf(exc.toLowerCase()) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static void addDuplicatesForRemoval(M3uPlaylistSong searchedSong,
            final M3uPlaylistSong songToKeep,
            M3uPlaylist m3uPlaylistDup,
            M3uPlaylist m3uPlaylistWithDupSongs) {
        int initSize = m3uPlaylistWithDupSongs.getSongs().size();
        if (searchedSong != songToKeep && !isExceptionFromRemoval(searchedSong)) {
            m3uPlaylistWithDupSongs.addSong(searchedSong);
        }
        m3uPlaylistWithDupSongs.addSongs(m3uPlaylistDup.getSongs(), new SongFilter() {
            public boolean accept(M3uPlaylistSong song) {
                return !isExceptionFromRemoval(song) && (songToKeep == null || song != songToKeep);
            }
        });
        assert TO_DELETE_EXCEPTED_SONG.length != 0 || m3uPlaylistWithDupSongs.getSongs()
                .size() == (initSize + m3uPlaylistDup.getSongs().size());
    }

    private static boolean existsExceptionForRemoval(M3uPlaylistSong searchedSong,
            List<M3uPlaylistSong> songs) {
        List<M3uPlaylistSong> allSongs = new ArrayList<M3uPlaylistSong>(songs.size() + 1);
        allSongs.add(searchedSong);
        allSongs.addAll(songs);
        for (M3uPlaylistSong song : allSongs) {
            if (isExceptionFromRemoval(song)) {
                return true;
            }
        }
        return false;
    }

    private static M3uPlaylistSong findDuplicateToKeep(M3uPlaylistSong searchedSong,
            List<M3uPlaylistSong> songs) {
        //        int maxUniqueWordsCount = -1;
        M3uPlaylistSong songToKeep = null;
        //        int uniqueWordsSize;
        List<M3uPlaylistSong> allSongs = new ArrayList<M3uPlaylistSong>(songs.size() + 1);
        allSongs.add(searchedSong);
        allSongs.addAll(songs);
        float songScore, songToKeepScore;
        for (M3uPlaylistSong song : allSongs) {
            //            uniqueWordsSize = song.getUniqueWords().size();
            if (songToKeep == null) {
                songToKeep = song;
                //                maxUniqueWordsCount = uniqueWordsSize;
            }
            songScore = Float.parseFloat((String) song.getMisc("score"));
            songToKeepScore = Float.parseFloat((String) songToKeep.getMisc("score"));
            if (songScore == songToKeepScore) {
                if (countFileParents(song.getMp3FilePath()) ==
                        countFileParents(songToKeep.getMp3FilePath())) {
                    if (song.getUniqueWords().size() > songToKeep.getUniqueWords().size()) {
                        songToKeep = song;
                    }
                } else if (countFileParents(song.getMp3FilePath()) >
                        countFileParents(songToKeep.getMp3FilePath())) {
                    songToKeep = song;
                }
            } else if (songScore > songToKeepScore) {
                songToKeep = song;
            }
        }
        return songToKeep;
    }

    private static int countFileParents(String filePath) {
        int i = 0;
        File file = new File(filePath);
        while ((file = file.getParentFile()) != null) {
            i++;
        }
        return i;
    }

    public static void convertPlayLists(String basePath, float maxScoreDeviationPercentAgainstMatch,
            boolean foundSongCanMiss, boolean generateConvertedM3u) {
        String[] m3uFiles = (new File(basePath)).list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.indexOf(".m3u") > 0 && name.indexOf("pmp") < 0 &&
                        !MusicIndexSearcher.isM3uGenerated(name);
            }
        });
        for (String m3uFilePath : m3uFiles) {
            convertPlayList(basePath + m3uFilePath, generateConvertedM3u, null,
                    foundSongCanMiss, maxScoreDeviationPercentAgainstMatch, true);
        }
    }

    /**
     * Creaza si eventual salveaza o lista cu melodiile din m3uFilePath ce NU se afla in index.
     *
     * @param m3uFilePath
     * @param writeNewM3u
     */
    public static void saveNewSongsList(String m3uFilePath, boolean writeNewM3u) {
        M3uPlaylist foundSongs = convertPlayList(m3uFilePath, false, null, true, 5f, true);

        M3uPlaylist newSongs = new M3uPlaylist(foundSongs.getM3uFilePath(), true);
        newSongs.addSongs(foundSongs.getSongs(), new SongFilter() {
            public boolean accept(M3uPlaylistSong song) {
                return song.getMisc().get("keep") == null;
            }
        });

        M3uPlaylist searchedList = newSongs.getSongs().get(0).getM3uPlaylist();

        boolean found;
        for (M3uPlaylistSong searchedSong : searchedList.getSongs()) {
            found = false;
            for (M3uPlaylistSong foundSong : foundSongs.getSongs()) {
                if (searchedSong == foundSong.getMisc().get("searchedSong")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                newSongs.addSong(searchedSong);
            }
        }

        System.out.println(newSongs);

        if (writeNewM3u) {
            newSongs.write();
        }
    }

    public static M3uPlaylist convertPlayList(String m3uFilePath, boolean writeNewM3u,
            SongFilter foundSongFilter, boolean foundSongCanMiss,
            float maxScoreDeviationPercentAgainstMatch,
            boolean dontReturnForOverMaxScoreDeviation) {
        M3uPlaylist m3uPlaylistEXTERN = new M3uPlaylist(m3uFilePath);
        // at trebui true pt ca o cale externa e f.posibil sa nu corespunda HDD local
        m3uPlaylistEXTERN.setDontReadTags(true);
        // cel mai probabil se converteste o lista externa la una careia
        // sa-i corespunda caile melodiilor cu cele de pe HDD local; in
        // acest caz calcularea marimii fisierului pe baza caii externe NU va reusi
        m3uPlaylistEXTERN.setDontComputeFileSize(true);
        m3uPlaylistEXTERN.setDontComputeFileSizeIfExistsSeconds(true);
        m3uPlaylistEXTERN.loadPlayList();
        return convertPlayList(m3uPlaylistEXTERN, writeNewM3u, foundSongFilter,
                foundSongCanMiss, maxScoreDeviationPercentAgainstMatch,
                dontReturnForOverMaxScoreDeviation);
    }

    /**
     * Cauta toate melodiile din m3uFilePath in INDEX_DIR si returneaza un nou playlist
     * cu melodiile gasite. Afiseaza un raport privind ce s-a gasit si ce nu.
     *
     * @param m3uPlaylist
     * @param writeNewM3u
     * @param foundSongFilter
     * @param foundSongCanMiss
     * @param maxScoreDeviationPercentAgainstMatch
     * @param dontReturnForOverMaxScoreDeviation
     * @return
     */
    public static M3uPlaylist convertPlayList(M3uPlaylist m3uPlaylist, boolean writeNewM3u,
            SongFilter foundSongFilter, boolean foundSongCanMiss,
            float maxScoreDeviationPercentAgainstMatch,
            boolean dontReturnForOverMaxScoreDeviation) {
        System.out.println(m3uPlaylist.getM3uFilePath() +
                " size = " + m3uPlaylist.getSongs().size());
        System.out.println("\nmaxScoreDev%:\t" + maxScoreDeviationPercentAgainstMatch);
        System.out.println("dontReturnForOverMaxScoreDeviation:\t" + dontReturnForOverMaxScoreDeviation);

        MusicIndexSearcher musicIndexSearcher = new MusicIndexSearcher(true);
        musicIndexSearcher.setThrowErrorOnPerfectMatchNotFound(false);
        musicIndexSearcher.setFoundSongCanMiss(foundSongCanMiss);
        musicIndexSearcher.setMaxScoreDeviationPercentAgainstMatch(maxScoreDeviationPercentAgainstMatch);
        musicIndexSearcher.setDontReturnForOverMaxScoreDeviation(dontReturnForOverMaxScoreDeviation);

        M3uPlaylist m3uPlaylistCONVERTED = musicIndexSearcher.searchSongs(m3uPlaylist);

        String report = m3uPlaylistCONVERTED.compareToOriginal(m3uPlaylist,
                WARN_ABOUT_DIFF_FOLDERS,
                foundSongFilter);
        System.out.println(report);

        final HashMap<String, Integer> writtenSizeMap = new HashMap<String, Integer>();
        writtenSizeMap.put("writtenSize", 0);

        System.out.println(m3uPlaylistCONVERTED.getM3uFilePath() +
                " size = " + m3uPlaylistCONVERTED.getSongs().size());

        final M3uPlaylist m3uPlaylistNotConverted = new M3uPlaylist(
                Util.suffixPath(m3uPlaylist, " - not found"), true);
        if (writeNewM3u) {
            final List<M3uPlaylistSong> searchedAndFound = new ArrayList<>();

            m3uPlaylistCONVERTED.write(song -> {
                M3uPlaylistSong searchedSong = (M3uPlaylistSong) song.getMisc("searchedSong");
                searchedAndFound.add(searchedSong);
                if (song.getMisc("keep") != null) {
                    int count = writtenSizeMap.get("writtenSize") + 1;
                    writtenSizeMap.put("writtenSize", count);
                    return true;
                }
                return false;
            });

            for (M3uPlaylistSong song : m3uPlaylist.getSongs()) {
                if (!searchedAndFound.contains(song)) {
                    m3uPlaylistNotConverted.addSong(song);
                }
            }

            if (!m3uPlaylistNotConverted.getSongs().isEmpty()) {
                m3uPlaylistNotConverted.write();
            }
        }
        System.out.println(m3uPlaylistCONVERTED.getM3uFilePath() +
                " written.size = " + writtenSizeMap.get("writtenSize"));
        System.out.println(m3uPlaylistNotConverted.getM3uFilePath() +
                " written.size = " + m3uPlaylistNotConverted.getSongs().size());

        return m3uPlaylistCONVERTED;
    }

    public static void saveFullPathInPlayList(String m3uFilePath, String m3uFileFullPath, boolean doWrite) {
        M3uPlaylist m3uPlaylistEXTERN = new M3uPlaylist(m3uFilePath);
        m3uPlaylistEXTERN.setDontThrowErrForMissingFilesWhenComputingSize(true);
        m3uPlaylistEXTERN.loadPlayList();
        //        System.out.println(m3uPlaylistEXTERN.toString());
        if (doWrite) {
            m3uPlaylistEXTERN.setM3uFilePath(m3uFileFullPath);
            m3uPlaylistEXTERN.setWriteFullMp3PathToM3u(true);
            m3uPlaylistEXTERN.write(new SongFilter() {
                public boolean accept(M3uPlaylistSong song) {
                    return song.exists();
                }
            });
        }
    }

    private static String getFullPathPlayListNameFor(String m3uFilePath) {
        return m3uFilePath.substring(0, m3uFilePath.indexOf(".m3u")) + " - full paths.m3u8";
    }

    public static void moveSongsToPath(String newBasePath, M3uPlaylist m3uPlaylist) {
        if ((new File(newBasePath)).isFile()) {
            newBasePath = (new File(newBasePath)).getParent();
        }
        String basePath = computeBasePath(m3uPlaylist);
        String songFullPath, newSongFullPath;
        if (newBasePath.charAt(newBasePath.length() - 1) == File.separatorChar) {
            newBasePath = newBasePath.substring(0, newBasePath.length() - 1);
        }
        for (M3uPlaylistSong song : m3uPlaylist.getSongs()) {
            if (!song.exists()) {
                continue;
            }
            songFullPath = song.getMp3FilePathFull();
            newSongFullPath = newBasePath + File.separatorChar + songFullPath.substring(basePath.length());
            (new File(newSongFullPath)).getParentFile().mkdirs();
            if (!(new File(songFullPath)).renameTo(new File(newSongFullPath))) {
                System.out.println("\nNu s-a putut redenumi:\n" + songFullPath + "\ncu\n" + newSongFullPath);
            }
        }
    }

    private static String computeBasePath(M3uPlaylist m3uPlaylist) {
        String basePath = null, songFullPath;
        for (M3uPlaylistSong song : m3uPlaylist.getSongs()) {
            songFullPath = new File(song.getMp3FilePathFull()).
                    getParentFile().getAbsolutePath().toLowerCase();
            if (basePath == null) {
                basePath = songFullPath;
            } else {
                basePath = intersectString(basePath, songFullPath);
            }
        }
        return basePath;
    }

    private static String intersectString(String s1, String s2) {
        char[] charArray1 = s1.toCharArray();
        char[] charArray2 = s2.toCharArray();
        for (int i = 0; i < charArray1.length; i++) {
            if (charArray1[i] != charArray2[i]) {
                return s1.substring(0, i);
			}
		}
		return s1;
	}
}
