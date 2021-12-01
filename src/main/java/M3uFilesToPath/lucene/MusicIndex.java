package M3uFilesToPath.lucene;

import M3uFilesToPath.m3u.M3uPlaylist;
import M3uFilesToPath.m3u.M3uPlaylistSong;
import M3uFilesToPath.util.Util;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: adrr
 * Date: Jan 13, 2011
 * Time: 7:54:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class MusicIndex {
	public static final String ymd = (new SimpleDateFormat("yyyy-MM-dd")).format(new Date());
	public static final Version LUCENE_VERSION = Version.LUCENE_29;
	public static File INDEX_DIR = new File("..\\MusicIndex\\MusicIndex home " + ymd);
	private final M3uPlaylist m3uPlaylist;
	private final boolean forceReCreateIndex;

	public MusicIndex(M3uPlaylist m3uPlaylist, boolean forceReCreateIndex) {
		this.m3uPlaylist = m3uPlaylist;
		this.forceReCreateIndex = forceReCreateIndex;
	}

	public static void reindexSong(String mp3FileFullPath) {
		M3uPlaylistSong song = new M3uPlaylistSong(mp3FileFullPath);
		if (!song.getMp3FilePath().equals(song.getMp3FilePathFull())) {
			System.out.println(song + " nu contine full path ci cai relative !");
			return;
		}
		song.computeFileSize();
		song.fillWithTags();
		IndexWriter writer = null;
		try {
			writer = openIndexWriter(false);
			IndexReader reader = writer.getReader();
			IndexSearcher searcher = new IndexSearcher(reader);
			PhraseQuery phraseQuery = new PhraseQuery();
			phraseQuery.add(new Term("mp3FilePath", mp3FileFullPath));
			TopDocs topDocs = searcher.search(phraseQuery, 2);
			if (topDocs.totalHits != 1) {
				System.out.println("S-a gasit de " + topDocs.totalHits + "ori:\n" + mp3FileFullPath);
				return;
			}
			Document doc = searcher.doc(topDocs.scoreDocs[0].doc);
			song.setSeconds(Integer.parseInt(doc.get("seconds")));
			song.setRawMp3Details(doc.get("rawMp3Details"));
			doc = m3uPlaylistSongDocument(song);
			writer.updateDocument(new Term("mp3FilePath", mp3FileFullPath), doc);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close(true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void miscUpdateM3uPlayListIndex() {
		IndexWriter writer = null;
		try {
			writer = openIndexWriter(false);
			IndexReader reader = writer.getReader();
			IndexSearcher searcher = new IndexSearcher(reader);
			WildcardQuery wildcardQuery = new WildcardQuery(new Term("mp3FilePath", "*"));
			TopDocsCollector results = TopScoreDocCollector.create(reader.numDocs(), true);
			searcher.search(wildcardQuery, results);
			TopDocs topDocs = results.topDocs();
			Document doc;
			String mp3FileName, mp3FileNameNoExt, mp3FilePathFull;
			String author, album;
			boolean docChanged = false;
			for (int j = 0; j < topDocs.totalHits; j++) {
				doc = searcher.doc(topDocs.scoreDocs[j].doc);
				author = doc.get("author");
				album = doc.get("album");
				mp3FilePathFull = doc.get("mp3FilePath");
				mp3FileName = doc.get("mp3FileName");
				if (author == null) {
					doc.add(new Field("author", "",
							Field.Store.YES, Field.Index.NOT_ANALYZED));
					docChanged = true;
				}
				if (album == null) {
					doc.add(new Field("album", "",
							Field.Store.YES, Field.Index.NOT_ANALYZED));
					docChanged = true;
				}
				if (mp3FileName == null) {
					mp3FileName = Util.computeFileName(mp3FilePathFull);
					doc.add(new Field("mp3FileName", mp3FileName,
							Field.Store.YES, Field.Index.NOT_ANALYZED));
					docChanged = true;
				}
				mp3FileNameNoExt = doc.get("mp3FileNameNoExt");
				if (mp3FileNameNoExt == null) {
					mp3FileNameNoExt = Util.computeFileNameNoExt(mp3FilePathFull);
					doc.add(new Field("mp3FileNameNoExt", mp3FileNameNoExt,
							Field.Store.YES, Field.Index.NOT_ANALYZED));
					docChanged = true;
				}
				if (docChanged) {
					writer.updateDocument(new Term("mp3FilePath", mp3FilePathFull), doc);
					docChanged = false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close(true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static Document m3uPlaylistSongDocument(M3uPlaylistSong song) {
		if (song.getSeconds() < 0) {
			//			throw new UnsupportedOperationException(
			//					"Foloseste winamp library pt Read metadata on selected items iar apoi salveaza playlist-ul de indexat.");
			System.out.println(song.getMp3FilePathFull() + " has zero duration!");
		}
		Document doc = new Document();
		doc.add(new Field("seconds", String.valueOf(song.getSeconds()),
				Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("mp3FilePath", song.getMp3FilePathFull(),
				Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("words", Util.concatenate(song.getWords(), " "),
				Field.Store.YES, Field.Index.ANALYZED));
		doc.add(new Field("mp3FileName", song.getMp3FileName(),
				Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("mp3FileNameNoExt", Util.computeFileNameNoExt(song.getMp3FileName()),
				Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("fileSize", String.valueOf(song.getFileSize()),
				Field.Store.YES, Field.Index.NOT_ANALYZED));
		if (song.getRawMp3Details() != null) {
			doc.add(new Field("rawMp3Details", song.getRawMp3Details(),
					Field.Store.YES, Field.Index.NOT_ANALYZED));
		}
		String tagValue;
		for (String tag : M3uPlaylistSong.TAGS) {
			tagValue = song.getTag(tag);
			if (!song.isEmptyTag(tag)) {
				doc.add(new Field(tag, tagValue, Field.Store.YES, Field.Index.NOT_ANALYZED));
			}
		}
		if (song.isEmptyTag("author")) {
			doc.add(new Field("author", "", Field.Store.YES, Field.Index.NOT_ANALYZED));
		}
		if (song.isEmptyTag("album")) {
			doc.add(new Field("album", "", Field.Store.YES, Field.Index.NOT_ANALYZED));
		}
		return doc;
	}

	private static IndexWriter openIndexWriter(boolean create) throws IOException {
		return new IndexWriter(FSDirectory.open(INDEX_DIR), new StandardAnalyzer(LUCENE_VERSION),
				create, IndexWriter.MaxFieldLength.UNLIMITED);
	}

	public static Directory createInMemoryIndex(List<M3uPlaylistSong> songs) {
		try {
			RAMDirectory idx = new RAMDirectory();
			IndexWriter writer = new IndexWriter(idx, new StandardAnalyzer(MusicIndex.LUCENE_VERSION),
					true, IndexWriter.MaxFieldLength.UNLIMITED);
			for (M3uPlaylistSong song : songs) {
				writer.addDocument(m3uPlaylistSongDocument(song));
			}
			writer.optimize();
			writer.close();
			return idx;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void indexM3uPlaylist() {
		IndexWriter writer = null;
		try {
			writer = openAndCreate();
			for (M3uPlaylistSong song : m3uPlaylist.getSongs()) {
				if (song.exists()) {
					writer.addDocument(m3uPlaylistSongDocument(song));
				} else {
					System.out.println("Nu exista pt indexare: " + song);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close(true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private IndexWriter openAndCreate() throws IOException {
		boolean createIndex = false;
		if (!INDEX_DIR.exists()) {
			createIndex = INDEX_DIR.mkdirs();
		} else if (INDEX_DIR.list().length == 0) {
			createIndex = true;
		}
		if (forceReCreateIndex || createIndex) {
			File[] files = INDEX_DIR.listFiles();
			for (File file : files) {
				file.delete();
			}
		}
		return openIndexWriter(forceReCreateIndex || createIndex);
	}
}
