import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Original capsule-clock artwork. All vector and raster assets use this geometry. */
public final class IconGenerator {
  static final Color TEAL=new Color(0x216C5C), IVORY=new Color(0xFFFCF4), MINT=new Color(0xB5D6C6);
  static Path2D capsule(double x,double y,double scale) {
    Path2D p=new Path2D.Double(); double L=31*scale, r=11*scale;
    p.moveTo(-L+r,-r); p.lineTo(L-r,-r); p.quadTo(L,-r,L,r); p.lineTo(-L+r,r); p.quadTo(-L,r,-L,-r); p.closePath();
    AffineTransform t=new AffineTransform(); t.translate(x,y); t.rotate(Math.PI/4); return new Path2D.Double(t.createTransformedShape(p));
  }
  static Path2D clock(double x,double y,double s) {
    Path2D p=new Path2D.Double(); p.append(new Ellipse2D.Double(x-9*s,y-9*s,18*s,18*s),false); return p;
  }
  static void draw(Graphics2D g,int size,boolean transparent) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
    double s=size/108.0; if(!transparent){g.setColor(TEAL);g.fillRect(0,0,size,size);} g.scale(s,s);
    g.setColor(IVORY);g.fill(capsule(54,54,1));
    g.setColor(MINT);g.fill(clock(54,54,1));g.setColor(TEAL);g.setStroke(new BasicStroke(1.9f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(clock(54,54,1));
    g.setStroke(new BasicStroke(2.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(new Line2D.Double(54,54,54,48.5));g.draw(new Line2D.Double(54,54,58.2,56.5));
  }
  static BufferedImage image(int size,boolean transparent){BufferedImage b=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);Graphics2D g=b.createGraphics();draw(g,size,transparent);g.dispose();return b;}
  static String pathData(Shape shape){StringBuilder b=new StringBuilder();PathIterator it=shape.getPathIterator(null,0.3);double[] a=new double[6];while(!it.isDone()){int k=it.currentSegment(a);switch(k){case PathIterator.SEG_MOVETO:b.append("M ").append(f(a[0])).append(',').append(f(a[1])).append(' ');break;case PathIterator.SEG_LINETO:b.append("L ").append(f(a[0])).append(',').append(f(a[1])).append(' ');break;case PathIterator.SEG_QUADTO:b.append("Q ").append(f(a[0])).append(',').append(f(a[1])).append(' ').append(f(a[2])).append(',').append(f(a[3])).append(' ');break;case PathIterator.SEG_CUBICTO:b.append("C ").append(f(a[0])).append(',').append(f(a[1])).append(' ').append(f(a[2])).append(',').append(f(a[3])).append(' ').append(f(a[4])).append(',').append(f(a[5])).append(' ');break;case PathIterator.SEG_CLOSE:b.append("Z ");}it.next();}return b.toString().trim();}
  static String f(double n){return String.format(java.util.Locale.US,"%.2f",n);}
  static void write(Path p,String s)throws Exception{Files.createDirectories(p.getParent());Files.write(p,s.getBytes("UTF-8"));}
  static void png(Path p,BufferedImage b)throws Exception{Files.createDirectories(p.getParent());ImageIO.write(b,"png",p.toFile());}
  static String vector(String paths){return "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"108dp\" android:height=\"108dp\" android:viewportWidth=\"108\" android:viewportHeight=\"108\">"+paths+"</vector>";}
  static String foreground(){return vector("<path android:fillColor=\"#FFFCF4\" android:pathData=\""+pathData(capsule(54,54,1))+"\"/><path android:fillColor=\"#B5D6C6\" android:strokeColor=\"#216C5C\" android:strokeWidth=\"1.9\" android:pathData=\""+pathData(clock(54,54,1))+"\"/><path android:fillColor=\"#00000000\" android:strokeColor=\"#216C5C\" android:strokeWidth=\"2.2\" android:strokeLineCap=\"round\" android:pathData=\"M 54,54 L 54,48.5 M 54,54 L 58.2,56.5\"/>");}
  static String monochrome(){return vector("<path android:fillColor=\"#FFFFFFFF\" android:pathData=\""+pathData(capsule(54,54,1))+"\"/><path android:fillColor=\"#00000000\" android:strokeColor=\"#FFFFFFFF\" android:strokeWidth=\"2\" android:pathData=\""+pathData(clock(54,54,1))+"\"/><path android:fillColor=\"#00000000\" android:strokeColor=\"#FFFFFFFF\" android:strokeWidth=\"2.2\" android:strokeLineCap=\"round\" android:pathData=\"M 54,54 L 54,48.5 M 54,54 L 58.2,56.5\"/>");}
  static Shape notifShape(Shape s){AffineTransform t=new AffineTransform();t.translate(-0.0,-0.0);t.scale(2.0/9,2.0/9);return t.createTransformedShape(s);}
  static String notification(){return "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"24dp\" android:height=\"24dp\" android:viewportWidth=\"24\" android:viewportHeight=\"24\"><path android:fillColor=\"#FFFFFFFF\" android:pathData=\""+pathData(notifShape(capsule(54,54,0.72)))+"\"/><path android:fillColor=\"#00000000\" android:strokeColor=\"#FFFFFFFF\" android:strokeWidth=\"0.67\" android:strokeLineCap=\"round\" android:pathData=\"M 12,12 L 12,10.78 M 12,12 L 12.93,12.56\"/></vector>";}
  static void rounded(Graphics2D g,int x,int y,int s,int radius,boolean circle){Shape clip=circle?new Ellipse2D.Double(x,y,s,s):new RoundRectangle2D.Double(x,y,s,s,radius,radius);g.setColor(TEAL);g.fill(clip);Graphics2D q=(Graphics2D)g.create();q.clip(clip);q.translate(x,y);draw(q,s,false);q.dispose();g.setColor(new Color(0xD8D5CC));g.setStroke(new BasicStroke(1));g.draw(clip);}
  public static void main(String[] a)throws Exception {Path res=Paths.get(a[0]),art=Paths.get(a[1]);write(res.resolve("drawable/ic_launcher_foreground.xml"),foreground());write(res.resolve("drawable/ic_launcher_monochrome.xml"),monochrome());write(res.resolve("drawable/ic_stat_notification.xml"),notification());String adaptive="<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\"><background android:drawable=\"@color/icon_background\"/><foreground android:drawable=\"@drawable/ic_launcher_foreground\"/><monochrome android:drawable=\"@drawable/ic_launcher_monochrome\"/></adaptive-icon>";write(res.resolve("mipmap-anydpi-v26/ic_launcher.xml"),adaptive);write(res.resolve("mipmap-anydpi-v26/ic_launcher_round.xml"),adaptive);write(res.resolve("values/colors.xml"),"<resources><color name=\"icon_background\">#216C5C</color></resources>");
   png(art.resolve("branding-icon-512.png"),image(512,false));int[] ds={48,72,96,144,192};String[] names={"mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi"};for(int i=0;i<ds.length;i++){Path d=res.resolve("mipmap-"+names[i]);png(d.resolve("ic_launcher.png"),image(ds[i],false));png(d.resolve("ic_launcher_round.png"),image(ds[i],false));}
   int W=760,H=400;BufferedImage sheet=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(0xFFFCF4));g.fillRect(0,0,W,H);g.setColor(new Color(0x253530));g.setFont(new Font("SansSerif",Font.BOLD,20));g.drawString("Launcher icon · mask preview",26,30);int[] sizes={48,72};int[] xs={305,455};for(int row=0;row<2;row++){int s=sizes[row], y=64+row*160;g.setFont(new Font("SansSerif",Font.BOLD,15));g.drawString(s+" px",210,y+s/2+5);for(int col=0;col<2;col++){int x=xs[col]-s/2;rounded(g,x,y,s,col==0?s/2:14*s/48,col==0);g.setColor(new Color(0x253530));g.setFont(new Font("SansSerif",Font.PLAIN,13));String label=col==0?"Circle":"Squircle";g.drawString(label,(float)(xs[col]-g.getFontMetrics().stringWidth(label)/2.0),y+s+22);}}g.dispose();png(art.resolve("branding-mask-preview.png"),sheet);
  }
}
