package M3uFilesToPath;

public class M3uFilesToPath {
    public static void main(String[] args) throws Exception {
        System.out.println("BEGIN main");
        M3uFilesToPathCopy m3u = new M3uFilesToPathCopy(args);
        m3u.copyFiles();
        System.out.println("END main");
    }
}