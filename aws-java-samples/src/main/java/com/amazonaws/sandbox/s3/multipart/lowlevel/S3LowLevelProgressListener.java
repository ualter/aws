package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.text.DecimalFormat;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;

public class S3LowLevelProgressListener implements ProgressListener {
	
	private long totalBytesTransfered;
	private long showMessage;
	
	private static DecimalFormat DF = new DecimalFormat("###,###,#00");

	@Override
	public void progressChanged(ProgressEvent progressEvent) {
		
		totalBytesTransfered += progressEvent.getBytesTransferred();
		showMessage          += (progressEvent.getBytesTransferred() / 1024); 
		
		if ( showMessage > 10000 ) {
			System.out.println("==========> Total Bytes Transfered..: " + DF.format((totalBytesTransfered / 1024)) );
			showMessage = 0;
		} else
		if ( showMessage >= 5000 ) {
			System.out.print("=");
		}
	}
	
	public long getTotalBytesTransfered() {
		return totalBytesTransfered;
	}

	public void setTotalBytesTransfered(long totalBytesTransfered) {
		this.totalBytesTransfered = totalBytesTransfered;
	}

}
