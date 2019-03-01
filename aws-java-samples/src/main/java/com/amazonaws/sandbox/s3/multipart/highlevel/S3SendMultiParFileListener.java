package com.amazonaws.sandbox.s3.multipart.highlevel;

import java.io.File;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;

public class S3SendMultiParFileListener implements ProgressListener {
	
	private File file;
	private String target;

	public S3SendMultiParFileListener(File file, String target) {
		super();
		this.file = file;
		this.target = target;
	}

	@Override
	public void progressChanged(ProgressEvent progressEvent) {
		if ( progressEvent.getEventType() == ProgressEventType.TRANSFER_STARTED_EVENT ) {
			this.out("Started to upload: " + file.getAbsolutePath() + " -> "+ this.target);
		} else
		if ( progressEvent.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT ) {
			this.out("Completed upload: " + file.getAbsolutePath() + " -> "+ this.target);
		} else
		if ( progressEvent.getEventType() == ProgressEventType.TRANSFER_FAILED_EVENT ) {
			this.out("Failed to upload: " + file.getAbsolutePath() + " -> "+ this.target);
		}
		//this.out("Transferred bytes: " + progressEvent.getBytesTransferred());
	}
	
	private void out(String msg) {
		System.out.println(msg);
	}
	

}
