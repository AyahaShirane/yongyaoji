package android.content;
import java.io.File;
public class Context { private final File dir; public Context(File dir){this.dir=dir;} public File getDatabasePath(String name){return new File(dir,name);} }
