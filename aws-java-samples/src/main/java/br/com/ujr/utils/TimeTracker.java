package br.com.ujr.utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

public class TimeTracker {
	
	private Map<String,TimeTrack> times     = new HashMap<String,TimeTrack>();
	private Map<String,Integer>   sequences = new HashMap<String,Integer>();
	
	private int sequence;
	
	private static TimeTracker me = new TimeTracker();

	private TimeTracker() {
	}
	
	public static TimeTracker getInstance() {
		return me;
	}
	
	
	public String startTracking(String description) {
		sequences.put(description,++sequence);
		times.put(description, new TimeTrack());
		return String.format("%s @@@ START %s--> %s ###"
				,df.format(sequences.get(description))
				,StringUtils.rightPad(description + " ", 80,"-")
				,times.get(description).getTimeStartFormatted());
	}
	
	private String descriptionTmpEnd;
	
	public String endTracking(String description) {
		return this.endTrackingTimeTracker(description).formattedTimeLabel();
	}
	
	public TimeTracker endTrackingTimeTracker(String description) {
		if ( !times.containsKey(description)) {
			throw new RuntimeException(description + " Not found");
		}
		times.get(description).stop();
		this.descriptionTmpEnd = description;
		return this;
	}
	
	public String formattedTime() {
		return times.get(this.descriptionTmpEnd).getTimeSpenFormatted();
	}
	
	
	private DecimalFormat df = new DecimalFormat("###,#00");
	
	public String formattedTimeLabel() {
		return String.format("%s ### END   %s--> %s ### (Total Spent..: %s)"
															  ,df.format(sequences.get(this.descriptionTmpEnd))
				 											  ,StringUtils.rightPad(this.descriptionTmpEnd + " ", 80,"-")
				                                              ,times.get(this.descriptionTmpEnd).getTimeEndFormatted() 
				                                              ,times.get(this.descriptionTmpEnd).getTimeSpenFormatted());
	}
	
	public static class TimeTrack {
		private String description;
		private long startTime;
		private long endTime;
		private long timeSpent;
		private String timeSpenFormatted;
		
		public TimeTrack() {
			this.startTime = System.currentTimeMillis();
		}
		public void stop() {
			this.endTime = System.currentTimeMillis();
			this.timeSpent = this.endTime - this.startTime;
			this.timeSpenFormatted = this.formatMillis(this.timeSpent);
		}
		
		private String formatMillis(long timeSpent) {
			String resultTime = String.format("%02d:%02d:%02d:%04d", 
				TimeUnit.MILLISECONDS.toHours(timeSpent),
				
				TimeUnit.MILLISECONDS.toMinutes(timeSpent) -  
				TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeSpent)),
				
				TimeUnit.MILLISECONDS.toSeconds(timeSpent) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeSpent)),
				
				TimeUnit.MILLISECONDS.toMillis(timeSpent) -
				TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(timeSpent))
			);
			return resultTime;
		}
		
		private static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		public String formatMillisAtDateTime(long milis ) {
			return SDF.format(new Date(milis));
		}
		public String getTimeStartFormatted() {
			return this.formatMillisAtDateTime(this.startTime);
		}
		public String getTimeEndFormatted() {
			return this.formatMillisAtDateTime(this.endTime);
		}
		public String getTimeSpenFormatted() {
			return timeSpenFormatted;
		}
		public void setTimeSpenFormatted(String timeSpenFormatted) {
			this.timeSpenFormatted = timeSpenFormatted;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public long getStartTime() {
			return startTime;
		}
		public void setStartTime(long startTime) {
			this.startTime = startTime;
		}
		public long getEndTime() {
			return endTime;
		}
		public void setEndTime(long endTime) {
			this.endTime = endTime;
		}
		public long getTimeSpent() {
			return timeSpent;
		}
		public void setTimeSpent(long timeSpent) {
			this.timeSpent = timeSpent;
		}
		
		
		
	}

}