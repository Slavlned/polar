package com.slavlend.Compiler.Libs;
import java.util.Base64;

/*
Библиотека для работы с BASE64
(by Sliva)
 */
public class PolarBase64 {
	/*
    BASE64
     */

	public byte[] encode (byte[] in) {
		return Base64.getEncoder().encode(in);
	}
	public byte[] decode (byte[] in) {
		return Base64.getDecoder().decode(in);
	}
	public byte[] bytes (String in) {
		return in.getBytes();
	}
	public String str (byte[] in) {
		return new String(in);
	}
}
