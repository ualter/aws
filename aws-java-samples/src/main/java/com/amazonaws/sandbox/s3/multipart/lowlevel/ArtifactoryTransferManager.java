package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

public class ArtifactoryTransferManager {
	
	public static void main(String[] args) {
		
		String originFolder = "NONE";
		String bucketDestiny = "NONE";
		String origin = "/var/ecommerce/artifactory_home/data/filestore/";
		String bucketName = "emagin-delivery/general/artifactory/";
		String keyPrefix  = "";
		
		if (args.length < 1) {
			System.out.println("Usage (in the order):");
			System.out.println("  ArtifactoryTransferManager --folder [FOLDER_NAME]");
			System.out.println("  ArtifactoryTransferManager --foldersfromfile");
			System.exit(0);
		} else
		if ( args[0].equalsIgnoreCase("--folder") ){
			originFolder  = args[1];
			bucketDestiny = args[1];
			origin = "/var/ecommerce/artifactory_home/data/filestore/" + originFolder;
			bucketName = "emagin-delivery/general/artifactory/" + bucketDestiny;
			
			copyFolderRecursively(origin, bucketName, keyPrefix);
			
		} else
		if ( args[0].equalsIgnoreCase("--foldersfromfile") ){
			String fileName = "/data/dev/uaza/aws/aws-java-samples/src/main/resources/"
					+ "folders_to_copy2.txt";
			Stream<String> stream;
			String currentFolder = "";
			try {
				stream = Files.lines(Paths.get(fileName));
				List<String> foldersToCopy = stream.collect(Collectors.toList());
				
				for(String folder : foldersToCopy) {
					currentFolder = folder;
					origin        = "/var/ecommerce/artifactory_home/data/filestore/" + folder;
					bucketName    = "emagin-delivery/general/artifactory/" + folder;
					
					try {
						
						if ( folder.equalsIgnoreCase("08") ) {
							throw new RuntimeException("Deu merda!");
						}
						System.out.println(origin + " to " +  bucketName);
						//copyFolderRecursively(origin, bucketName, keyPrefix);
						
					} catch (Throwable e) {
						try {
							String fError = "/data/dev/uaza/aws/aws-java-samples/" + currentFolder + "_Error.txt";
							System.out.print("\033[0;33m");
							System.out.println("Houston! For the folder " +  currentFolder + ", was thrown the error: \\033[1;34m" +  e.getMessage() + "\\033[0;33m, check file:" + fError);
							System.out.print("\033[0m");
							FileWriter fw = new FileWriter(fError);
							fw.write(ExceptionUtils.getStackTrace(e));
							fw.flush();
							fw.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				
			} catch (Throwable e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			
			System.exit(0);
			
		} 
		
		System.out.println("Sorry! You did not say anything, what exactly can I do for you? \n");
		System.out.println("Usage (in the order):");
		System.out.println("  ArtifactoryTransferManager --folder 00");
		System.out.println("");
		
	}

	private static void copyFolderRecursively(String origin, String bucketName, String keyPrefix) {
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
