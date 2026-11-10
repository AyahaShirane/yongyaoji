package android.database.sqlite;
import android.content.*; import android.database.Cursor; import java.sql.*; import java.util.*;
public class SQLiteDatabase {
 public static final int CONFLICT_IGNORE=4; final Connection c; public SQLiteDatabase(Connection c){this.c=c;} public void execSQL(String s){try(Statement q=c.createStatement()){q.execute(s);}catch(SQLException e){throw new RuntimeException(e);}}
 public void beginTransaction(){execSQL("BEGIN");} public void setTransactionSuccessful(){successful=true;} boolean successful; public void endTransaction(){execSQL(successful?"COMMIT":"ROLLBACK");successful=false;}
 String select(String table,String[] cols,String where,String order){return "SELECT "+(cols==null?"*":String.join(",",cols))+" FROM "+table+(where==null?"":" WHERE "+where)+(order==null?"":" ORDER BY "+order);}
 public Cursor query(String t,String[] cols,String w,String[] a,String g,String h,String o){try{PreparedStatement p=c.prepareStatement(select(t,cols,w,o));args(p,a);return new Cursor(p.executeQuery(),p);}catch(SQLException e){throw new RuntimeException(e);}}
 void args(PreparedStatement p,String[] a)throws SQLException{if(a!=null)for(int i=0;i<a.length;i++)p.setString(i+1,a[i]);}
 public long insertOrThrow(String t,String n,ContentValues v){return insert(t,v,false);} public long insertWithOnConflict(String t,String n,ContentValues v,int conflict){return insert(t,v,true);}
 long insert(String t,ContentValues v,boolean ignore){try{String keys=String.join(",",v.keySet()),marks=String.join(",",Collections.nCopies(v.size(),"?"));PreparedStatement p=c.prepareStatement("INSERT "+(ignore?"OR IGNORE ":"")+"INTO "+t+"("+keys+") VALUES("+marks+")",Statement.RETURN_GENERATED_KEYS);int i=1;for(Object x:v.values())p.setObject(i++,x);p.executeUpdate();ResultSet r=p.getGeneratedKeys();return r.next()?r.getLong(1):-1;}catch(SQLException e){throw new RuntimeException(e);}}
 public int update(String t,ContentValues v,String w,String[] a){try{StringJoiner j=new StringJoiner(",");v.keySet().forEach(k->j.add(k+"=?"));PreparedStatement p=c.prepareStatement("UPDATE "+t+" SET "+j+" WHERE "+w);int i=1;for(Object x:v.values())p.setObject(i++,x);if(a!=null)for(String x:a)p.setString(i++,x);return p.executeUpdate();}catch(SQLException e){throw new RuntimeException(e);}}
 public int delete(String t,String w,String[] a){try{PreparedStatement p=c.prepareStatement("DELETE FROM "+t+(w==null?"":" WHERE "+w));args(p,a);return p.executeUpdate();}catch(SQLException e){throw new RuntimeException(e);}}
}
