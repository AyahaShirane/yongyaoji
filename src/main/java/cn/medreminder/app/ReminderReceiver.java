package cn.medreminder.app;
import android.content.BroadcastReceiver;import android.content.Context;import android.content.Intent;
public final class ReminderReceiver extends BroadcastReceiver { public void onReceive(Context c,Intent i){if(AlarmSelfTest.ACTION.equals(i.getAction())){AlarmSelfTest.received(c,i);return;}long id=i.getLongExtra(Alarms.MED_ID,-1),rev=i.getLongExtra(Alarms.REVISION,-1),at=i.getLongExtra(Alarms.PLANNED_AT,-1);String key=i.getStringExtra(Alarms.OCCURRENCE);if(id<=0||rev<=0||at<=0||key==null)return;Alarms.delivered(c,id,rev,key,at);} }
