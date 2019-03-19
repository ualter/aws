package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.text.DecimalFormat;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;

public class S3LowLevelProgressListener implements ProgressListener {
	
	private long totalBytesTransfered;
	private long showMessage;
	private long totalFileSizeInBytes;
	
	private static  DecimalFormat DF = new DecimalFormat("###,###,###");
	private static  DecimalFormat PERC = new DecimalFormat("#00");
	private int     blockSize = 10000;
	private float   level = 0.3f;
	private float   count = 0;
	private boolean firstShown = false;
	
	@Override
	public void progressChanged(ProgressEvent progressEvent) {
		
		if ( !firstShown) {
			//System.out.print("•");
			firstShown = true;
		}
		
		totalBytesTransfered += progressEvent.getBytesTransferred();
		showMessage          += (progressEvent.getBytesTransferred() / 1024);
        int percentual        = Math.round(((float)this.totalBytesTransfered / (float)this.totalFileSizeInBytes) * 100);
        
        
		if ( showMessage > blockSize ) {
			System.out.println("> Total Bytes Transfered..: " + PERC.format(percentual)  + "% (" + DF.format((totalBytesTransfered / 1024)) + " bytes)");
			showMessage = 0;
		}
		
		if ( percentual >= count ) {
			System.out.print("•");
			count += level;
		}
	}
	
	public int getBlockSize() {
		return blockSize;
	}



	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}



	public long getTotalFileSizeInBytes() {
		return totalFileSizeInBytes;
	}

	public void setTotalFileSizeInBytes(long totalFileSizeInBytes) {
		this.totalFileSizeInBytes = totalFileSizeInBytes;
	}



	public long getTotalBytesTransfered() {
		return totalBytesTransfered;
	}

	public void setTotalBytesTransfered(long totalBytesTransfered) {
		this.totalBytesTransfered = totalBytesTransfered;
	}

}
