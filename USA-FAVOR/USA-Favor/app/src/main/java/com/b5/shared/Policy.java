package com.b5.shared;
import java.util.Locale;
public final class Policy {
 public static final long DAY=86400000L;
 public static long duration(String d){switch(d){case "1H":return 3600000L;case "1D":return DAY;case "7D":return 7*DAY;case "30D":return 30*DAY;case "LIFETIME":return 0;default:throw new IllegalArgumentException("Unknown duration");}}
 public static boolean validCode(String c){return c!=null&&c.matches("USA-[A-Z0-9]{4}");}
 public static boolean expired(long expiry,long now){return expiry>0&&expiry<=now;}
 public static String remaining(long expiry,long now){if(expiry==0)return "LIFETIME";long s=Math.max(0,(expiry-now)/1000);return String.format(Locale.US,"%d Days %02d Hours %02d Min %02d Sec",s/86400,(s/3600)%24,(s/60)%60,s%60);}
 public static long extended(long expiry,long now){return Math.max(expiry,now)+7*DAY;}
}
