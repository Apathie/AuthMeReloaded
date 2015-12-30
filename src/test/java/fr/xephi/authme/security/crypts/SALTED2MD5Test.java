package fr.xephi.authme.security.crypts;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.WrapperMock;
import org.junit.Before;

/**
 * Test for {@link SALTED2MD5}.
 */
public class SALTED2MD5Test extends AbstractEncryptionMethodTest {

    @Before
    public void setUpAlgorithm() {
        WrapperMock.createInstance();
        Settings.saltLength = 8;
    }

    public SALTED2MD5Test() {
        super(new SALTED2MD5(),
            new EncryptedPassword("9f3d13dc01a6fe61fd669954174399f3", "9b5f5749"),  // password
            new EncryptedPassword("b28c32f624a4eb161d6adc9acb5bfc5b", "f750ba32"),  // PassWord1
            new EncryptedPassword("38dcb83cc68424afe3cda012700c2bb1", "eb2c3394"),  // &^%te$t?Pw@_
            new EncryptedPassword("ad25606eae5b760c8a2469d65578ac39", "04eee598")); // âË_3(íù*)
    }

}