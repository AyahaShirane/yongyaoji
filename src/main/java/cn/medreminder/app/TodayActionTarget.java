package cn.medreminder.app;

/** Immutable confirmation identity captured by the user's button tap. */
public final class TodayActionTarget {
    public final long medId,revision,plannedAt;
    public final String occurrence,medName,plannedDose;
    public TodayActionTarget(Store.Med med,long plannedAt,String occurrence){
        if(med==null||med.id<=0||med.revision<=0||plannedAt<=0||!Core.occurrence(med.id,med.revision,plannedAt).equals(occurrence))throw new IllegalArgumentException("Invalid planned occurrence");
        this.medId=med.id;this.revision=med.revision;this.plannedAt=plannedAt;this.occurrence=occurrence;this.medName=med.name;this.plannedDose=med.dose;
    }
}
