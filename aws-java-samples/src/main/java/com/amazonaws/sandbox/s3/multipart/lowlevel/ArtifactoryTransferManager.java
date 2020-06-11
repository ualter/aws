package com.amazonaws.sandbox.s3.multipart.lowlevel;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ArtifactoryTransferManager {
	
	public static void main(String[] args) {
		String bucketName = "emagin-delivery/general/artifactory";
		String keyPrefix  = "";
		String origin     = "/data/dev/uaza/.m2/";
		boolean recursive = true;
		
		TransferManager xfer_mgr = TransferManagerBuilder.standard().build();
		try {
		    MultipleFileUpload xfer = xfer_mgr.uploadDirectory(bucketName,
		            key_prefix, new File(origin), recursive);
		    // loop with Transfer.isDone()
		    XferMgrProgress.showTransferProgress(xfer);
		    // or block with Transfer.waitForCompletion()
		    XferMgrProgress.waitForCompletion(xfer);
		} catch (AmazonServiceException e) {
		    System.err.println(e.getErrorMessage());
		    System.exit(1);
		}
		xfer_mgr.shutdownNow();
	}

}
