package cn.lanink.lightlogin.utils;

import cn.lanink.gamecore.form.windows.AdvancedFormWindowCustom;
import cn.lanink.gamecore.form.windows.AdvancedFormWindowModal;
import cn.lanink.gamecore.form.windows.AdvancedFormWindowSimple;
import cn.lanink.gamecore.mailapi.MailAPI;
import cn.lanink.gamecore.smsapi.SmsApi;
import cn.lanink.lightlogin.LightLogin;
import cn.lanink.lightlogin.PluginConfig;
import cn.lanink.lightlogin.data.PlayerData;
import cn.lanink.lightlogin.data.PlayerDataManager;
import cn.nukkit.Player;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.element.ElementToggle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * @author LT_Name
 */
public class FormHelper {

    /**
     * 发送主界面
     *
     * @param player 玩家
     */
    public static void sendMainForm(@NotNull Player player) {
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(LightLogin.PLUGIN_NAME);
        PluginConfig pluginConfig = LightLogin.getInstance().getPluginConfig();
        PlayerData playerData = PlayerDataManager.getPlayerData(player);
        if (!playerData.isRegistered() || !playerData.isLogin()) {
            return;
        }

        simple.addButton("§e修改密码", FormHelper::sendChangePasswordForm);
        if (pluginConfig.isEnableBindEmail()) {
            if (!playerData.isBindEmail()) {
                simple.addButton("§e绑定邮箱", FormHelper::sendBindEmailForm);
            } else {
                simple.addButton("§c[TODO]§e换绑邮箱", (cp) -> {
                    sendTipReturnMenu(cp, "敬请期待", FormHelper::sendMainForm, null, null);
                });
            }
        }
        if (pluginConfig.isEnableBindPhone()) {
            if (!playerData.isBindPhone()) {
                simple.addButton("§e绑定手机", FormHelper::sendBindPhoneForm);
            } else {
                simple.addButton("§c[TODO]§e换绑手机", (cp) -> {
                    sendTipReturnMenu(cp, "敬请期待", FormHelper::sendMainForm, null, null);
                });
            }
        }

        simple.showToPlayer(player);
    }

    /**
     * 发送修改密码表单
     *
     * @param player 玩家
     */
    public static void sendChangePasswordForm(@NotNull Player player) {
        PlayerData data = PlayerDataManager.getPlayerData(player);
        if (!data.isRegistered() || !data.isLogin()) {
            return;
        }
        AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom(LightLogin.PLUGIN_NAME);
        PluginConfig.PasswordStrengthRequirements requirements = LightLogin.getInstance().getPluginConfig().getPasswordStrengthRequirements();

        custom.addElement(new ElementLabel("您正在更改密码！" +
                "\n新密码长度要求为： " + requirements.getMinLength() + "-" + requirements.getMaxLength() + "\n" +
                (requirements.isNeedNumber() ? "包含数字 " : " ") + (requirements.isNeedLowercaseLetters() ? " 包含小写字母" : "") +
                (requirements.isNeedUppercaseLetters() ? " 包含大写字母" : "") + (requirements.isNeedSpecialCharacters() ? " 包含特殊字符" : ""))); //0
        custom.addElement(new ElementInput("请输入您的原密码", "大小写字母或数字")); //1
        custom.addElement(new ElementInput("请输入您的新密码", "大小写字母或数字")); //2

        custom.onResponded(((formResponse, cp) -> {
            PlayerData playerData = PlayerDataManager.getPlayerData(cp);
            LightLogin lightLogin = LightLogin.getInstance();
            String password = formResponse.getInputResponse(1);
            if (!playerData.getPassword().equals(lightLogin.encryptionPassword(password))) {
                sendTipReturnMenu(cp, "原密码错误！", FormHelper::sendChangePasswordForm, FormHelper::sendChangePasswordForm, FormHelper::sendChangePasswordForm);
                return;
            }
            String newPassword = formResponse.getInputResponse(2);
            if (password.equals(newPassword)) {
                sendTipReturnMenu(cp, "新密码不能与原密码相同！", FormHelper::sendChangePasswordForm, FormHelper::sendChangePasswordForm, FormHelper::sendChangePasswordForm);
                return;
            }
            if (!checkPasswordStrength(cp, newPassword, newPassword, requirements, FormHelper::sendChangePasswordForm)) {
                return;
            }
            playerData.setPassword(lightLogin.encryptionPassword(newPassword));
            playerData.save();
            Consumer<Player> consumer = (cp2) -> {
                cp2.setImmobile(true);
                playerData.setLogin(false);
                FormHelper.sendLoginForm(cp2);
            };
            sendTipReturnMenu(cp, "密码修改成功！", consumer, consumer, consumer);
        }));

        custom.showToPlayer(player);
    }

    /**
     * 发送充值密码表单
     *
     * @param player 玩家
     */
    public static void sendResetPasswordMainForm(@NotNull Player player, @Nullable Consumer<Player> closeConsumer) {
        PluginConfig pluginConfig = LightLogin.getInstance().getPluginConfig();
        PlayerData data = PlayerDataManager.getPlayerData(player);
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(LightLogin.PLUGIN_NAME);
        if (data.isBindEmail() && pluginConfig.isEnableBindEmail()) {
            simple.addButton("通过邮箱验证", p -> FormHelper.sendResetPasswordVerifyForm(p, PlayerVerificationData.VerificationType.EMAIL, closeConsumer));
        }
        if (data.isBindPhone() && pluginConfig.isEnableBindPhone()) {
            simple.addButton("通过手机验证", p -> FormHelper.sendResetPasswordVerifyForm(p, PlayerVerificationData.VerificationType.PHONE, closeConsumer));
        }

        if (simple.getButtons().isEmpty()) {
            simple.setContent("您没有绑定邮箱/手机 或服务器未启用功能，请联系管理员处理！");
        } else {
            simple.setContent("请选择重置密码验证方式");
        }

        if (closeConsumer != null) {
            simple.onClosed(closeConsumer);
        }

        simple.showToPlayer(player);
    }

    private static void sendResetPasswordVerifyForm(@NotNull Player player, PlayerVerificationData.VerificationType type, @Nullable Consumer<Player> closeConsumer) {
        PlayerData data = PlayerDataManager.getPlayerData(player);
        if ((type == PlayerVerificationData.VerificationType.EMAIL && !data.isBindEmail())
                || (type == PlayerVerificationData.VerificationType.PHONE && !data.isBindPhone())) {
            return;
        }

        PlayerVerificationData verificationData;
        if ((verificationData = LightLogin.getInstance().getPlayerResetVerificationDataCache().getIfPresent(player)) == null
                || verificationData.getType() != type) {
            verificationData = new PlayerVerificationData();
            verificationData.setPlayer(player);
            verificationData.setAccountNumber(type == PlayerVerificationData.VerificationType.EMAIL ? data.getEmail() : data.getPhone());
            verificationData.setCode(String.valueOf(new Random().nextInt(899999) + 100000));
            verificationData.setTime(System.currentTimeMillis());
            verificationData.setType(type);
            LightLogin.getInstance().getPlayerResetVerificationDataCache().put(player, verificationData);

            switch (type) {
                case EMAIL -> {
                    MailAPI.getInstance().sendMail(
                            data.getEmail(),
                            "LightLogin - 密码重置验证",
                            "LightLogin： 您正在申请重置密码，您的验证码为 " + verificationData.getCode()
                    );
                }
                case PHONE -> {
                    SmsApi.getInstance().sendSms(data.getPhone(), verificationData.getCode());
                }
            }
        }

        AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom(LightLogin.PLUGIN_NAME);
        String s = switch (type) {
            case EMAIL -> "邮箱";
            case PHONE -> "手机";
        };
        custom.addElement(new ElementLabel("您正在通过 " + s + " 验证重置密码")); //0
        custom.addElement(new ElementInput("请输入您收到的验证码", "验证码")); //1

        PlayerVerificationData finalVerificationData = verificationData;
        custom.onResponded(((formResponse, cp) -> {
            String inputCode = formResponse.getInputResponse(1);
            if (finalVerificationData.getTime() + 180 * 1000 < System.currentTimeMillis()) {
                sendTipReturnMenu(player, "验证码已失效！", (p) -> sendResetPasswordMainForm(player, closeConsumer), null, null);
            } else if (inputCode != null && inputCode.trim().equals(finalVerificationData.getCode())) {
                String newPassword = Utils.getRandomString(16);
                data.setPassword(LightLogin.getInstance().encryptionPassword(newPassword));
                data.setLoginComplete(player);
                sendTipReturnMenu(player, "您的密码已成功重置为 " + newPassword + " 请及时修改！");
                if (data.isBindEmail() && LightLogin.getInstance().getPluginConfig().isEnableBindEmail()) {
                    MailAPI.getInstance().sendMail(data.getEmail(), "LightLogin - 密码重置成功！", "您的密码已成功重置为 " + newPassword + " 请及时修改！");
                }
            } else {
                sendTipReturnMenu(player, "验证码错误！", (p) -> sendResetPasswordVerifyForm(player, type, closeConsumer), null, null);
            }
        }));

        if (closeConsumer != null) {
            custom.onClosed(closeConsumer);
        }

        custom.showToPlayer(player);
    }

    /**
     * 发送绑定邮箱表单
     *
     * @param player 玩家
     */
    public static void sendBindEmailForm(@NotNull Player player) {
        PlayerData data = PlayerDataManager.getPlayerData(player);
        if (!LightLogin.getInstance().getPluginConfig().isEnableBindEmail() || data.isBindEmail()) {
            return;
        }

        PlayerVerificationData cacheVData = LightLogin.getInstance().getPlayerEmailVerificationDataCache().getIfPresent(player);
        if (cacheVData != null) {
            if (cacheVData.getTime() + 180 * 1000 > System.currentTimeMillis()) {
                sendBindEmailVerifyForm(player, data, cacheVData);
                return;
            } else {
                LightLogin.getInstance().getPlayerEmailVerificationDataCache().invalidate(player);
            }
        }

        AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom(LightLogin.PLUGIN_NAME);

        custom.addElement(new ElementLabel("您正在绑定邮箱！")); //0
        custom.addElement(new ElementInput("请输入您要绑定的邮箱", "邮箱")); //1

        custom.onResponded((formResponse, cp) -> {
            if (data.getLastSendEmailCodeTime() + 60 * 1000 > System.currentTimeMillis()) {
                sendTipReturnMenu(player, "请勿频繁发送验证码！请稍后重试！", FormHelper::sendBindEmailForm, null, null);
                return;
            }

            String email = formResponse.getInputResponse(1);
            if (!Pattern.matches("^(\\w+([-.][A-Za-z0-9]+)*){3,18}@\\w+([-.][A-Za-z0-9]+)*\\.\\w+([-.][A-Za-z0-9]+)*$", email)) {
                sendTipReturnMenu(player, "请输入正确的邮箱！", FormHelper::sendBindEmailForm, null, null);
                return;
            }

            PlayerVerificationData verificationData = new PlayerVerificationData();
            verificationData.setPlayer(player);
            verificationData.setAccountNumber(email.trim());
            verificationData.setCode(String.valueOf(new Random().nextInt(899999) + 100000));
            verificationData.setTime(System.currentTimeMillis());
            LightLogin.getInstance().getPlayerEmailVerificationDataCache().put(player, verificationData);

            PlayerData playerData = PlayerDataManager.getPlayerData(player);
            playerData.setLastSendEmailCodeTime(System.currentTimeMillis());
            player.sendTitle(LightLogin.PLUGIN_NAME, "验证码已发送，请稍等片刻！");
            player.sendMessage("验证码已发送，请注意查收！Tips：可使用/LLBind 命令完成绑定！");
            if (LightLogin.getInstance().getPluginConfig().isDebug()) {
                LightLogin.getInstance().getLogger().info("[debug] 发送验证码：" + verificationData.getCode() + " 目标邮箱：" + verificationData.getAccountNumber());
            }
            MailAPI.getInstance().sendMail(
                    verificationData.getAccountNumber(),
                    "LightLogin 账号绑定验证码",
                    "您的验证码为： " + verificationData.getCode() + " \n请在三分钟内输入验证码完成绑定！",
                    (callBack) -> {
                        if (LightLogin.getInstance().getPluginConfig().isDebug()) {
                            LightLogin.getInstance().getLogger().info("[debug] 验证码发送结果：" + callBack.getMessage());
                        }
                        if (callBack.isSuccess()) {
                            sendBindEmailVerifyForm(player, playerData, verificationData);
                        } else {
                            sendTipReturnMenu(player, "发送验证码失败！请检查邮箱 " + email + " 是否正确或稍后重试！\n错误：" + callBack.getMessage());
                        }
                    }
            );
        });

        custom.showToPlayer(player);
    }

    public static void sendBindEmailVerifyForm(@NotNull Player player, @NotNull PlayerData playerData, @NotNull PlayerVerificationData verificationData) {
        if (!LightLogin.getInstance().getPluginConfig().isEnableBindEmail()) {
            return;
        }
        AdvancedFormWindowCustom verifyCustom = new AdvancedFormWindowCustom(LightLogin.PLUGIN_NAME);

        verifyCustom.addElement(new ElementLabel("""
                您正在绑定邮箱！
                验证码发送成功三分钟内有效！请输入验证码！

                """)); //0
        verifyCustom.addElement(new ElementInput("请输入您收到的验证码", "验证码")); //1

        verifyCustom.onResponded((verifyFormResponse, cp2) -> {
            String inputCode = verifyFormResponse.getInputResponse(1);
            if (verificationData.getTime() + 180 * 1000 < System.currentTimeMillis()) {
                sendTipReturnMenu(player, "验证码已失效！", (p) -> sendBindEmailForm(player), null, null);
            } else if (inputCode != null && inputCode.trim().equals(verificationData.getCode())) {
                playerData.setEmail(verificationData.getAccountNumber());
                playerData.save();
                LightLogin.getInstance().getPlayerPhoneVerificationDataCache().invalidate(player);
                sendTipReturnMenu(player, "您已成功绑定邮箱 " + verificationData.getAccountNumber());
            } else {
                sendTipReturnMenu(player, "验证码错误！", (p) -> sendBindEmailVerifyForm(player, playerData, verificationData), null, null);
            }
        });

        verifyCustom.showToPlayer(player);
    }

    /**
     * 发送绑定手机号表单
     *
     * @param player 玩家
     */
    public static void sendBindPhoneForm(@NotNull Player player) {
        PlayerData data = PlayerDataManager.getPlayerData(player);
        if (!LightLogin.getInstance().getPluginConfig().isEnableBindPhone() || data.isBindPhone()) {
            return;
        }

        PlayerVerificationData cacheVData = LightLogin.getInstance().getPlayerPhoneVerificationDataCache().getIfPresent(player);
        if (cacheVData != null) {
            if (cacheVData.getTime() + 180 * 1000 > System.currentTimeMillis()) {
                sendBindPhoneVerifyForm(player, data, cacheVData);
                return;
            } else {
                LightLogin.getInstance().getPlayerPhoneVerificationDataCache().invalidate(player);
            }
        }

        AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom(LightLogin.PLUGIN_NAME);

        custom.addElement(new ElementLabel("""
                您正在绑定手机号！

                请注意！LightLogin会尝试使用加密等手段保护您的数据安全，但请注意如果服务器发生数据泄漏密钥也大概率会泄漏，请您在绑定手机号前三思！！！

                """)); //0
        custom.addElement(new ElementInput("请输入您要绑定的手机号", "手机号")); //1
        custom.addElement(new ElementToggle("我已知晓并同意将手机号存储到服务器", false)); //2

        custom.onResponded((formResponse, cp) -> {
            if (!formResponse.getToggleResponse(2)) {
                sendTipReturnMenu(player, "未同意将手机号存储到服务器，绑定取消！", FormHelper::sendMainForm, null, null);
                return;
            }

            if (data.getLastSendSmsCodeTime() + 60 * 1000 > System.currentTimeMillis()) {
                sendTipReturnMenu(player, "请勿频繁发送验证码！请稍后重试！", FormHelper::sendBindPhoneForm, null, null);
                return;
            }

            String phone = formResponse.getInputResponse(1);
            if (!Pattern.matches("^1[3-9]\\d{9}$", phone)) {
                sendTipReturnMenu(player, "请输入正确的手机号！", FormHelper::sendBindPhoneForm, null, null);
                return;
            }

            PlayerVerificationData verificationData = new PlayerVerificationData();
            verificationData.setPlayer(player);
            verificationData.setAccountNumber(phone.trim());
            verificationData.setCode(String.valueOf(new Random().nextInt(899999) + 100000));
            verificationData.setTime(System.currentTimeMillis());
            LightLogin.getInstance().getPlayerPhoneVerificationDataCache().put(player, verificationData);

            PlayerData playerData = PlayerDataManager.getPlayerData(player);
            playerData.setLastSendSmsCodeTime(System.currentTimeMillis());
            player.sendTitle(LightLogin.PLUGIN_NAME, "验证码已发送，请稍等片刻！");
            player.sendMessage("验证码已发送，请注意查收！Tips：可使用/LLBind 命令完成绑定！");
            SmsApi.getInstance().sendSms(verificationData.getAccountNumber(), verificationData.getCode(), (callBack) -> {
                if (callBack.isSuccess()) {
                    sendBindPhoneVerifyForm(player, playerData, verificationData);
                } else {
                    sendTipReturnMenu(player, "发送验证码失败！请检查手机号 " + phone + " 是否正确或稍后重试！\n错误：" + callBack.getMessage());
                }
            });
        });

        custom.showToPlayer(player);
    }

    /**
     * 发送绑定手机号验证码表单
     *
     * @param player 玩家
     * @param playerData 玩家数据
     * @param verificationData 验证数据
     */
    public static void sendBindPhoneVerifyForm(Player player, PlayerData playerData, PlayerVerificationData verificationData) {
        if (!LightLogin.getInstance().getPluginConfig().isEnableBindPhone()) {
            return;
        }
        AdvancedFormWindowCustom verifyCustom = new AdvancedFormWindowCustom(LightLogin.PLUGIN_NAME);

        verifyCustom.addElement(new ElementLabel("""
                您正在绑定手机号！ 
                验证码发送成功三分钟内有效！请输入验证码！

                请注意！LightLogin会尝试使用加密等手段保护您的数据安全，但请注意如果服务器发生数据泄漏密钥也大概率会泄漏，请您在绑定手机号前三思！！！

                """)); //0
        verifyCustom.addElement(new ElementInput("请输入您收到的验证码", "验证码")); //1

        verifyCustom.onResponded((verifyFormResponse, cp2) -> {
            String inputCode = verifyFormResponse.getInputResponse(1);
            if (verificationData.getTime() + 180 * 1000 < System.currentTimeMillis()) {
                sendTipReturnMenu(player, "验证码已失效！", (p) -> sendBindPhoneForm(player), null, null);
            } else if (inputCode != null && inputCode.trim().equals(verificationData.getCode())) {
                playerData.setPhone(verificationData.getAccountNumber());
                playerData.save();
                LightLogin.getInstance().getPlayerPhoneVerificationDataCache().invalidate(player);
                sendTipReturnMenu(player, "您已成功绑定手机号 " + verificationData.getAccountNumber());
            } else {
                sendTipReturnMenu(player, "验证码错误！", (p) -> sendBindPhoneVerifyForm(player, playerData, verificationData), null, null);
            }
        });

        verifyCustom.showToPlayer(player);
    }

    /**
     * 发送注册表单
     *
     * @param player 玩家
     */
    public static void sendRegisterForm(@NotNull Player player) {
        if (PlayerDataManager.getPlayerData(player).isRegistered()) {
            return;
        }
        AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom(LightLogin.PLUGIN_NAME);
        PluginConfig.PasswordStrengthRequirements requirements = LightLogin.getInstance().getPluginConfig().getPasswordStrengthRequirements();
        custom.addElement(new ElementLabel("您需要先注册！" +
                "\n密码长度要求为： " + requirements.getMinLength() + "-" + requirements.getMaxLength() + "\n" +
                (requirements.isNeedNumber() ? "包含数字 " : " ") + (requirements.isNeedLowercaseLetters() ? " 包含小写字母" : "") +
                (requirements.isNeedUppercaseLetters() ? " 包含大写字母" : "") + (requirements.isNeedSpecialCharacters() ? " 包含特殊字符" : ""))); //0
        custom.addElement(new ElementInput("请输入您要设置的密码", "大小写字母或数字")); //1
        custom.addElement(new ElementInput("请重复输入密码确认", "大小写字母或数字")); //2

        custom.onResponded((formResponse, cp) -> {
            String password = formResponse.getInputResponse(1);
            if (!checkPasswordStrength(cp, password, formResponse.getInputResponse(2), requirements, FormHelper::sendRegisterForm)) {
                return;
            }
            PlayerData playerData = PlayerDataManager.getPlayerData(cp);
            playerData.setPassword(LightLogin.getInstance().encryptionPassword(password));
            playerData.setLoginComplete(cp);
            cp.setImmobile(false);
            sendTipReturnMenu(cp, "注册成功！");
        });

        custom.onClosed(FormHelper::sendRegisterForm);

        custom.showToPlayer(player);
    }

    private static boolean checkPasswordStrength(Player player, String password, String password2, PluginConfig.PasswordStrengthRequirements requirements, Consumer<Player> consumer) {
        if (password == null || password.isBlank()) {
            sendTipReturnMenu(player, "密码不能为空！", consumer, consumer, consumer);
            return false;
        }
        if (!password.equals(password2)) {
            sendTipReturnMenu(player, "两次输入的密码不一致！", consumer, consumer, consumer);
            return false;
        }
        if (password.length() < requirements.getMinLength() || password.length() > requirements.getMaxLength()) {
            sendTipReturnMenu(player,
                    "密码长度要求为 " + requirements.getMinLength() + "-" + requirements.getMaxLength() + " 之间！",
                    consumer, consumer, consumer);
            return false;
        }
        if (requirements.isNeedNumber() && !password.matches(".*\\d+.*")) {
            sendTipReturnMenu(player, "密码必须包含数字！", consumer, consumer, consumer);
            return false;
        }
        if (requirements.isNeedLowercaseLetters() && !password.matches(".*[a-z]+.*")) {
            sendTipReturnMenu(player, "密码必须包含小写字母！", consumer, consumer, consumer);
            return false;
        }
        if (requirements.isNeedUppercaseLetters() && !password.matches(".*[A-Z]+.*")) {
            sendTipReturnMenu(player, "密码必须包含大写字母！", consumer, consumer, consumer);
            return false;
        }
        if (requirements.isNeedSpecialCharacters() && !password.matches(".*[~!@#$%^&*()_+|<>,.?/:;'\\[\\]{}\"]+.*")) {
            sendTipReturnMenu(player, "密码必须包含特殊符号！", consumer, consumer, consumer);
            return false;
        }
        return true;
    }

    public static void sendLoginForm(@NotNull Player player) {
        if (PlayerDataManager.getPlayerData(player).isLogin()) {
            return;
        }
        AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom(LightLogin.PLUGIN_NAME);
        custom.addElement(new ElementLabel("您需要输入密码进行登录！")); //0
        custom.addElement(new ElementInput("请输入您的密码", "大小写字母或数字")); //1

        custom.onResponded((formResponse, cp) -> {
            PlayerData playerData = PlayerDataManager.getPlayerData(cp);
            String password = formResponse.getInputResponse(1);
            if (password == null || password.isBlank() || password.length() < 6) {
                sendTipReturnMenu(cp, "密码格式错误！", FormHelper::sendLoginForm, FormHelper::sendLoginForm, FormHelper::sendLoginForm);
                return;
            }
            LightLogin lightLogin = LightLogin.getInstance();
            if (!playerData.getPassword().equals(lightLogin.encryptionPassword(password))) {
                playerData.loginErrorCount++;
                if (playerData.loginErrorCount >= lightLogin.getPluginConfig().getMaxLoginErrorCount()) {
                    playerData.setLoginLockTime(System.currentTimeMillis() + lightLogin.getPluginConfig().getLoginLockTime() * 1000L);
                    playerData.setLoginErrorCount(0);
                    playerData.save();
                    cp.kick("§c密码错误次数过多，请稍后重试！", false);
                    return;
                }
                sendTipReturnMenu(cp, "密码错误！",
                        "返回",
                        FormHelper::sendLoginForm,
                        "重置密码",
                        p -> FormHelper.sendResetPasswordMainForm(player, FormHelper::sendLoginForm),
                        FormHelper::sendLoginForm
                );
                return;
            }
            playerData.setLoginComplete(player);
            sendTipReturnMenu(cp, "登录成功！");
        });

        custom.onClosed(FormHelper::sendLoginForm);

        custom.showToPlayer(player);
    }

    private static void sendTipReturnMenu(@NotNull Player player, @NotNull String text) {
        sendTipReturnMenu(player, text, null, null, null);
    }

    private static void sendTipReturnMenu(
            @NotNull Player player,
            @NotNull String text,
            @Nullable Consumer<Player> consumerTrue,
            @Nullable Consumer<Player> consumerFalse,
            @Nullable Consumer<Player> consumerClose
    ) {
        sendTipReturnMenu(player, text, "返回", consumerTrue, "关闭", consumerFalse, consumerClose);
    }

    private static void sendTipReturnMenu(
            @NotNull Player player,
            @NotNull String text,
            @NotNull String trueButtonText,
            @Nullable Consumer<Player> consumerTrue,
            @NotNull String falseButtonText,
            @Nullable Consumer<Player> consumerFalse,
            @Nullable Consumer<Player> consumerClose
    ) {
        AdvancedFormWindowModal modal = new AdvancedFormWindowModal(
                LightLogin.PLUGIN_NAME,
                text,
                trueButtonText,
                falseButtonText
        );

        if (consumerTrue != null) {
            modal.onClickedTrue(consumerTrue);
        }
        if (consumerFalse != null) {
            modal.onClickedFalse(consumerFalse);
        }
        if (consumerClose != null) {
            modal.onClosed(consumerClose);
        }

        modal.showToPlayer(player);
    }

}
