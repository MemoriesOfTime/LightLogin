package cn.lanink.lightlogin.command;

import cn.lanink.lightlogin.utils.FormHelper;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;

import java.util.Objects;

/**
 * @author LT_Name
 */
public class LightLoginCommand extends Command {

    public LightLoginCommand(String name) {
        super(name);
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        if (!commandSender.isPlayer()) {
            commandSender.sendMessage("§c请在游戏内使用此命令！");
            return true;
        }
        FormHelper.sendMainForm(Objects.requireNonNull(commandSender.asPlayer()));
        return true;
    }
}
