package M3uFilesToPath.m3u;

import M3uFilesToPath.lucene.MusicIndex;
import M3uFilesToPath.lucene.MusicIndexSearcher;
import org.farng.mp3.TagConstant;
import org.farng.mp3.TagOptionSingleton;
import org.farng.mp3.id3.AbstractID3v2;
import org.farng.mp3.id3.AbstractID3v2Frame;
import org.farng.mp3.id3.FrameBodyTPE2;
import org.farng.mp3.id3.ID3v1;
import org.farng.mp3.object.ObjectNumberHashMap;
import org.farng.mp3.object.ObjectStringSizeTerminated;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static M3uFilesToPath.lucene.MusicIndex.ymd;
import static M3uFilesToPath.m3u.Main.CURRENT_ARCHIVE_DIR;

/**
 * Created by IntelliJ IDEA.
 * User: adr
 * Date: Mar 1, 2011
 * Time: 4:08:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class M3uTests {
    public static void main(String[] args) throws Exception {
        System.out.println("BEGIN main");
//        MusicIndex.INDEX_DIR = new File("..\\MusicIndex\\MusicIndex job " + ymd);
//        MusicIndex.INDEX_DIR = new File("..\\MusicIndex\\MusicIndex home " + ymd);
        MusicIndex.INDEX_DIR = new File("..\\MusicIndex\\MusicIndex home 2011-03-05");
//        MusicIndex.INDEX_DIR = new File(ARCHIVE_DIR +
//                "M3uFilesToPath home 2011-03-05\\MusicIndex home 2011-03-05");
//        MusicIndex.INDEX_DIR = new File(CURRENT_ARCHIVE_DIR + "MusicIndex home 2011-03-05");
//        MusicIndex.INDEX_DIR = new File(CURRENT_ARCHIVE_DIR + "MusicIndex home 2011-03-07");

//        M3uPlaylist m3uPlaylist = loadNonPMPWinampDevicePlayList("G:\\Temp\\Playlists\\existing on sony player 2011-02-13.m3u");
//        M3uPlaylist m3uPlaylist = loadNonPMPWinampDevicePlayList(
//                "F:\\MUZICA\\Temp\\Archives\\M3uFilesToPath home 2011-03-02\\2011-02-13 all songs & pmp - sony.m3u");
//        M3uPlaylist m3uPlaylistCONVERTED = convertPlayList(m3uPlaylist, true, null, false, true, 1f, true);

//        M3uPlaylist m3uPlaylist = loadNonPMPWinampDevicePlayList(
//                "f:\\MUZICA\\Temp\\Archives\\M3uFilesToPath home 2011-03-02\\2011-02-13 all songs & pmp - sony.m3u");

        showDeviceSurplus(CURRENT_ARCHIVE_DIR + "device 2011-02-13 all files.m3u",
                          CURRENT_ARCHIVE_DIR + "device 2011-03-02 all songs.m3u");

//        M3uPlaylist m3uPlaylist = searchUsingNonPMPWinampDevicePlayList(CURRENT_ARCHIVE_DIR + "device 2011-02-13 all files.m3u");
//        System.out.println(m3uPlaylist);

//        MusicIndex.reindexSong("F:\\MUZICA\\Others\\CC CATCH  - TEARS WON'T WASH AWAY MY HEARTACHE.mp3");

//        String basePath = "f:\\MUZICA\\Temp\\Archives\\M3uFilesToPath home 2011-03-02\\";
//        MusicIndex.INDEX_DIR = new File(basePath + "Copy of MusicIndex home 2011-03-02");
//        MusicIndex.miscUpdateM3uPlayListIndex();

//        jid3blinkenlights();
//        jid3();

//        song();

        System.out.println("END main");
    }

    private static void showDeviceSurplus(String allDeviceFilesPlayList, String allSongsPlayListOnDevice) {
        M3uPlaylist devicePlaylistAllNoPMP = loadNonPMPWinampDevicePlayList(allDeviceFilesPlayList);

        M3uPlaylist devicePlaylistAllSelected = new M3uPlaylist(allSongsPlayListOnDevice);
        devicePlaylistAllSelected.loadWinampDevicePlayList(null);

        M3uPlaylist diff = devicePlaylistAllNoPMP.substract(devicePlaylistAllSelected);
        Collections.sort(diff.getSongs());

        System.out.println(diff);
        System.out.println("");

        Set<String> titles = new TreeSet<String>();
        for (M3uPlaylistSong song : diff.getSongs()) {
            titles.add(song.getTag("title"));
        }
        for (String title : titles) {
            System.out.println(title);
        }
    }

    /**
     * http://javamusictag.sourceforge.net/index.html
     *
     * @throws Exception
     */
    private static void jid3() throws Exception {
        System.out.println("BEGIN jid3");
//        String filePath = "F:\\MUZICA\\NewMusic\\08-07-18_113941.mp3";
//        String filePath = "d:\\Temp\\Track01 - Copy.mp3";
        String filePath = "D:\\MUZICA\\NewMusic\\Tears for Fears\\16 Change.wma";

        M3uPlaylistSong song = new M3uPlaylistSong(filePath);
        song.fillWithTags();

        org.farng.mp3.MP3File mp3File = new org.farng.mp3.MP3File(filePath);
        ID3v1 id3v1 = mp3File.getID3v1Tag();
        AbstractID3v2 id3v2 = mp3File.getID3v2Tag();
//        AbstractLyrics3 lyrics3 = mp3File.getLyrics3Tag();

        // orchestra varianta 1
        AbstractID3v2Frame v2Frame = id3v2.getFrame("TPE2");
        FrameBodyTPE2 fragmentBody = (FrameBodyTPE2) v2Frame.getBody();

        Iterator it1 = fragmentBody.iterator();
        ObjectNumberHashMap objectNumberHashMap = (ObjectNumberHashMap) it1.next();
        String encoding = (String) objectNumberHashMap.getIdToString()
                .get((Long) objectNumberHashMap.getValue());
        ObjectStringSizeTerminated objectStringHashMap = (ObjectStringSizeTerminated) it1.next();
        String orchestra = new String(objectStringHashMap.writeByteArray(), encoding);
        System.out.println("Band/orchestra/accompaniment: " + orchestra);

        objectStringHashMap.readByteArray((id3v1.getArtist() + "123abc").getBytes(encoding));
        TagOptionSingleton.getInstance().setDefaultSaveMode(TagConstant.MP3_FILE_SAVE_OVERWRITE);
        mp3File.save();
        TagOptionSingleton.getInstance().setDefaultSaveMode(TagConstant.MP3_FILE_SAVE_WRITE);

//        String encoding = (String) TagConstant.textEncodingIdToString.get(new Long(fragmentBody.getTextEncoding()));
//        String orchestra = new String(fragmentBody.getText().getBytes(), encoding);

        // orchestra varianta 2
//        RandomAccessFile sourceFile = null;
//        try {
//            sourceFile = new RandomAccessFile(filePath, "r");
//            FrameBodyTPE2 bodyTPE2 = new FrameBodyTPE2(sourceFile);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (sourceFile != null) {
//                sourceFile.close();
//            }
//        }

//        try {
//            sourceFile = new RandomAccessFile(filePath, "r");

//            ID3v1_1 id3v1_1 = new ID3v1_1(sourceFile);
//            ID3v1 id3v1 = new ID3v1(sourceFile);
//            ID3v2_3 id3v2_3 = new ID3v2_3(sourceFile);

//            ID3v2_2 id3v2_2 = new ID3v2_2(sourceFile);
//            ID3v2_4 id3v2_4 = new ID3v2_4(sourceFile);
//            Lyrics3v2 lyrics3v2 = new Lyrics3v2(sourceFile);
//            Lyrics3v1 lyrics3v1 = new Lyrics3v1(sourceFile);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (sourceFile != null) {
//                sourceFile.close();
//            }
//        }

//        FilenameTag filenameTag = FilenameTagBuilder.createFilenameTagFromMP3File(mp3File);
//        filenameTag.getAuthorComposer();

        System.out.println("END jid3");
    }

    private static M3uPlaylist searchUsingNonPMPWinampDevicePlayList(String existingOnSonyPlayer) {
        M3uPlaylist devicePlayList = loadNonPMPWinampDevicePlayList(existingOnSonyPlayer);
        MusicIndexSearcher searcher = new MusicIndexSearcher(true);
        searcher.setDontSearchBySeconds(true);
        searcher.setDontReturnForOverMaxScoreDeviation(false);
        searcher.setDontReturnForOverMaxSecondsDeviation(false);
        searcher.setFoundSongCanMiss(false);
        searcher.setMaxScoreDeviationPercentAgainstMatch(10f);
        searcher.setMaxScoreDeviationPercentAgainstMatch(0.1f);
        searcher.setResultPerSong(99);
        searcher.setSearchMissingFilesToo(true);
        searcher.setThrowErrorOnPerfectMatchNotFound(true);
        searcher.setFoundSongCanMiss(true);
        return searcher.searchSongs(devicePlayList);
    }

    private static M3uPlaylist loadNonPMPWinampDevicePlayList(String filesFromDevicePlayList) {
        M3uPlaylist m3uPlaylist = new M3uPlaylist(filesFromDevicePlayList);
        m3uPlaylist.loadWinampDevicePlayList(new SongFilter() {
            public boolean accept(M3uPlaylistSong song) {
                String[] avoid = new String[]{"CAMP Hot Topics", "PMP Exam Prep", "PMBOK", "Rita Mulcahy", "Crosswind"};
                for (String void1 : avoid) {
                    if (song.getMp3FilePath().indexOf(void1) >= 0) {
                        return false;
                    }
                }
                return true;
            }
        });
        return m3uPlaylist;
    }

    private static void song() {
//        M3uPlaylistSong song = new M3uPlaylistSong("D:\\MUZICA\\NewMusic\\Outkast_Hey_Ya.mp3");
//        M3uPlaylistSong song = new M3uPlaylistSong("D:\\MUZICA\\NewMusic\\01 - nelly furtado feat. timbaland - promiscuous radio edit.mp3");
//        M3uPlaylistSong song = new M3uPlaylistSong("D:\\MUZICA\\NewMusic\\01 Winter Song.mp3");
//        M3uPlaylistSong song = new M3uPlaylistSong("D:\\MUZICA\\NewMusic\\05 - Morcheeba - Gained The World (Feat. Manda).mp3");
//        M3uPlaylistSong song = new M3uPlaylistSong("D:\\MUZICA\\NewMusic\\CCCatch\\C.C. Catch - Soul Survivor '98 (New Vocal V.mp3");
//        M3uPlaylistSong song = new M3uPlaylistSong(
//                "D:\\MUZICA\\NewMusic\\Chris Norman\\Hits From The Heart\\I Want To Be Needed (With Sha.mp3");
//        M3uPlaylistSong song = new M3uPlaylistSong("D:\\MUZICA\\NewMusic\\Manowar - Warriors Of The World United.MP3");
//        M3uPlaylistSong song = new M3uPlaylistSong("F:\\MUZICA\\0 Muzica Romaneasca 16 CDs\\Disc 10\\142 LUIGI IONESCU - Cum am ajuns sa te iubesc.mp3");
//        M3uPlaylistSong song = new M3uPlaylistSong("F:\\MUZICA\\0 Muzica Romaneasca 16 CDs\\Disc 13\\026 ANASTASIA LAZARIUC - Se-nsera.mp3");
//        M3uPlaylistSong song = new M3uPlaylistSong("F:\\MUZICA\\Temp\\VERDICT - Sa Vina Politia.mp3");
//        M3uPlaylistSong song = new M3uPlaylistSong(
//                "F:\\MUZICA\\Diverse6\\Soundtracks\\2001 Space Odyssey - Theme.mp3");
//        M3uPlaylistSong song = new M3uPlaylistSong("F:\\MUZICA\\Others\\Florin Chilian-Chiar daca.wma");
//        M3uPlaylistSong song = new M3uPlaylistSong("F:\\MUZICA\\Geo\\06 Run dmc - Walk this way.mp3");
        M3uPlaylistSong song = new M3uPlaylistSong(
                "F:\\MUZICA\\Others\\CC CATCH  - TEARS WON'T WASH AWAY MY HEARTACHE.mp3");
        song.fillWithTags();
        System.out.println(song);

//        M3uPlaylistSong song = new M3uPlaylistSong(
//                "F:\\MUZICA\\0 Muzica Romaneasca 16 CDs\\Disc 1\\004 CARMEN RADULESCU - De-as uita iubirea ta.mp3");
//        song.putTag("mp3.id3tag.orchestra", song.getTag("author"), true);
//        song.updateAlbumArtist(false);
    }

//    private static void jid3blinkenlights() throws Exception {
//        String filePath = "F:\\MUZICA\\Others\\CC CATCH  - TEARS WON'T WASH AWAY MY HEARTACHE.mp3";
//        File oSourceFile = new File(filePath);
//        MediaFile oMediaFile = new org.blinkenlights.jid3.MP3File(oSourceFile);
//        ID3Util.printTags(oMediaFile.getTags());
//        ID3V1Tag id3V1Tag = oMediaFile.getID3V1Tag();
//        ID3V2Tag id3V2Tag = oMediaFile.getID3V2Tag();
//        TPE2TextInformationID3V2Frame frame = ((ID3V2_3_0Tag) id3V2Tag).getTPE2TextInformationFrame();
//        String orchestra = frame.getBandOrchestraAccompaniment();
//        System.out.println("Band/Orchestra/Accompaniment: " + orchestra);
////        frame.setBandOrchestraAccompaniment(orchestra + ymd);
////        oMediaFile.sync();
//    }
}
