package com.amazonaws.sandbox.s3.multipart.lowlevel;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.sandbox.s3.multipart.lowlevel.S3ClientConnection;
import com.amazonaws.services.s3.AmazonS3;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ArtifactoryTransferManager {
	
	public static void main(String[] args) {
		
		String origin = "NONE";
		
		int cur_arg = 0;
		while (args[cur_arg].startsWith("--")) {
            if (args[cur_arg].equals("--test")) {
            	origin = "/var/ecommerce/artifactory_home/data/filestore";
            } else if (args[cur_arg].equals("--artifactory")) {
            	origin     = "/var/ecommerce/artifactory_home/data/filestore";
            } else {
                System.out.println("Unknown argument: " + args[cur_arg]);
                System.exit(1);
            }
            cur_arg += 1;
        }
		
		if (origin.equals("NONE")) {
			 System.out.println("Origin=" + origin + "  INVALID!";);
             System.exit(1)
		}
		
		String bucketName = "emagin-delivery/general/artifactory";
		String keyPrefix  = "";
		//String origin     = "/data/dev/uaza/.m2/";
		boolean recursive = true;
		
		AmazonS3 s3 = S3ClientConnection.S3Client("ecomm");
		
		TransferManager xfer_mgr = TransferManagerBuilder
				.standard()
				.withS3Client(s3)
				.build();
		
		try {
		    MultipleFileUpload xfer = xfer_mgr.uploadDirectory(bucketName,
		    		keyPrefix, new File(origin), recursive);
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
