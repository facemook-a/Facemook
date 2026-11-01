package de.blinkt.openvpn.core;
/** B5 integration: route all engine notifications/reconnects to entitlement-aware service. */
public final class VpnServiceTarget {public static Class<?> type(){try{return Class.forName("com.b5.vpn.B5VpnService");}catch(ClassNotFoundException e){throw new IllegalStateException(e);}}}
