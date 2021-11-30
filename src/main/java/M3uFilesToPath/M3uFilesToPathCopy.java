package M3uFilesToPath;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: adr
 * Date: Oct 27, 2007
 * Time: 10:44:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class M3uFilesToPathCopy {
    private File m3uFile;
    private File pathToCopyTo;

    /**
     * args[0] - fisier m3u sau cale relativa
     * args[1] - director destinatie
     *
     * @param args
     * @throws Exception
     */
    public M3uFilesToPathCopy(String[] args) throws Exception {
        String m3uFilePath;
        String sPathToCopyTo;
        if (args.length > 0 && args[0].indexOf(".m3u") >= 0) {
            m3uFilePath = args[0];
        } else {
//            Console cons = System.console();//jsdk 1.6
            LineNumberReader cons = new LineNumberReader(new InputStreamReader(System.in));
            System.out.println("Calea cu numele fisierului m3u:");
            m3uFilePath = cons.readLine();
        }
        System.out.println("m3uFilePath: " + m3uFilePath);
        m3uFile = new File(m3uFilePath);
        if (!m3uFile.exists()) {
            System.out.println(m3uFilePath + " nu exista !");
        } else {
            System.out.println("m3uFile: " + m3uFile.getCanonicalPath());
        }
        if (args.length > 1) {
            sPathToCopyTo = args[1];
        } else {
            sPathToCopyTo = m3uFile.getParent() + File.separator + m3uFile.getName()
                    .substring(0, m3uFile.getName().lastIndexOf('.'));
        }
        System.out.println("sPathToCopyTo: " + sPathToCopyTo);
        pathToCopyTo = new File(sPathToCopyTo);
        if (!pathToCopyTo.exists()) {
            System.out.println(pathToCopyTo.getCanonicalPath() + " se va creea !");
        } else {
            System.out.println("pathToCopyTo: " + pathToCopyTo.getCanonicalPath());
        }
    }

    public void copyFiles() throws Exception {
        List files = getFiles();
        Iterator it1 = files.iterator();
        File musicFileSource;
        System.out.println("Will copy to:");
        pathToCopyTo.mkdir();
        while (it1.hasNext()) {
            musicFileSource = (File) it1.next();
            writeBytesToFile(pathToCopyTo.getPath() + File.separator + musicFileSource.getName(),
                             getFileBytes(musicFileSource));
        }
    }

    private void writeBytesToFile(String fileName, byte[] bytes) throws Exception {
        File file = new File(fileName);
        if (file.exists()) {
            System.out.println(file.getCanonicalPath() + " - already exists");
            return;
        }
        System.out.println(file.getCanonicalPath());
        FileOutputStream writer = new FileOutputStream(file);
        writer.write(bytes);
    }

    private byte[] getFileBytes(File file) throws Exception {
        FileInputStream reader = new FileInputStream(file);
        int available = reader.available();
        if (available == 0) {
            return null;
        }
        byte[] bytes = new byte[available];
        reader.read(bytes);
        return bytes;
    }

    private List getFiles() throws Exception {
        LineNumberReader lineNumberReader = new LineNumberReader(new BufferedReader(new FileReader(m3uFile)));
        String line;
        List files = new ArrayList();
        File file;
        System.out.println("Music files to copy:");
        while ((line = lineNumberReader.readLine()) != null) {
            file = getFile(line);
            if (file == null) {
                continue;
            }
            System.out.println(file.getCanonicalPath());
            files.add(file);
        }
        lineNumberReader.close();
        return files;
    }

    private File getFile(String m3uLine) {
        if (m3uLine.charAt(0) == '#') {
            return null;
        }
        return new File(m3uFile.getParent() + File.separator + m3uLine);
    }
}
