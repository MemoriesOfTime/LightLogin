package cn.lanink.lightlogin.utils;

import cn.lanink.lightlogin.LightLogin;
import cn.nukkit.Player;
import cn.nukkit.utils.LoginChainData;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author LT_Name
 */
public class Utils {

    private Utils() {
        throw new RuntimeException();
    }

    public static String getClientIdentification(@NotNull Player player) {
        StringBuilder builder = new StringBuilder("LightLogin:");
        LoginChainData loginChainData = player.getLoginChainData();
        builder.append(player.getAddress())
                .append(loginChainData.getDeviceId())
                .append(loginChainData.getDeviceModel())
                .append(loginChainData.getDeviceOS())
                .append(loginChainData.getClientId())
                .append(loginChainData.getClientUUID())
                .append(loginChainData.getGameVersion())
                .append(loginChainData.getLanguageCode())
                .append(loginChainData.isXboxAuthed() ? loginChainData.getXUID() : "NoXboxAuthed");
        return getMd5(builder.toString());
    }

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = ThreadLocalRandom.current();
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            stringBuffer.append(str.charAt(number));
        }
        return stringBuffer.toString();
    }

    public static String getMd5(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte[] b = md.digest();
            StringBuilder buf = new StringBuilder();
            for (int value : b) {
                int i = value;
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (Exception e) {
            LightLogin.getInstance().getLogger().error("获取MD5值失败", e);
            return plainText;
        }
    }

}
