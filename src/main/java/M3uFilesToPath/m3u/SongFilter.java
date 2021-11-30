package M3uFilesToPath.m3u;

/**
 * Created by IntelliJ IDEA.
 * User: adr
 * Date: Feb 9, 2011
 * Time: 4:10:36 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SongFilter {
    public boolean accept(M3uPlaylistSong song);
}
