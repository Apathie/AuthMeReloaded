package fr.xephi.authme.process.register;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.register.executors.RegistrationExecutor;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

import static fr.xephi.authme.permission.PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS;

/**
 * Asynchronous processing of a request for registration.
 */
public class AsyncRegister implements AsynchronousProcess {

    @Inject
    private DataSource database;
    @Inject
    private PlayerCache playerCache;
    @Inject
    private CommonService service;
    @Inject
    private PermissionsManager permissionsManager;

    AsyncRegister() {
    }

    /**
     * Performs the registration process for the given player.
     *
     * @param player the player to register
     * @param executor the registration executor to perform the registration with
     */
    public void register(Player player, RegistrationExecutor executor) {
        if (preRegisterCheck(player) && executor.isRegistrationAdmitted()) {
            executeRegistration(player, executor);
        }
    }

    private boolean preRegisterCheck(Player player) {
        final String name = player.getName().toLowerCase();
        if (playerCache.isAuthenticated(name)) {
            service.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return false;
        } else if (!service.getProperty(RegistrationSettings.IS_ENABLED)) {
            service.send(player, MessageKey.REGISTRATION_DISABLED);
            return false;
        } else if (database.isAuthAvailable(name)) {
            service.send(player, MessageKey.NAME_ALREADY_REGISTERED);
            return false;
        }

        return isPlayerIpAllowedToRegister(player);
    }

    /**
     * Executes the registration.
     *
     * @param player the player to register
     * @param executor the executor to perform the registration process with
     */
    private void executeRegistration(Player player, RegistrationExecutor executor) {
        PlayerAuth auth = executor.buildPlayerAuth();
        if (database.saveAuth(auth)) {
            executor.executePostPersistAction();
        } else {
            service.send(player, MessageKey.ERROR);
        }
    }

    /**
     * Checks whether the registration threshold has been exceeded for the given player's IP address.
     *
     * @param player the player to check
     * @return true if registration may take place, false otherwise (IP check failed)
     */
    private boolean isPlayerIpAllowedToRegister(Player player) {
        final int maxRegPerIp = service.getProperty(RestrictionSettings.MAX_REGISTRATION_PER_IP);
        final String ip = PlayerUtils.getPlayerIp(player);
        if (maxRegPerIp > 0
            && !"127.0.0.1".equalsIgnoreCase(ip)
            && !"localhost".equalsIgnoreCase(ip)
            && !permissionsManager.hasPermission(player, ALLOW_MULTIPLE_ACCOUNTS)) {
            List<String> otherAccounts = database.getAllAuthsByIp(ip);
            if (otherAccounts.size() >= maxRegPerIp) {
                service.send(player, MessageKey.MAX_REGISTER_EXCEEDED, Integer.toString(maxRegPerIp),
                    Integer.toString(otherAccounts.size()), String.join(", ", otherAccounts));
                return false;
            }
        }
        return true;
    }
}
