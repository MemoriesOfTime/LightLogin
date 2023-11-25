package cn.lanink.lightlogin.command;

import cn.lanink.lightlogin.LightLogin;
import cn.lanink.lightlogin.data.PlayerData;
import cn.lanink.lightlogin.data.PlayerDataManager;
import cn.lanink.lightlogin.utils.FormHelper;
import cn.lanink.lightlogin.utils.PlayerVerificationData;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;

import java.util.Objects;

/**
 * @author LT_Name
 */
public class LightLoginBindCommand extends Command {

    public LightLoginBindCommand(String name) {
        super(name, "邮箱/手机绑定验证码", "/llbind 验证码");
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        if (!commandSender.isPlayer()) {
            commandSender.sendMessage("§c请在游戏内使用此命令！");
            return true;
        }
        Player player = Objects.requireNonNull(commandSender.asPlayer());
        PlayerVerificationData cacheVData = LightLogin.getInstance().getPlayerEmailVerificationDataCache().getIfPresent(player);
        if (cacheVData != null) {
            if (cacheVData.getTime() + 180 * 1000 > System.currentTimeMillis()) {
                FormHelper.sendBindEmailVerifyForm(player, PlayerDataManager.getPlayerData(player), cacheVData);
                return true;
            }
        }

        cacheVData = LightLogin.getInstance().getPlayerPhoneVerificationDataCache().getIfPresent(player);
        if (cacheVData != null) {
            if (cacheVData.getTime() + 180 * 1000 > System.currentTimeMillis()) {
                FormHelper.sendBindPhoneVerifyForm(player, PlayerDataManager.getPlayerData(player), cacheVData);
                return true;
            }
        }

        commandSender.sendMessage("§c您还没有发送验证码！");

        return true;
    }

}
