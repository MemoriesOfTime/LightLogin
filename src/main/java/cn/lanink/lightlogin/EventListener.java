package cn.lanink.lightlogin;

import cn.lanink.lightlogin.data.PlayerData;
import cn.lanink.lightlogin.data.PlayerDataManager;
import cn.lanink.lightlogin.utils.FormHelper;
import cn.lanink.lightlogin.utils.Utils;
import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.inventory.InventoryOpenEvent;
import cn.nukkit.event.inventory.InventoryPickupItemEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.inventory.InventoryHolder;

import java.util.ArrayList;

/**
 * @author LT_Name
 */
public class EventListener implements Listener {

    private final LightLogin lightLogin;

    public EventListener(LightLogin lightLogin) {
        this.lightLogin = lightLogin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setImmobile(true);
        if (PlayerDataManager.getPlayerData(player).getLoginLockTime() > System.currentTimeMillis()) {
            player.kick("§c错误！请稍后重试！\n错误代码：LightLogin-10001", false);
        }
    }

    @EventHandler
    public void onPlayerPreLogin(PlayerPreLoginEvent event) {
        Player player = event.getPlayer();

        Player repeatPlayer = null;
        for (Player p : new ArrayList<>(lightLogin.getServer().getOnlinePlayers().values())) {
            if (p != player && p.getName() != null) {
                if (p.getName().equalsIgnoreCase(player.getName()) || player.getUniqueId().equals(p.getUniqueId())) {
                    repeatPlayer = p;
                    break;
                }
            }
        }

        if (repeatPlayer != null) {
            PlayerData data = PlayerDataManager.getPlayerData(repeatPlayer);
            if (data.isLogin() || Utils.getClientIdentification(repeatPlayer).equals(data.getClientIdentification())) {
                event.setCancelled(true);
                player.kick("§c您已在其他客户端登录！", false);
            } else {
                repeatPlayer.kick("§c您已在其他客户端登录！", false);
            }
        }
    }

    @EventHandler
    public void onPlayerLocallyInitialized(PlayerLocallyInitializedEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = PlayerDataManager.getPlayerData(player);

        if (playerData.isRegistered()
                && !player.getAddress().equals("127.0.0.1") //本地或内网穿透时不启用自动登录
                && Utils.getClientIdentification(player).equalsIgnoreCase(playerData.getClientIdentification())
                && playerData.getLastLoginQuitTime() + this.lightLogin.getPluginConfig().getKeepLoginTime() * 1000L > System.currentTimeMillis()) {
            playerData.setLoginComplete(player);
            player.sendTitle(LightLogin.PLUGIN_NAME, "§a自动登录成功！");
            this.checkPlayerBind(playerData, player);
            return;
        }
        if (!playerData.isRegistered()) {
            FormHelper.sendRegisterForm(player);
        } else if (player.getLoginChainData().isXboxAuthed()
                && Utils.getClientIdentification(player).equals(playerData.getClientIdentification())) {
            playerData.setLoginComplete(player);
            player.sendTitle(LightLogin.PLUGIN_NAME, "§a通过Xbox自动登录成功！");
            this.checkPlayerBind(playerData, player);
        } else {
            FormHelper.sendLoginForm(player);
        }
    }

    private void checkPlayerBind(PlayerData data, Player player) {
        PluginConfig pluginConfig = this.lightLogin.getPluginConfig();
        if (pluginConfig.isEnableBindEmail() || pluginConfig.isEnableBindPhone()) {
            if (!data.isBindEmail() && !data.isBindPhone()) {
                this.lightLogin.getServer().getScheduler().scheduleDelayedTask(this.lightLogin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage("[" + LightLogin.PLUGIN_NAME + "] §e您还未绑定邮箱或手机！建议您使用 /LightLogin 命令进行绑定！");
                    }
                }, 20, true); //稍微延迟下保证信息排在后面显示
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = PlayerDataManager.getPlayerData(player);
        if (playerData.isRegistered()) {
            if (playerData.isLogin() && !playerData.isXboxLogin()) {
                playerData.setLastLoginQuitTime(System.currentTimeMillis());
            }
            PlayerDataManager.save(playerData);
        }
        PlayerDataManager.remove(player);
        LightLogin.getInstance().getPlayerPhoneVerificationDataCache().invalidate(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = PlayerDataManager.getPlayerData(player);
        if (!playerData.isLogin()
                && event.getFrom().getFloorX() != event.getTo().getFloorX()
                && event.getFrom().getFloorZ() != event.getTo().getFloorZ()) {
            if (!playerData.isRegistered()) {
                FormHelper.sendRegisterForm(player);
            } else {
                FormHelper.sendLoginForm(player);
            }
            event.setCancelled();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = PlayerDataManager.getPlayerData(player);
        if (!playerData.isLogin()) {
            if (!playerData.isRegistered()) {
                FormHelper.sendRegisterForm(player);
            } else {
                FormHelper.sendLoginForm(player);
            }
            event.setCancelled();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!PlayerDataManager.getPlayerData(player).isLogin()) {
            event.setCancelled();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!PlayerDataManager.getPlayerData(player).isLogin()) {
            event.setCancelled();
        }
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!PlayerDataManager.getPlayerData(player).isLogin()) {
            event.setCancelled();
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!PlayerDataManager.getPlayerData(player).isLogin()) {
            event.setCancelled();
        }
    }

    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Player) {
            Player player = (Player) holder;
            if (!PlayerDataManager.getPlayerData(player).isLogin()) {
                event.setCancelled();
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        if (!PlayerDataManager.getPlayerData(player).isLogin()) {
            event.setCancelled();
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!PlayerDataManager.getPlayerData(player).isLogin()) {
                event.setCancelled();
            }
        }
    }

}
