package M3uFilesToPath.m3u;

import M3uFilesToPath.lucene.MusicIndexSearcher;
import M3uFilesToPath.util.Util;
import org.farng.mp3.*;
import org.farng.mp3.filename.FilenameTagBuilder;
import org.farng.mp3.id3.*;
import org.farng.mp3.lyrics3.Lyrics3v1;
import org.farng.mp3.lyrics3.Lyrics3v2;
import org.farng.mp3.object.ObjectNumberHashMap;
import org.farng.mp3.object.ObjectStringSizeTerminated;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: adrr
 * Date: Jan 12, 2011
 * Time: 8:07:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class M3uPlaylistSong implements Comparator<M3uPlaylistSong>, Comparable<M3uPlaylistSong> {
	public static final String[] SUPPORTED_AUDIO_EXTENSIONS = new String[] { "mp3", "wma", "mp4",
			"m4a", "3gp", "wav" };
	public static final int MAX_ABSOLUTE_SIZE_DEVIATION = 1024 * 100;
	public static final int MAX_ABSOLUTE_SECONDS_DEVIATION = 5;
	//    private static final String filePathSplitExp = "\\s|[!\"#$%&*\\-_=+,;:'\\.\\[\\]\\(\\){}]";
	//    private static final String[] ignoreWords = {"all", "and", "cds", "ce", "cu", "de", "disc",
	//            "do", "flac", "for", "in", "la", "mp3", "new", "of", "on", "or", "pe", "sa", "si", "the",
	//            "top", "wav", "wma", "[various]"};
	//    private static final String[] ignoreForPrefix = {"track"};
	public static final float MAX_IN_MEMORY_SCORE_DEV = 0.952f;
	public static final String[] TAGS_INCLUDED_IN_WORDS = { "album", "author", "comment", "mp3.id3tag.composer",
			"mp3.id3tag.genre", "mp3.id3tag.orchestra", "mp3.id3tag.publisher", "title",
			"jid3.Original artist(s)/performer(s)", "jid3.Involved people list",
			"jid3.Original Lyricist(s)/text writer(s)", "jid3.Original album/movie/show title",
			"jid3.Conductor/performer refinement", "jid3.Interpreted, remixed, or otherwise modified by",
			"Lyricist/Text writer"
	};
	public static final String[] TAGS = { "album", "author", "bitrate", "comment", "date", "duration",
			"mp3.frequency.hz", "mp3.id3tag.composer", "mp3.id3tag.encoded", "mp3.id3tag.genre",
			"mp3.id3tag.orchestra", "mp3.id3tag.publisher", "mp3.id3tag.track", "mp3.length.bytes", "title",
			"jid3.Original artist(s)/performer(s)", "jid3.Involved people list",
			"jid3.Original Lyricist(s)/text writer(s)", "jid3.Original album/movie/show title",
			"jid3.Conductor/performer refinement", "jid3.Interpreted, remixed, or otherwise modified by",
			"Lyricist/Text writer",
			"id3v1.album", "id3v1.author", "id3v1.title",
			"id3v2.album", "id3v2.author", "id3v2.title"
	};
	private static final int MAX_DUPLICATES_COUNT = 99;
	//    private static final int minLengthForAllowedNumbers = 3;
	private static final int maxFilePathFolderLevel = 0;
	private final Map<String, Object> misc = new TreeMap<>();
	/**
	 * linia cu #.
	 */
	private List<String> mp3Details = new ArrayList<String>();
	private String mp3FilePath;
	private String mp3FileExtension;
	private String mp3FileName;
	private String mp3FilePathFull = null;
	private String rawMp3Details;
	private int seconds = -1;
	private M3uPlaylist m3uPlaylist;
	private int fileSize = -1;//length in bytes
	private Map<String, String> tags = new TreeMap<>();

	public M3uPlaylistSong(String mp3FilePath) {
		this(mp3FilePath, null, null);
	}

	public M3uPlaylistSong(Map<String, String> song) {
		this(song, null);
	}

	public M3uPlaylistSong(Map<String, String> song, M3uPlaylist m3uPlaylist) {
		this(song.get("mp3FilePath"), null, m3uPlaylist);
		setRawMp3Details(song.get("rawMp3Details"));
		misc.put("score", song.get("score"));
		misc.put("query", song.get("query"));
		setFileSize(Integer.parseInt(song.get("fileSize")));
		setSeconds(Integer.parseInt(song.get("seconds")));
		putTags(song);
	}

	public M3uPlaylistSong(String mp3Details, M3uPlaylist m3uPlaylist) {
		this(null, mp3Details, m3uPlaylist);
	}

	public M3uPlaylistSong(String mp3FilePath, String mp3Details, M3uPlaylist m3uPlaylist) {
		setMp3Details(mp3Details);
		setM3uPlaylist(m3uPlaylist);
		setMp3FilePath(mp3FilePath);
	}

	private void appendSplits(String text, List<String> splitsList) {
		text = text.trim().toLowerCase();
		List<String> splits = Util.prepareForLucene(text);
		String str;
		for (Object s : splits) {
			str = s.toString();
			if (str.length() < 1) {
				continue;
			}
			splitsList.add(str);
		}
	}

	//    private boolean isAllowedNumber(String s) {
	//        if (s.length() >= minLengthForAllowedNumbers) {
	//            return true;
	//        }
	//        return !s.matches("\\d++");
	//    }

	//    private boolean startsWithBadPrefix(String s) {
	//        for (String prefix : ignoreForPrefix) {
	//            if (s.startsWith(prefix)) {
	//                return true;
	//            }
	//        }
	//        return false;
	//    }

	public List<String> getMp3FilePathPartsTokenized() {
		File mp3FilePathFile = new File(mp3FilePath);
		List<String> paths = new ArrayList<String>();
		String path;
		int level = 0;
		do {
			path = mp3FilePathFile.getName().toLowerCase();
			if (level == 0 && path.lastIndexOf('.') >= 0) {
				path = path.substring(0, path.lastIndexOf('.'));
			}
			appendSplits(path, paths);
			mp3FilePathFile = mp3FilePathFile.getParentFile();
			level++;
		} while (mp3FilePathFile != null && level <= maxFilePathFolderLevel);
		return paths;
	}

	/**
	 * Set<String> -> duplicatele vor avea aceeas importanta la cautarea lucene ca si cuvintele unice
	 * List<String> -> duplicatele vor avea mai mare importanta la cautarea lucene decat cuvintele unice
	 *
	 * @return
	 */
	public Collection<String> getWords() {
		//        Set<String> words = new TreeSet<String>(mp3Details);
		List<String> words;
		if (tags.isEmpty()) {
			words = new ArrayList<>(mp3Details);
		} else {
			words = new ArrayList<>();
			for (String tag : TAGS_INCLUDED_IN_WORDS) {
				if (isEmptyTag(tag)) {
					continue;
				}
				appendSplits(getTag(tag), words);
			}
		}
		words.addAll(getMp3FilePathPartsTokenized());
		Collections.sort(words);
		return words;
	}

	public Collection<String> getUniqueWords() {
		Collection<String> words = getWords();
		return new TreeSet<>(words);
	}

	public String getRawMp3Details() {
		return rawMp3Details;
	}

	public void setRawMp3Details(String rawMp3Details) {
		if (rawMp3Details == null) {
			return;
		}
		this.rawMp3Details = rawMp3Details;
		appendSplits(rawMp3Details, this.mp3Details);
	}

	public String getMp3FilePathFull() {
		return mp3FilePathFull;
	}

	public List<String> getMp3Details() {
		return mp3Details;
	}

	public void setMp3Details(String mp3Details) {
		if (mp3Details == null) {
			return;
		}
		int idx = mp3Details.indexOf(',') + 1;
		if (idx <= 0 || mp3Details.length() == idx) {
			setRawMp3Details(mp3Details);
			return;
		}
		try {
			seconds = Integer.parseInt(mp3Details.substring(
					M3uPlaylist.detailsHeaderPrefix.length(), idx - 1));
		} catch (Exception e) {
			seconds = 0;
		}
		setRawMp3Details(mp3Details.substring(idx));
	}

	public void setMp3Details(List<String> mp3Details) {
		this.mp3Details = mp3Details;
	}

	public String getMp3FileName() {
		return mp3FileName;
	}

	public void setMp3FileName(String mp3FileName) {
		this.mp3FileName = mp3FileName;
	}

	public String getMp3FileNameWithoutExtension() {
		return mp3FileExtension == null ? mp3FileName : mp3FileName.substring(0, mp3FileName.lastIndexOf('.'));
	}

	public void setDeviceSongPath(String deviceSongPath) {
		// http://127.0.0.1:54387/?a=&l=&t=Ashanti%20feat.%20T.I.%20-%20I%20Know%20%28Main%20Mix%29&d=WALKMAN;.mp3
		// http://127.0.0.1:54387/?a=ABBA&l=M1%2B%20Top%201000&t=Dancing%20Queen&d=WALKMAN;.mp3
		try {
			URL url = new URL(deviceSongPath);
			deviceSongPath = url.getQuery();
			deviceSongPath = URLDecoder.decode(deviceSongPath, StandardCharsets.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(deviceSongPath);
		}
		deviceSongPath = deviceSongPath.substring(deviceSongPath.indexOf("a="));
		int idxWalkman = deviceSongPath.indexOf("&d=WALKMAN");
		String extension = null;
		if (deviceSongPath.length() > idxWalkman + "&d=WALKMAN".length()) {
			extension = deviceSongPath.substring(idxWalkman + "&d=WALKMAN".length() + 1);
		}
		deviceSongPath = deviceSongPath.substring(0, idxWalkman);
		int idxAlbum = deviceSongPath.indexOf("&l=");
		String artist = deviceSongPath.substring("a=".length(), idxAlbum);
		int idxTitle = deviceSongPath.indexOf("&t=");
		String album = deviceSongPath.substring(idxAlbum + "&l=".length(), idxTitle);
		String title = deviceSongPath.substring(idxTitle + "&t=".length());
		if (!artist.trim().equals("")) {
			putTag("author", artist, false);
		}
		if (!album.trim().equals("")) {
			putTag("album", album, false);
		}
		putTag("title", title, false);
		if (extension != null) {
			setMp3FilePath(computeAAlTPath() + extension);
		} else {
			setMp3FilePath(computeAAlTPath());
		}
	}

	public void fillWithTags() {
		fillWithTagsJavaZoom();
		fillWithTagsJid3();
		//		fillTagsWithJavaAudioSystem();
		updateSecondsFromTags();
		updateFileSizeFromTags();
	}

	private void updateSecondsFromTags() {
		if (seconds < 0 && tags.get("duration") != null) {
			try {
				seconds = (int) (Long.parseLong(tags.get("duration")) / 1000000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void updateFileSizeFromTags() {
		if (fileSize < 0 && tags.get("mp3.length.bytes") != null) {
			try {
				fileSize = Integer.parseInt(tags.get("mp3.length.bytes"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Standard parameters :
	 * - duration : [Long], duration in microseconds.
	 * - title : [String], Title of the stream.
	 * - author : [String], Name of the artist of the stream.
	 * - album : [String], Name of the album of the stream.
	 * - date : [String], The date (year) of the recording or release of the stream.
	 * - copyright : [String], Copyright message of the stream.
	 * - comment : [String], Comment of the stream.  Extended MP3 parameters :
	 * - mp3.version.mpeg : [String], mpeg version : 1,2 or 2.5
	 * - mp3.version.layer : [String], layer version 1, 2 or 3
	 * - mp3.version.encoding : [String], mpeg encoding : MPEG1, MPEG2-LSF, MPEG2.5-LSF
	 * - mp3.channels : [Integer], number of channels 1 : mono, 2 : stereo.
	 * - mp3.frequency.hz : [Integer], sampling rate in hz.
	 * - mp3.bitrate.nominal.bps : [Integer], nominal bitrate in bps.
	 * - mp3.length.bytes : [Integer], length in bytes.
	 * - mp3.length.frames : [Integer], length in frames.
	 * - mp3.framesize.bytes : [Integer], framesize of the first frame.
	 * framesize is not constant for VBR streams.
	 * - mp3.framerate.fps : [Float], framerate in frames per seconds.
	 * - mp3.header.pos : [Integer], position of first audio header (or ID3v2 size).
	 * - mp3.vbr : [Boolean], vbr flag.
	 * - mp3.vbr.scale : [Integer], vbr scale.
	 * - mp3.crc : [Boolean], crc flag.
	 * - mp3.original : [Boolean], original flag.
	 * - mp3.copyright : [Boolean], copyright flag.
	 * - mp3.padding : [Boolean], padding flag.
	 * - mp3.mode : [Integer], mode 0:STEREO 1:JOINT_STEREO 2:DUAL_CHANNEL 3:SINGLE_CHANNEL
	 * - mp3.id3tag.genre : [String], ID3 tag (v1 or v2) genre.
	 * - mp3.id3tag.track : [String], ID3 tag (v1 or v2) track info.
	 * - mp3.id3tag.v2 : [InputStream], ID3v2 frames.
	 * - mp3.shoutcast.metadata.key : [String], Shoutcast meta key with matching value.
	 * For instance :
	 * mp3.shoutcast.metadata.icy-irc=#shoutcast
	 * mp3.shoutcast.metadata.icy-metaint=8192
	 * mp3.shoutcast.metadata.icy-genre=Trance Techno Dance
	 * mp3.shoutcast.metadata.icy-url=http://www.di.fm
	 */
	private void fillWithTagsJavaZoom() {
		try {
			File file = new File(getMp3FilePathFull());
			AudioFileFormat baseFileFormat;
			AudioFormat baseFormat;
			baseFileFormat = AudioSystem.getAudioFileFormat(file);
			baseFormat = baseFileFormat.getFormat();
			// TAudioFileFormat properties
			if (baseFileFormat instanceof TAudioFileFormat) {
				Map<String, Object> properties = baseFileFormat.properties();
				// tags.putAll(properties);
				putTags(properties);
			}
			// TAudioFormat	or AudioFormat properties
			if (baseFormat != null) {
				Map<String, Object> properties = baseFormat.properties();
				// tags.putAll(properties);
				putTags(properties);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("ERR (JavaZoom) reading tags for: " + mp3FilePathFull);
		}
	}

	/*private void fillTagsWithJavaAudioSystem() {
		try {
			URL url = new File(mp3FilePathFull).toURI().toURL();
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(mp3FilePathFull));
			AudioFormat format = audioInputStream.getFormat();
			AudioFileFormat fformat = AudioSystem.getAudioFileFormat(url);
			Map<String, Object> properties = fformat.properties();
			//			tags.putAll(properties);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	private void fillWithTagsJid3() {
		try {
			org.farng.mp3.MP3File mp3File = constructJid3MP3File();
			if (mp3File.hasID3v1Tag()) {
				ID3v1 id3v1 = mp3File.getID3v1Tag();
				putTag("album", id3v1.getAlbum(), false);
				putTag("author", id3v1.getArtist(), false);//folosit de winamp la salvare pe sony
				putTag("title", id3v1.getTitle(), false);
				putTag("id3v1.album", id3v1.getAlbum(), false);
				putTag("id3v1.author", id3v1.getArtist(), false);//folosit de winamp la salvare pe sony
				putTag("id3v1.title", id3v1.getTitle(), false);
				putTag("date", id3v1.getYear(), false);
				if (id3v1 instanceof ID3v1_1) {
					putTag("mp3.id3tag.track", id3v1.getTrackNumberOnAlbum(), false);
				}
				putTag("mp3.id3tag.genre", (String)
						TagConstant.genreIdToString.get(Long.valueOf(id3v1.getSongGenre())), false);
			}
			if (!mp3File.hasID3v2Tag()) {
				return;
			}

			AbstractID3v2 id3v2 = mp3File.getID3v2Tag();
			AbstractID3v2Frame v2Frame;
			String identifier;
			String frameDescr = null, frameText;
			for (Iterator it1 = id3v2.getFrameIterator(); it1.hasNext(); ) {
				v2Frame = (AbstractID3v2Frame) it1.next();
				identifier = v2Frame.getIdentifier();
				if (identifier.length() > 4) {
					identifier = v2Frame.getIdentifier().substring(0, 4);
				} else if (identifier.length() < 4) {
					//                    System.out.println("identifier = " + identifier);
				}
				if (v2Frame instanceof ID3v2_4Frame) {
					frameDescr = (String) TagConstant.id3v2_4FrameIdToString.get(identifier);
				} else if (v2Frame instanceof ID3v2_3Frame) {
					frameDescr = (String) TagConstant.id3v2_3FrameIdToString.get(identifier);
				} else if (v2Frame instanceof ID3v2_2Frame) {
					frameDescr = (String) TagConstant.id3v2_2FrameIdToString.get(identifier);
				}
				if (frameDescr == null) {
					continue;
				}
				if (frameDescr.startsWith("Text: ") && frameDescr.length() > "Text: ".length()) {
					frameDescr = frameDescr.substring("Text: ".length());
				} else if (frameDescr.startsWith("URL: ") && frameDescr.length() > "URL: ".length()) {
					frameDescr = frameDescr.substring("URL: ".length());
				}
				frameText = getFragmentBodyText(v2Frame.getBody());
				if (frameText == null) {
					continue;
				}
				if (frameDescr.equals("Band/orchestra/accompaniment")) {
					putTag("mp3.id3tag.orchestra", frameText, false);
				} else if (frameDescr.equals("Year")) {
					putTag("date", frameText, false);
				} else if (frameDescr.equals("Original release year")) {
					putTag("date", frameText, false);
				} else if (frameDescr.equals("Date")) {
					putTag("date", frameText, false);
				} else if (frameDescr.equals("Album/Movie/Show title")) {
					putTag("album", frameText, false);
					putTag("id3v2.album", frameText, false);
				} else if (frameDescr.equals("Content type")) {
					putTag("mp3.id3tag.genre", frameText, false);
				} else if (frameDescr.equals("Title/songname/content description")) {
					putTag("title", frameText, false);
					putTag("id3v2.title", frameText, false);
				} else if (frameDescr.equals("Track number/Position in set")) {
					putTag("mp3.id3tag.track", frameText, false);
				} else if (frameDescr.equals("Composer")) {
					putTag("mp3.id3tag.composer", frameText, false);
				} else if (frameDescr.equals("Lead performer(s)/Soloist(s)")) {
					putTag("author", frameText, false);
					putTag("id3v2.author", frameText, false);
				} else if (frameDescr.equals("Publisher")) {
					putTag("mp3.id3tag.publisher", frameText, false);
				} else if (frameDescr.equals("Comments")) {
					putTag("comment", frameText, false);
				} else if (frameDescr.equals("Encoded by")) {
					putTag("mp3.id3tag.encoded", frameText, false);
				} else if (frameDescr.equals("Part of a set")) {
					putTag("mp3.id3tag.disc", frameText, false);
				} else if (isAllowedTag(frameDescr)) {
					putTag("jid3." + frameDescr, frameText, false);
				} else {
					putTag("jid3." + frameDescr, frameText, false);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("ERR (jid3) reading tags for: " + mp3FilePathFull);
		}
	}

	private boolean isAllowedTag(String s) {
		for (String tag : TAGS) {
			if (tag.indexOf("jid3.") == 0) {
				tag = tag.substring("jid3.".length());
			}
			if (tag.indexOf(s) == 0) {
				return true;
			}
		}
		return false;
	}

	private MP3File constructJid3MP3File() throws IOException, TagException {
		try {
			return new MP3File(mp3FilePathFull);
		} catch (Exception e) {
			e.printStackTrace();
		}
		MP3File mp3File = new MP3File();
		mp3File.setMp3file(new File(mp3FilePathFull));
		RandomAccessFile newFile = null;
		try {
			newFile = new RandomAccessFile(mp3FilePathFull, "r");
			try {
				mp3File.setID3v1Tag(new ID3v1_1(newFile));
			} catch (TagNotFoundException ex) {
				// tag might be different version
			}
			try {
				if (!mp3File.hasID3v1Tag()) {
					mp3File.setID3v1Tag(new ID3v1(newFile));
				}
			} catch (TagNotFoundException ex) {
				// ok if it's null
			}
			try {
				mp3File.setID3v2Tag(new ID3v2_4(newFile));
			} catch (TagNotFoundException ex) {
				// maybe different version
			}
			try {
				if (!mp3File.hasID3v2Tag()) {
					mp3File.setID3v2Tag(new ID3v2_3(newFile));
				}
			} catch (TagNotFoundException ex) {
				// maybe a different version
			} catch (UnsupportedOperationException uoe) {
				//                ex.printStackTrace();
			} catch (Exception ex) {
				//                ex.printStackTrace();
			}
			try {
				if (!mp3File.hasID3v2Tag()) {
					mp3File.setID3v2Tag(new ID3v2_2(newFile));
				}
			} catch (TagNotFoundException ex) {
				// it's ok to be null
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			try {
				mp3File.setLyrics3Tag(new Lyrics3v2(newFile));
			} catch (TagNotFoundException ex) {
				// maybe a different version
			}
			try {
				if (!mp3File.hasLyrics3Tag()) {
					mp3File.setLyrics3Tag(new Lyrics3v1(newFile));
				}
			} catch (TagNotFoundException ex) {
				//it's ok to be null
			}
		} finally {
			if (newFile != null) {
				newFile.close();
			}
		}
		try {
			mp3File.setFilenameTag(FilenameTagBuilder.createFilenameTagFromMP3File(mp3File));
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("ERR createFileNameTagFromMP3File:\n" + mp3FilePathFull);
		}
		return mp3File;
	}

	private String getFragmentBodyText(AbstractMP3FragmentBody fragmentBody)
			throws UnsupportedEncodingException {
		Iterator it1 = fragmentBody.iterator();
		if (!it1.hasNext()) {
			return null;
		}
		Object object = it1.next();
		if (object instanceof ObjectNumberHashMap) {
			ObjectNumberHashMap objectNumberHashMap = (ObjectNumberHashMap) object;
			Long encodingId = (Long) objectNumberHashMap.getValue();
			String encoding = (String) objectNumberHashMap.getIdToString().get(encodingId);
			object = it1.next();
			if (object instanceof ObjectStringSizeTerminated) {
				ObjectStringSizeTerminated stringSizeTerminated = (ObjectStringSizeTerminated) object;
				return new String(stringSizeTerminated.writeByteArray(), encoding);
			} else {
				while (it1.hasNext()) {
					object = it1.next();
					if (object instanceof ObjectStringSizeTerminated) {
						break;
					}
				}
				if (object instanceof ObjectStringSizeTerminated) {
					ObjectStringSizeTerminated stringSizeTerminated = (ObjectStringSizeTerminated) object;
					return new String(stringSizeTerminated.writeByteArray(), encoding);
				}
			}
		}
		return null;
	}

	public boolean updateArtist(boolean overwrite) {
		try {
			org.farng.mp3.MP3File mp3File = constructJid3MP3File();
			ID3v1 id3v1;
			if (mp3File.hasID3v1Tag()) {
				id3v1 = mp3File.getID3v1Tag();
			} else {
				id3v1 = new ID3v1_1();
				mp3File.setID3v1Tag(id3v1);
			}
			id3v1.setArtist(tags.get("author"));
			if (overwrite) {
				TagOptionSingleton.getInstance().setDefaultSaveMode(TagConstant.MP3_FILE_SAVE_OVERWRITE);
			} else {
				TagOptionSingleton.getInstance().setDefaultSaveMode(TagConstant.MP3_FILE_SAVE_WRITE);
			}
			mp3File.save();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("writeAlbumArtist orchestra = " + tags.get("orchestra") +
					"\nmp3FilePathFull: " + mp3FilePathFull);
		} finally {
			TagOptionSingleton.getInstance().setDefaultSaveMode(TagConstant.MP3_FILE_SAVE_WRITE);
		}
		return false;
	}

	public boolean updateAlbumArtist(boolean overwrite) {
		try {
			org.farng.mp3.MP3File mp3File = constructJid3MP3File();
			//            ID3v1 id3v1 = mp3File.getID3v1Tag();
			AbstractID3v2 id3v2;
			if (mp3File.hasID3v2Tag()) {
				id3v2 = mp3File.getID3v2Tag();
			} else {
				id3v2 = new ID3v2_4();
				mp3File.setID3v2Tag(id3v2);
			}

			FrameBodyTPE2 fragmentBody;
			if (id3v2.hasFrame("TPE2")) {
				AbstractID3v2Frame v2Frame = id3v2.getFrame("TPE2");
				fragmentBody = (FrameBodyTPE2) v2Frame.getBody();
			} else {
				fragmentBody = new FrameBodyTPE2(
						(byte) ((Long) TagConstant.textEncodingStringToId.get("UTF-16")).longValue(),
						null);
				AbstractID3v2Frame v2Frame;
				if (id3v2 instanceof ID3v2_4) {
					v2Frame = new ID3v2_4Frame(fragmentBody);
				} else if (id3v2 instanceof ID3v2_3) {
					v2Frame = new ID3v2_3Frame(fragmentBody);
				} else {
					v2Frame = new ID3v2_2Frame(fragmentBody);
				}
				id3v2.setFrame(v2Frame);
			}
			Iterator it1 = fragmentBody.iterator();
			ObjectNumberHashMap objectNumberHashMap = (ObjectNumberHashMap) it1.next();
			Long encodingCode = (Long) objectNumberHashMap.getValue();
			String encoding = (String) objectNumberHashMap.getIdToString().get(encodingCode);
			ObjectStringSizeTerminated objectStringHashMap = (ObjectStringSizeTerminated) it1.next();
			//            String orchestra = new String(objectStringHashMap.writeByteArray(), encoding);
			//            System.out.println("Band/orchestra/accompaniment: " + orchestra);
			objectStringHashMap.readByteArray(tags.get("mp3.id3tag.orchestra").getBytes(encoding));

			if (overwrite) {
				TagOptionSingleton.getInstance().setDefaultSaveMode(TagConstant.MP3_FILE_SAVE_OVERWRITE);
			} else {
				TagOptionSingleton.getInstance().setDefaultSaveMode(TagConstant.MP3_FILE_SAVE_WRITE);
			}
			mp3File.save();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("writeAlbumArtist orchestra = " + tags.get("orchestra") +
					"\nmp3FilePathFull: " + mp3FilePathFull);
		} finally {
			TagOptionSingleton.getInstance().setDefaultSaveMode(TagConstant.MP3_FILE_SAVE_WRITE);
		}
		return false;
	}

	private String computeMp3FilePathFull(String mp3FilePath, M3uPlaylist m3uPlaylist) {
		if (mp3FilePath.indexOf(':') >= 0) {
			return mp3FilePath;
		} else if (mp3FilePath.startsWith(File.separator)) {
			return (new File(mp3FilePath)).getAbsolutePath();
		} else {
			if (m3uPlaylist == null || m3uPlaylist.getM3uFilePath() == null) {
				return null;
			}
			String basePath = (new File(m3uPlaylist.getM3uFilePath())).getParent();
			return (new File(basePath + File.separatorChar + mp3FilePath)).getAbsolutePath();
		}
	}

	public String getMp3FilePath() {
		return mp3FilePath;
	}

	public void setMp3FilePath(String mp3FilePath) {
		this.mp3FilePath = mp3FilePath;
		this.mp3FileName = null;
		this.mp3FileExtension = null;
		this.mp3FilePathFull = null;
		if (this.mp3FilePath != null) {
			this.mp3FileName = Util.computeFileName(this.mp3FilePath);
			this.mp3FileExtension = Util.computeFileExtension(mp3FileName);
			this.mp3FilePathFull = computeMp3FilePathFull(this.mp3FilePath, this.m3uPlaylist);
		}
	}

	@Override
	public String toString() {
		return toString(false, false, false, null);
	}

	public String toString(boolean showDetails, boolean showFilePath,
			boolean showMp3FilePathParts, String[] miscToShow) {
		StringBuilder sb = new StringBuilder();
		if (this.getM3uPlaylist() != null && this.getM3uPlaylist().getLoadedFromDevice()) {
			sb.append("My Computer\\WALKMAN\\Storage Media\\MUSIC\\");
			sb.append(mp3FilePath);
			return sb.toString();
		}
		sb.append('(').append(seconds).append(") ");
		if (getMp3FilePathFull() != null) {
			sb.append(getMp3FilePathFull());
		} else {
			sb.append(getMp3FilePath());
		}
		if (showFilePath) {
			sb.append("\nfilePath: ");
			if (showMp3FilePathParts) {
				appendToStringBuilder(getMp3FilePathPartsTokenized(), sb);
			} else {
				sb.append(mp3FilePath);
			}
		}
		if (showDetails && mp3Details != null && !mp3Details.isEmpty()) {
			sb.append("\ndetails: ");
			appendToStringBuilder(mp3Details, sb);
		}
		if (miscContainsAnyKey(miscToShow)) {
			sb.append(" (");
			boolean firstMisc = true;
			for (String key : miscToShow) {
				if (!misc.containsKey(key)) {
					continue;
				}
				if (!firstMisc) {
					sb.append(", ");
				}
				sb.append(key).append("=").append(misc.get(key));
				firstMisc = false;
			}
			sb.append(")");
		}
		return sb.toString();
	}

	private boolean miscContainsAnyKey(String[] keys) {
		if (keys == null) {
			return false;
		}
		for (String key : keys) {
			if (misc.containsKey(key)) {
				return true;
			}
		}
		return false;
	}

	public M3uPlaylist getDupPlayList(String duplicatesBasePath, float maxScoreDeviationPercentAgainstMatch,
			boolean dontReturnForOverMaxScoreDeviation, boolean foundSongCanMiss) {
		MusicIndexSearcher searcher = new MusicIndexSearcher();
		// In functie de deviatie, duplicatele gasite pot fi realmente sau nu duplicate !
		// E preferabil ca deviatia acceptata sa fie cat mai mica.
		searcher.setMaxSecondsDeviationPercent(0f);
		searcher.setDontReturnForOverMaxSecondsDeviation(true);
		searcher.setMaxScoreDeviationPercentAgainstMatch(maxScoreDeviationPercentAgainstMatch);
		searcher.setDontReturnForOverMaxScoreDeviation(dontReturnForOverMaxScoreDeviation);
		searcher.setThrowErrorOnPerfectMatchNotFound(!foundSongCanMiss);
		searcher.setResultPerSong(MAX_DUPLICATES_COUNT);
		searcher.setFoundSongCanMiss(foundSongCanMiss);
		List<M3uPlaylistSong> resultSongs = searcher.searchSong(this);
		if (resultSongs.isEmpty()) {
			throw new RuntimeException("Nu s-a gasit in index:\n" + this);
		}

		// a) daca indexul in care se cauta contine songToSearchForDups atunci inseamna ca SI chiar songToSearchForDups a fost gasit !
		// b) daca songToSearchForDups NU este indexat atunci inseamna ca s-a gasit o melodie care DOAR este posibil sa fie un duplicat !
		// Se asuma ca suntem in cazul a).
		M3uPlaylistSong songMatch = this.getPerfectMatchSong(resultSongs, searcher);

		if (resultSongs.size() == 1) {
			// practic s-a gasit chiar melodia cautata
			return null;
		}

		this.putMisc("score", songMatch.getMisc("score"));

		resultSongs.remove(songMatch);

		String playListWithDupPath = this.computePlayListWithDupPath(duplicatesBasePath);
		M3uPlaylist playListWithDuplicates = new M3uPlaylist(playListWithDupPath, true);
		playListWithDuplicates.addSongs(resultSongs);
		return playListWithDuplicates;
	}

	public M3uPlaylistSong getPerfectMatchSong(List<M3uPlaylistSong> resultSongs,
			MusicIndexSearcher searcher) {
		if (this.getM3uPlaylist().getLoadedFromDevice()) {
			return resultSongs.get(0);
		}
		String mp3FileNameLCNoExt = Util.computeFileNameNoExt(mp3FileName).toLowerCase();
		String songLCNoExt;
		Collection<String> words = getWords();
		Set<String> commons;
		List<M3uPlaylistSong> songsFoundInMem;
		for (M3uPlaylistSong song : resultSongs) {
			if (Math.abs(seconds - song.getSeconds()) > MAX_ABSOLUTE_SECONDS_DEVIATION) {
				continue;
			}
			songLCNoExt = Util.computeFileNameNoExt(song.getMp3FileName()).toLowerCase();
			if (songLCNoExt.contains(mp3FileNameLCNoExt) || mp3FileNameLCNoExt.contains(songLCNoExt)) {
				return song;
			}
			commons = Util.getEquals(words, song.getWords());
			if (commons.isEmpty()) {
				// song nu are nici un cuvant in comun cu this
				continue;
			}
			songsFoundInMem = searcher.searchSongInMemory(this, song, 2);
			if (!areSongsFoundInMemSimilar(songsFoundInMem)) {
				continue;
			}
			song.putMisc("inMemScoreDev", songsFoundInMem.get(0).getMisc("inMemScoreDev"));
			return song;
		}
		return null;
	}

	private boolean areSongsFoundInMemSimilar(List<M3uPlaylistSong> songsInFoundMem) {
		if (songsInFoundMem.size() < 2) {
			return false;
		}
		float original = Float.parseFloat((String) songsInFoundMem.get(0).getMisc("score"));
		float copy = Float.parseFloat((String) songsInFoundMem.get(1).getMisc("score"));
		float inMemScoreDev = (original - copy) / original;
		songsInFoundMem.get(0).putMisc("inMemScoreDev", String.valueOf(inMemScoreDev));
		System.out.println(this.getMp3FilePath() + ": inMemScoreDev = " + inMemScoreDev);
		return inMemScoreDev <= MAX_IN_MEMORY_SCORE_DEV;
	}

	private String computePlayListWithDupPath(String duplicatesBasePath) {
		String playListWithDupPath = this.getMp3FilePath();
		playListWithDupPath = playListWithDupPath.substring(0, playListWithDupPath.lastIndexOf('.'));
		playListWithDupPath = playListWithDupPath.replaceAll("[\\\\]", "-");
		playListWithDupPath = duplicatesBasePath + File.separatorChar + playListWithDupPath + ".m3u";
		return playListWithDupPath;
	}

	private void appendToStringBuilder(List<String> strings, StringBuilder sb) {
		boolean isFirst = true;
		for (String s : strings) {
			if (!isFirst) {
				sb.append(' ');
			} else {
				isFirst = false;
			}
			sb.append(s);
		}
	}

	public void computeFileSize() {
		String mp3FileFullPath = getMp3FilePathFull();
		File file = new File(mp3FileFullPath);
		this.fileSize = (int) file.length();
		//        if (mp3FilePath.indexOf("Black Eyed Peas - Don't Phunk With My Heart") >= 0 ||
		//                mp3FilePath.indexOf("08.If I Can't.mp3") >= 0) {
		//            System.out.println("load mp3FilePath = " + mp3FilePath + " fileSize = " + fileSize);
		//        }
		//        System.out.println("load mp3FilePath = " + mp3FilePath + " fileSize = " + fileSize);
		if (this.fileSize == 0) {
			if (file.exists()) {
				// probabil este un fisier generat de torrent dar care inca nu s-a downloadat
				return;
			}
			this.fileSize = -1;
			if (m3uPlaylist.getDontThrowErrForMissingFilesWhenComputingSize()) {
				return;
			}
			throw new RuntimeException(mp3FileFullPath + " NU exista!");
		}
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

	public Object putMisc(String key, Object value) {
		return misc.put(key, value);
	}

	public Object getMisc(String key) {
		return misc.get(key);
	}

	public Map<String, Object> getMisc() {
		return misc;
	}

	public boolean exists() {
		if (this.fileSize == -1) {
			String mp3FileFullPath = getMp3FilePathFull();
			return (new File(mp3FileFullPath)).exists();
		}
		return this.fileSize >= 0L;
	}

	public int getSeconds() {
		return seconds;
	}

	public void setSeconds(int seconds) {
		this.seconds = seconds;
	}

	public M3uPlaylist getM3uPlaylist() {
		return m3uPlaylist;
	}

	public void setM3uPlaylist(M3uPlaylist m3uPlaylist) {
		this.m3uPlaylist = m3uPlaylist;
		this.mp3FilePathFull = null;
		if (this.mp3FilePath != null) {
			this.mp3FilePathFull = computeMp3FilePathFull(this.mp3FilePath, this.m3uPlaylist);
		}
	}

	public String getMp3FileExtension() {
		return mp3FileExtension;
	}

	public void setMp3FileExtension(String mp3FileExtension) {
		this.mp3FileExtension = mp3FileExtension;
	}

	public boolean isSupportedAudio() {
		for (String extension : SUPPORTED_AUDIO_EXTENSIONS) {
			if (this.mp3FileExtension.equalsIgnoreCase(extension)) {
				return true;
			}
		}
		return false;
	}

	public boolean isEmptyTag(String key) {
		String value = tags.get(key);
		return value == null || value.length() == 0;
	}

	public String getTag(String key) {
		return tags.get(key);
	}

	public Long getTagLong(String key) {
		String val = tags.get(key);
		if (val == null) {
			return null;
		}
		return new Long(val);
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

	public void putTag(String key, String value, boolean overwrite) {
		if (value == null) {
			if (overwrite) {
				tags.remove(key);
			}
			return;
		}
		value = value.trim();
		if (value.equals("")) {
			if (overwrite) {
				tags.remove(key);
			}
			return;
		}
		if (overwrite || tags.get(key) == null) {
			tags.put(key, value);
		}
	}

	public void putTags(Map tags) {
		Object value;
		for (String tag : TAGS) {
			value = tags.get(tag);
			if (tag == null) {
				continue;
			}
			putTag(tag, value == null ? null : value.toString(), false);
		}
	}

	public boolean isExtractedFromDevice() {
		return rawMp3Details != null && rawMp3Details.startsWith("http");
	}

	private String computeAAlTPath() {
		StringBuilder sb = new StringBuilder();
		if (!isEmptyTag("author")) {
			sb.append(tags.get("author")).append(File.separatorChar);
		} else {
			sb.append("Unknown Artist").append(File.separatorChar);
		}
		if (!isEmptyTag("album")) {
			sb.append(tags.get("album")).append(File.separatorChar);
		} else {
			sb.append("Unknown Album").append(File.separatorChar);
		}
		sb.append(tags.get("title"));
		return sb.toString();
	}

	public int compareTo(M3uPlaylistSong song) {
		return computeAAlTPath().compareTo(song.computeAAlTPath());
	}

	public int compare(M3uPlaylistSong o1, M3uPlaylistSong o2) {
		return o1.compareTo(o2);
	}

	public boolean equals(Object pSong) {
		if (!(pSong instanceof M3uPlaylistSong)) {
			return false;
		}
		M3uPlaylistSong song = (M3uPlaylistSong) pSong;
		if (this == song) {
			return true;
		}
		if (mp3FilePathFull == null && song.getMp3FilePathFull() != null) {
			return false;
		} else if (mp3FilePathFull != null && song.getMp3FilePathFull() == null) {
			return false;
		} else if (mp3FilePathFull != null && song.getMp3FilePathFull() != null &&
				!mp3FilePathFull.equals(song.getMp3FilePathFull())) {
			return false;
		} else if (!mp3FilePath.equals(song.getMp3FilePath())) {
			return false;
		}
		if (!this.rawMp3Details.equals(song.getRawMp3Details())) {
			return false;
		}
		if (this.fileSize != song.getFileSize()) {
			return false;
		}
		for (Map.Entry<String, String> tag : tags.entrySet()) {
			if (!tag.getValue().equals(song.getTag(tag.getKey()))) {
				return false;
			}
		}
		return true;
	}
}
