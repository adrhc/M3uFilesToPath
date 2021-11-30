package M3uFilesToPath.util;

import M3uFilesToPath.lucene.MusicIndex;
import M3uFilesToPath.m3u.M3uPlaylist;

import java.io.File;
import java.text.Normalizer;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: adr
 * Date: Mar 4, 2011
 * Time: 10:39:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class Util {
    private static final String notTokenizedByLucene = "[\\-,'\\.]";

    public static String concatenate(Collection<String> words, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word);
            if (separator != null) {
                sb.append(separator);
            }
        }
        if (separator != null) {
            return sb.substring(0, sb.length() - separator.length());
        } else {
            return sb.toString();
        }
    }

    public static String removeAccents(String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        return text.replaceAll("[^\\p{ASCII}]", "");
    }

    /**
     * Metoda folosita pt a detecta care dintre cuvinte ar trebui ignorate (folosing appendSplits).
     *
     * @param string
     * @return
     */
    public static List<String> prepareForLucene(String string) {
        // pt a-l tokeniza cat mai in acord cu indexul este necesar sa se aplice removeAccents
        string = Util.removeAccents(string);
        List<String> luceneTokens = MusicIndex.tokenize(string);
        List<String> rez = new ArrayList<String>();
        for (String token : luceneTokens) {
            rez.addAll(Arrays.asList(token.split(notTokenizedByLucene)));
        }
        return rez;
    }

    public static Set<String> getEquals(Collection<String> collection1, Collection<String> collection2) {
        Set<String> commons = new HashSet<String>();
        for (String s : collection1) {
            if (collection2.contains(s)) {
                commons.add(s);
            }
        }
        return commons;
    }

    public static String suffixPath(M3uPlaylist referenceM3uPlayList, String appendSufix) {
        String m3uPlaylistLocalPath = referenceM3uPlayList.getM3uFilePath();
        int idx = m3uPlaylistLocalPath.lastIndexOf('.');
        return m3uPlaylistLocalPath.substring(0, idx) + appendSufix + m3uPlaylistLocalPath.substring(idx);
    }

    public static String computeFileName(String fileWithExtension) {
        return (new File(fileWithExtension)).getName().trim();
    }

    public static String computeFileNameNoExt(String fileWithExtension) {
        String fileName = computeFileName(fileWithExtension);
        int idx = fileName.lastIndexOf('.');
        if (idx < 0) {
            return fileName.trim();
        } else {
            return fileName.substring(0, idx).trim();
        }
    }

    public static String computeFileExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || (idx == fileName.length() - 1)) {
            return "";
        }
        return fileName.substring(idx + 1).trim();
    }
}
