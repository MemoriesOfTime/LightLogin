package cn.lanink.lightlogin.data;

import cn.lanink.lightlogin.LightLogin;
import cn.lanink.lightlogin.utils.Utils;
import cn.nukkit.Player;
import lombok.Data;

/**
 * @author LT_Name
 */
@Data
public class PlayerData {

    private String name;

    private String password = "";
    private String email = "";
    private String phone = "";

    private String clientIdentification = "";
    @Deprecated
    private String lastLoginIp = "";
    private long lastLoginQuitTime = -1; //时间戳
    private long loginLockTime = -1; //时间戳
    private long lastSendEmailCodeTime = -1; //时间戳
    private long lastSendSmsCodeTime = -1; //时间戳

    private boolean isLogin = false;
    private boolean isXboxLogin = false;
    public int loginErrorCount = 0;

    private long loginCompleteTime = -1; //时间戳

    public PlayerData(String name) {
        this.name = name;
    }

    public void save() {
        PlayerDataManager.save(this);
    }

    public boolean isRegistered() {
        return this.password != null && !this.password.isBlank();
    }

    public void setLoginComplete(Player player) {
        this.isLogin = true;
        if (player.getLoginChainData().isXboxAuthed()) {
            this.isXboxLogin = true;
        }

        this.setClientIdentification(Utils.getClientIdentification(player));

        PlayerDataManager.setUUID(player.getName(), player.getUniqueId());

        player.setImmobile(false);

        this.loginErrorCount = 0;

        this.loginCompleteTime = System.currentTimeMillis();

        this.save();
    }

    public boolean isBindEmail() {
        if (this.email == null || this.email.isBlank()) {
            return false;
        }
        String originalMail = this.getEmail();
        return originalMail != null && !originalMail.isBlank();
    }

    public String getEmail() {
        return LightLogin.getInstance().getAes().decrypt(this.email);
    }

    public String getEncryptEmail() {
        return this.email;
    }

    public void setEmail(String mail) {
        this.email = LightLogin.getInstance().getAes().encrypt(mail);
    }

    public void setEncryptEmail(String mail) {
        this.email = mail;
    }

    public boolean isBindPhone() {
        if (this.phone == null || this.phone.isBlank()) {
            return false;
        }
        String originalPhone = this.getPhone();
        return originalPhone != null && !originalPhone.isBlank();
    }

    public String getPhone() {
        return LightLogin.getInstance().getAes().decrypt(this.phone);
    }

    public String getEncryptPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = LightLogin.getInstance().getAes().encrypt(phone);
    }

    public void setEncryptPhone(String phone) {
        this.phone = phone;
    }
}
