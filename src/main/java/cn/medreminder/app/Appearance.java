package cn.medreminder.app;
/** Shared native and screen palette; preference is independent of prescriptions. */
public final class Appearance {
 private Appearance(){}
 public static boolean dark(String preference,boolean systemDark){return "dark".equals(preference)||(!"light".equals(preference)&&systemDark);}
 public static final class Palette {
  public final int bg,surface,ink,muted,accent,soft,border,danger,onAccent;
  Palette(int b,int s,int i,int m,int a,int f,int l,int d,int o){bg=b;surface=s;ink=i;muted=m;accent=a;soft=f;border=l;danger=d;onAccent=o;}
 }
 public static Palette palette(boolean dark){return dark?new Palette(0xff101b18,0xff1a2924,0xffe7eee9,0xffb3c4bb,0xff91d3ba,0xff203f33,0xff354b40,0xfff2a59d,0xff103426):new Palette(0xfff5f6f2,0xffffffff,0xff21332f,0xff596b65,0xff216c5c,0xffe6f1ec,0xffdee6e1,0xffa3443f,0xffffffff);}
 private static double luminance(int c){double[] a={((c>>16)&255)/255.0,((c>>8)&255)/255.0,(c&255)/255.0};for(int i=0;i<3;i++)a[i]=a[i]<=0.04045?a[i]/12.92:Math.pow((a[i]+.055)/1.055,2.4);return a[0]*.2126+a[1]*.7152+a[2]*.0722;}
 public static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
}
