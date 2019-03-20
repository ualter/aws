package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;

/**
 * 
 * @author Ualter Jr.
 *
 */
public class S3LowLevelProgressListener implements ProgressListener {
	
	private long totalBytesTransfered;
	private long showMessage;
	private long totalFileSizeInBytes;
	
	private static   DecimalFormat DF   = new DecimalFormat("###,###,###");
	private static   DecimalFormat PERC = new DecimalFormat("#00");
	private int      blockSize          = 10000;
	private boolean  firstShown         = false;
	private long     startTime          = System.currentTimeMillis();
	private long     lastPrint          = 0; 
	private long     intervalPrinting   = 299;
	//private String[] charsLoadingBar    = {"░","▒"};
	private String[] charsLoadingBar    = {"░","█"};
	private int      seqLoadingBar      = -1;
	
	@Override
	public void progressChanged(ProgressEvent progressEvent) {
		
		if ( !firstShown) {
			//System.out.print("•");
			firstShown = true;
		}
		
		totalBytesTransfered += progressEvent.getBytesTransferred();
		showMessage          += (progressEvent.getBytesTransferred() / 1024);
        int percentual        = Math.round(((float)this.totalBytesTransfered / (float)this.totalFileSizeInBytes) * 100);        
        long   timeSpent      = System.currentTimeMillis() - this.startTime;
        
		if ( showMessage > blockSize ) {
			String timeFormatted = this.formatMillis(timeSpent);
			System.out.println("\n" + timeFormatted + " --> " + PERC.format(percentual)  + "% --> " + DF.format((totalBytesTransfered / 1024)) + " bytes transfered");
			showMessage = 0;
		} else 
		if ( (System.currentTimeMillis() - lastPrint) > intervalPrinting ) {
			System.out.print(this.charsLoadingBar[++seqLoadingBar]);
			lastPrint = System.currentTimeMillis();
			if ( seqLoadingBar == (charsLoadingBar.length - 1) ) {
				seqLoadingBar = -1;
			}
		}
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
