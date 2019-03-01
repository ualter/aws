package com.amazonaws.sandbox.s3;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

public class SignerTest {
	
	public static void main(String[] args) {
		/*try {
			
			String key = "LrOQgJMDAd454H1GIsRGxCL+h2Qh+XSmylTNfu4k";
			
			String dateKey              = encrypt("AWS4" + key,"20181214");
			String dateRegionKey        = encrypt(dateKey,"us-east-1");
			String dateRegionServiceKey = encrypt(dateRegionKey,"s3");
			String signingKey           = encrypt(dateRegionServiceKey,"aws4_request");
			
			
			System.out.println(Hex.encodeHexString(signingKey.getBytes()));
			
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
			e.printStackTrace();
		}*/
	}

	private static String encrypt(String key, String data) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		
		SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
		sha256_HMAC.init(secret_key);
		
		return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
	}

}
