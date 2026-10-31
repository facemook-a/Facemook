package com.morocco.appfactory;

import com.morocco.appfactory.data.PasswordHasher;
import org.junit.Test;
import static org.junit.Assert.*;

public class PasswordHasherTest {
    @Test public void passwordHashIsSaltedAndVerifiable()throws Exception{
        char[] password="Atlas!Password1".toCharArray();String a=PasswordHasher.hash(password),b=PasswordHasher.hash(password);
        assertNotEquals(a,b);assertFalse(a.contains("Password"));assertTrue(PasswordHasher.verify(password,a));assertFalse(PasswordHasher.verify("wrong".toCharArray(),a));
    }
}
