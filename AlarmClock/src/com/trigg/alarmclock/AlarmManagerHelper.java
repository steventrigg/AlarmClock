package com.trigg.alarmclock;

import java.util.Calendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmManagerHelper extends BroadcastReceiver {

	public static final String ID = "id";
	public static final String NAME = "name";
	public static final String TIME_HOUR = "timeHour";
	public static final String TIME_MINUTE = "timeMinute";
	public static final String TONE = "alarmTone";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		setAlarms(context);
	}
	
	public static void setAlarms(Context context) {
		cancelAlarms(context);
		
		AlarmDBHelper dbHelper = new AlarmDBHelper(context);

		List<AlarmModel> alarms =  dbHelper.getAlarms();
		if(alarms!=null)
		{
		for (AlarmModel alarm : alarms) {
			if (alarm.isEnabled) {

				PendingIntent pIntent = createPendingIntent(context, alarm);

				Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.HOUR_OF_DAY, alarm.timeHour);
				calendar.set(Calendar.MINUTE, alarm.timeMinute);
				calendar.set(Calendar.SECOND, 00);

				//Find next time to set
				final int nowDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
				final int nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				final int nowMinute = Calendar.getInstance().get(Calendar.MINUTE);
				boolean alarmSet = false;
				
				//First check if it's later in the week
				for (int dayOfWeek = Calendar.SUNDAY; dayOfWeek <= Calendar.SATURDAY; ++dayOfWeek) {
					if (alarm.getRepeatingDay(dayOfWeek - 1) && dayOfWeek >= nowDay &&
							!(dayOfWeek == nowDay && alarm.timeHour < nowHour) &&
							!(dayOfWeek == nowDay && alarm.timeHour == nowHour && alarm.timeMinute <= nowMinute)) {
						calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
						
						setAlarm(context, calendar, pIntent);
						alarmSet = true;
						break;
					}
				}
				
				//Else check if it's earlier in the week
				if (!alarmSet) {
					for (int dayOfWeek = Calendar.SUNDAY; dayOfWeek <= Calendar.SATURDAY; ++dayOfWeek) {
						if (alarm.getRepeatingDay(dayOfWeek - 1) && dayOfWeek <= nowDay && alarm.repeatWeekly) {
							calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
							calendar.add(Calendar.WEEK_OF_YEAR, 1);
							
							setAlarm(context, calendar, pIntent);
							alarmSet = true;
							break;
						}
					}
				}
			}
		}
		
	        }
	}
	
	@SuppressLint("NewApi")
	private static void setAlarm(Context context, Calendar calendar, PendingIntent pIntent) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pIntent);
		} else {
			alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pIntent);
		}
	}
	
	public static void cancelAlarms(Context context) {
		AlarmDBHelper dbHelper = new AlarmDBHelper(context);
		
		List<AlarmModel> alarms =  dbHelper.getAlarms();
		
 		if (alarms != null) {
			for (AlarmModel alarm : alarms) {
				if (alarm.isEnabled) {
					PendingIntent pIntent = createPendingIntent(context, alarm);
	
					AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
					alarmManager.cancel(pIntent);
				}
			}
 		}
	}

	private static PendingIntent createPendingIntent(Context context, AlarmModel model) {
		Intent intent = new Intent(context, AlarmService.class);
		intent.putExtra(ID, model.id);
		intent.putExtra(NAME, model.name);
		intent.putExtra(TIME_HOUR, model.timeHour);
		intent.putExtra(TIME_MINUTE, model.timeMinute);
		intent.putExtra(TONE, model.alarmTone.toString());
		
		return PendingIntent.getService(context, (int) model.id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
}
