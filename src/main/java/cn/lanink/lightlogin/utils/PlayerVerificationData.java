package cn.lanink.lightlogin.utils;

import cn.nukkit.Player;
import lombok.Data;

/**
 * @author LT_Name
 */
@Data
public class PlayerVerificationData {

    private Player player;
    private String accountNumber;
    private String code;
    private Long time; //时间戳
    private VerificationType type;

    public enum VerificationType {
        EMAIL,
        PHONE;
    }

}
