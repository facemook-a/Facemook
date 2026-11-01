package com.b5.vpn;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;import de.blinkt.openvpn.*;import de.blinkt.openvpn.core.*;import java.io.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ProfileTest {
 @Test public void uploadedTcpAndRequestedUdpProfilesParseWithCertificates()throws Exception{for(String name:new String[]{"US-NewYork.ovpn","US-NewYork-UDP.ovpn"}){ConfigParser cp=new ConfigParser();try(Reader reader=new InputStreamReader(RuntimeEnvironment.getApplication().getAssets().open(name))){cp.parseConfig(reader);}VpnProfile p=cp.convertProfile();assertEquals("vpn318641113.opengw.net",p.mConnections[0].mServerName);assertEquals(name.contains("UDP")?"1195":"1923",p.mConnections[0].mServerPort);assertEquals(name.contains("UDP"),p.mConnections[0].mUseUdp);assertTrue(p.mCaFilename.contains("BEGIN CERTIFICATE"));assertTrue(p.mClientCertFilename.contains("BEGIN CERTIFICATE"));assertTrue(p.mClientKeyFilename.contains("PRIVATE KEY"));}}
}
