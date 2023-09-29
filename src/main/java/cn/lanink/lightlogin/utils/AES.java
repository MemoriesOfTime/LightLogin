package cn.lanink.lightlogin.utils;

import cn.lanink.lightlogin.LightLogin;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author LT_Name
 */
public class AES {

    private final String key;

    public AES() {
        this(Utils.getRandomString(16));
    }

    public AES(String key) {
        if (key == null) {
            key = Utils.getRandomString(16);
        }else if (key.length() != 16) {
            key = Utils.getMd5(key).substring(0, 16);
        }
        this.key = key;
    }

    public String encrypt(String str) {
        byte[] crypted = null;
        try {
            SecretKeySpec skey = new SecretKeySpec(this.key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skey);

            crypted = cipher.doFinal(str.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LightLogin.getInstance().getLogger().debug("AES#encrypt() Error", e);
        }
        return Base64.getUrlEncoder().encodeToString(crypted);
    }

    public String decrypt(String input) {
        if (input == null || this.key == null) {
            return null;
        }
        try {
            SecretKeySpec skey = new SecretKeySpec(this.key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skey);
            return new String(cipher.doFinal(Base64.getUrlDecoder().decode(input)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LightLogin.getInstance().getLogger().debug("AES#decrypt() Error", e);
        }
        return null;
    }

}
