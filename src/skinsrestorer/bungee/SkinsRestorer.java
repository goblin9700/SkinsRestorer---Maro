package skinsrestorer.bungee;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import skinsrestorer.bungee.commands.AdminCommands;
import skinsrestorer.bungee.commands.PlayerCommands;
import skinsrestorer.bungee.listeners.LoginListener;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.MySQL;

public class SkinsRestorer extends Plugin {

	private static SkinsRestorer instance;
	final CommandSender con = getProxy().getConsole();
	public static SkinsRestorer getInstance() {
		return instance;
	}

	private MySQL mysql;
	private boolean multibungee;
	private ExecutorService exe;

	private boolean outdated;

	@SuppressWarnings("deprecation")
	public void log(String msg){
		con.sendMessage(C.c("&e[&2SkinsRestorer&2] &r"+msg));
	}
	
	public String checkVersion() {
		try {
			HttpURLConnection con = (HttpURLConnection) new URL("http://www.spigotmc.org/api/general.php")
					.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.getOutputStream()
					.write("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=2124"
							.getBytes("UTF-8"));
			String version = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
			if (version.length() <= 7)
				return version;
		} catch (Exception ex) {
			System.out.println("&cFailed to check for an update on spigot.");
		}
		return getVersion();
	}

	public ExecutorService getExecutor() {
		return exe;
	}

	public MySQL getMySQL() {
		return mysql;
	}

	public String getVersion() {
		return getDescription().getVersion();
	}

	public boolean isMultiBungee() {
		return multibungee;
	}

	public boolean isOutdated() {
		return outdated;
	}

	@Override
	public void onDisable() {
		exe.shutdown();
	}

	@Override
	public void onEnable() {
		instance = this;
		Config.load(getResourceAsStream("config.yml"));
		Locale.load();
		MojangAPI.get().loadProxies();
		exe = Executors.newCachedThreadPool();

		if (Config.USE_MYSQL)
			SkinStorage.init(mysql = new MySQL(Config.MYSQL_HOST, Config.MYSQL_PORT, Config.MYSQL_DATABASE,
					Config.MYSQL_USERNAME, Config.MYSQL_PASSWORD));
		else
			SkinStorage.init(getDataFolder());

		getProxy().getPluginManager().registerListener(this, new LoginListener());
		getProxy().getPluginManager().registerCommand(this, new AdminCommands());
		getProxy().getPluginManager().registerCommand(this, new PlayerCommands());
		getProxy().registerChannel("SkinsRestorer");
		SkinApplier.init();

		multibungee = Config.MULTIBUNGEE_ENABLED
				|| ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null;

		exe.submit(new Runnable() {

			@Override
			public void run() {
				if (Config.UPDATER_ENABLED)
					if (checkVersion().equals(getVersion())) {
						outdated = false;
						log("&a----------------------------------------------");
						log(ChatColor.GREEN + "    +===============+");
						log(ChatColor.GREEN + "    | SkinsRestorer |");
						log(ChatColor.GREEN + "    +===============+");
						log("&a----------------------------------------------");
						log(ChatColor.AQUA + "    Current version: " + ChatColor.GREEN + getVersion());
						log(ChatColor.GREEN + "    The latest version!");
						log("&a----------------------------------------------");
					} else {
						outdated = true;
						log("&a----------------------------------------------");
						log(ChatColor.GREEN + "    +===============+");
						log(ChatColor.GREEN + "    | SkinsRestorer |");
						log(ChatColor.GREEN + "    +===============+");
						log("&a----------------------------------------------");
						log(ChatColor.AQUA + "    Current version: " + ChatColor.RED + getVersion());
						log(ChatColor.RED + "    A new version is available! Download it at:");
						log(
								ChatColor.YELLOW + "    https://www.spigotmc.org/resources/skinsrestorer.2124");
						log("&a----------------------------------------------");
					}

				if (Config.DEFAULT_SKINS_ENABLED)
					for (String skin : Config.DEFAULT_SKINS)
						try {
							SkinStorage.setSkinData(skin, MojangAPI.getSkinProperty(MojangAPI.getUUID(skin)));
						} catch (SkinRequestException e) {
							if (SkinStorage.getSkinData(skin) == null)
								log(
										ChatColor.RED + "Default Skin '" + skin + "' request error: " + e.getReason());
						}
			}

		});

	}
}
