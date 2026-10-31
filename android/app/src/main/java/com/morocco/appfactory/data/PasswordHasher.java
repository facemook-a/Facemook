package com.morocco.appfactory.data;

import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private PasswordHasher() {}
    public static String hash(char[] password) throws Exception {
        byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
        return hex(salt)+":"+hex(derive(password,salt));
    }
    public static boolean verify(char[] password,String stored) throws Exception {
        String[] parts=stored.split(":");
        return parts.length==2 && MessageDigest.isEqual(derive(password,unhex(parts[0])),unhex(parts[1]));
    }
    private static byte[] derive(char[] password,byte[] salt) throws Exception {
        PBEKeySpec spec=new PBEKeySpec(password,salt,120000,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
    private static String hex(byte[] bytes) {
        char[] alphabet="0123456789abcdef".toCharArray(),result=new char[bytes.length*2];
        for(int i=0;i<bytes.length;i++) { result[i*2]=alphabet[(bytes[i]&255)>>>4]; result[i*2+1]=alphabet[bytes[i]&15]; }
        return new String(result);
    }
    private static byte[] unhex(String value) {
        if(value.length()%2!=0) throw new IllegalArgumentException("Invalid stored password");
        byte[] out=new byte[value.length()/2];
        for(int i=0;i<out.length;i++) {
            int a=Character.digit(value.charAt(i*2),16),b=Character.digit(value.charAt(i*2+1),16);
            if(a<0||b<0) throw new IllegalArgumentException("Invalid stored password");out[i]=(byte)((a<<4)|b);
        }
        return out;
    }
}
