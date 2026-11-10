package cn.medreminder.app;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Exact sums of explicit actual-use log dose text; units are never converted. */
public final class DoseStats {
    private static final Pattern DOSE=Pattern.compile("^\\s*((?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+))\\s*([\\p{L}µμ]+)\\s*$");
    private static final Set<String> UNITS=new HashSet<>(Arrays.asList(
            "mg","g","kg","ug","mcg","μg","µg","mL","ml","L","l",
            "片","粒","颗","丸","包","袋","滴","支","瓶","枚","喷","揿","次"));
    private DoseStats() {}

    public static final class Amount { public final BigDecimal value; public final String unit; private Amount(BigDecimal v,String u){value=v;unit=u;} }
    public static final class Group {
        public final long medId; public String medName; public final String unit; public BigDecimal total=BigDecimal.ZERO; public int records;
        private Group(long id,String name,String u){medId=id;medName=name;unit=u;}
        public String totalText(){return total.stripTrailingZeros().toPlainString();}
    }
    public static final class Uncounted { public final long medId; public String medName; public int records; private Uncounted(long id,String n){medId=id;medName=n;} }
    public static final class Summary {
        public final List<Group> groups; public final List<Uncounted> uncounted;
        private Summary(List<Group> g,List<Uncounted> u){groups=g;uncounted=u;}
        public Group find(long medId,String unit){for(Group g:groups)if(g.medId==medId&&g.unit.equals(unit))return g;return null;}
        public List<Group> groupsFor(long medId){List<Group> out=new ArrayList<>();for(Group g:groups)if(g.medId==medId)out.add(g);return out;}
        public int uncountedFor(long medId){for(Uncounted u:uncounted)if(u.medId==medId)return u.records;return 0;}
    }

    public static Amount parseAmount(String text){
        if(text==null)return null;Matcher m=DOSE.matcher(text);if(!m.matches())return null;
        String unit=m.group(2);if(!UNITS.contains(unit))return null;
        try{BigDecimal amount=new BigDecimal(m.group(1));if(amount.signum()<=0)return null;return new Amount(amount,unit);}catch(NumberFormatException e){return null;}
    }
    public static Summary summarize(List<Store.Log> logs,long fromInclusive,long toExclusive){
        if(fromInclusive<0||toExclusive<=fromInclusive)throw new IllegalArgumentException("invalid actual-time range");
        TreeMap<String,Group> groups=new TreeMap<>();TreeMap<Long,Uncounted> uncounted=new TreeMap<>();
        if(logs!=null)for(Store.Log l:logs){if(l==null||l.actualAt<fromInclusive||l.actualAt>=toExclusive)continue;Amount amount=parseAmount(l.dose);
            if(amount==null){Uncounted u=uncounted.get(l.medId);if(u==null){u=new Uncounted(l.medId,l.medName);uncounted.put(l.medId,u);}u.records++;if(l.actualAt>0)u.medName=l.medName;continue;}
            String key=l.medId+"\u0000"+amount.unit;Group g=groups.get(key);if(g==null){g=new Group(l.medId,l.medName,amount.unit);groups.put(key,g);}if(l.actualAt>=0)g.medName=l.medName;g.total=g.total.add(amount.value);g.records++;
        }
        return new Summary(new ArrayList<>(groups.values()),new ArrayList<>(uncounted.values()));
    }
}
