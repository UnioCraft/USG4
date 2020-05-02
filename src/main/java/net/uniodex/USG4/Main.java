package net.uniodex.USG4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dye;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import com.connorlinfoot.actionbarapi.ActionBarAPI;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import e.Game;
import e.SGGameEndEvent;
import e.SGGameStartEvent;
import e.SGPlayer;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin implements PluginMessageListener, Listener {
	ArrayList<String> servername = new ArrayList<String>();
	public String sunucuadi = null;
	public String playerList = null;
	public String prefix = null;
	public static String nmsver;
	public static Chat chat = null;
	public static Plugin plugin;
	String account = null;
	public static boolean works = true;
	public Set<Player> flying = new HashSet<>();
	HashMap<Player, Integer> kills;
	HashMap<Player, Integer> chests;
	HashMap<Player, Integer> gamesPlayed;
	HashMap<Player, Integer> deaths;
	HashMap<Player, Integer> points;
	static FileConfiguration fc;
	static SQLManager sql;
	public ArrayList<EnchantingInventory> inventories;

	String hataprefix = ChatColor.AQUA + "" + ChatColor.BOLD + "USG " + ChatColor.DARK_GREEN + "->" + ChatColor.RED + " ";
	String dikkatprefix = ChatColor.AQUA + "" + ChatColor.BOLD + "USG " + ChatColor.DARK_GREEN + "->" + ChatColor.GOLD + " ";
	String bilgiprefix = ChatColor.AQUA + "" + ChatColor.BOLD + "USG " + ChatColor.DARK_GREEN + "->" + ChatColor.GREEN + " ";

	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() // Başlangıçta
	{
		initDatabase(); // Veritabanı bağlantısını hazırla
		inventories = new ArrayList<EnchantingInventory>();
		fc = this.getConfig(); // Config tanıma
		// HashMap'leri tanımla
		kills = new HashMap<Player, Integer>();
		chests = new HashMap<Player, Integer>();
		gamesPlayed = new HashMap<Player, Integer>();
		deaths = new HashMap<Player, Integer>();
		points = new HashMap<Player, Integer>();
		plugin = this; // Plugin tanıma
		setupChat(); // Chati hazırla
		setupVault(); // Vault hazırla 
		saveDefaultConfig(); // Configi kaydet
		// Listenerları tanımla
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
		// Sunucu başlatıldıktan 0.5 saniye sonra oyun kodunu veritabanına gir.
		Bukkit.getServer().getScheduler()
		.scheduleAsyncDelayedTask(this, new Runnable()
		{
			public void run()
			{
				sql.oyunKodugir();
			}
		}, 10L);
		// ActionBarAPI için NMS versiyonunu tanımla
		nmsver = Bukkit.getServer().getClass().getPackage().getName();
		nmsver = nmsver.substring(nmsver.lastIndexOf(".") + 1);

	}

	@Override
	public void onDisable() { // Devre dışı bırakıldığında
		sql.onDisable(); // Veritabanı bağlantısını kes*
		for (EnchantingInventory ei : inventories) {
			ei.setItem(1, null);
		}
		inventories = null;
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}
		// Sunucu adını çek
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();
		if (subchannel.equals("GetServer")) {
			String server = in.readUTF();
			if (!servername.contains(server)) {
				servername.add(server);
			}
		}
	}

	/*********************/
	/******KURULUMLAR*****/
	/*********************/

	private boolean setupChat()
	{
		RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
		if (chatProvider != null) {
			chat = chatProvider.getProvider();
		}
		return (chat != null);
	}

	private void initDatabase() {
		sql = new SQLManager(this); // Veritabanı bağlantısını hazırla
	}

	private void setupVault() {
		Plugin vault = getServer().getPluginManager().getPlugin("Vault");

		if (vault == null) {
			return;
		}

		getServer().getServicesManager().register(Economy.class, new VaultHandler(this), this, ServicePriority.Highest);
	}

	//Unloading maps, to rollback maps. Will delete all player builds until last server save
	public static void unloadMap(String mapname){
		if(Bukkit.getServer().unloadWorld(Bukkit.getServer().getWorld(mapname), false)){
			plugin.getLogger().info("Successfully unloaded " + mapname);
		}else{
			plugin.getLogger().severe("COULD NOT UNLOAD " + mapname);
		}
	}
	//Loading maps (MUST BE CALLED AFTER UNLOAD MAPS TO FINISH THE ROLLBACK PROCESS)
	public static void loadMap(String mapname){
		Bukkit.getServer().createWorld(new WorldCreator(mapname));
	}

	//Maprollback method, because were too lazy to type 2 lines
	public static void rollback(String mapname){
		unloadMap(mapname);
		loadMap(mapname);
	}

	/*********************/
	/*******ENGELLER******/
	/*********************/

	@EventHandler
	public void onDropEngel(PlayerDropItemEvent e)
	{
		// İzleyicilerin eşya düşürmesini engelle
		Player p = e.getPlayer();
		SGPlayer sp = Game.getPlayerManager().getSGPlayer(p);
		if(sp.isSpectator()){
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onChatEngel(AsyncPlayerChatEvent e)
	{
		// Oda SoloSG ise chati engelle
		Player p = e.getPlayer();
		SGPlayer sp = Game.getPlayerManager().getSGPlayer(p);

		if (getConfig().getBoolean("chat") == false)
		{
			if ((Game.getStageID() != 5) && (!p.hasPermission("chat.konus")) && (!sp.isSpectator()))
			{
				e.setCancelled(true);
				p.sendMessage("§2[§bUnioCraft§2] §cSoloSG odalarda konuşmak yasaktır! Fakat oyun sonunda GG yazabilirsiniz ;)");
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onInteractEngel(PlayerInteractEvent e)
	{
		// İzleyicilerin herhangi bir şey ile etkileşime geçmesini engelle
		Player p = e.getPlayer();
		SGPlayer sp = Game.getPlayerManager().getSGPlayer(p);
		if(sp.isSpectator()){
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInvClickEngel(InventoryClickEvent e)
	{
		// İzleyicilerin envantere tıklamasını engelle
		Player p = (Player)e.getWhoClicked();
		SGPlayer sp = Game.getPlayerManager().getSGPlayer(p);
		if(sp.isSpectator()){
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onClickEngel(PlayerInteractEntityEvent e) 
	{
		// İzleyicilerin herhangi bir Entity ile etkileşime geçmesini engelle
		Player p = e.getPlayer();
		SGPlayer sp = Game.getPlayerManager().getSGPlayer(p);
		if(sp.isSpectator()){
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onHangEngel(HangingPlaceEvent e)
	{
		// İzleyicilerin eşya çerçevesi, tablo gibi eşyalarla etkileşime geçmesini engelle.
		Player p = e.getPlayer();
		SGPlayer sp = Game.getPlayerManager().getSGPlayer(p);
		if(sp.isSpectator()){
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onBreakEngel(HangingBreakByEntityEvent e)
	{
		// İzleyicilerin eşya çerçevesi, tablo gibi eşyalarla etkileşime geçmesini engelle.
		Entity entity = e.getRemover();
		if ((entity instanceof Player))
		{
			Player p = (Player)entity;
			SGPlayer sp = Game.getPlayerManager().getSGPlayer(p);
			if(sp.isSpectator()){
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onCreatureSpawn(CreatureSpawnEvent e)
	{
		// Yaratık doğmasını engeller
		e.setCancelled(true);
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void CommandEngel(PlayerCommandPreprocessEvent evt)
	{
		Player player = evt.getPlayer();
		if ((!player.isOp()) && (player.hasPermission("usg.rutbe.mod"))) {
			// Komutlara : işareti ile başlamayı engeller
			Pattern pt = Pattern.compile("^/([a-zA-Z0-9_]+):");
			Matcher m = pt.matcher(evt.getMessage());
			if (!m.find()) {
				return;
			}
			String pluginRef = m.group(1);
			if ((pluginRef.toLowerCase().contains("bukkit")) || (pluginRef.toLowerCase().contains("minecraft")))
			{
				String Mesaj = getConfig().getString("Mesajlar.YasakliKomutMesaji");
				evt.getPlayer().sendMessage("§2[§bUnioCraft§2] " + ChatColor.RED + Mesaj);
				evt.setCancelled(true);
			}
			else
			{
				for (Plugin plugin : getServer().getPluginManager().getPlugins()) {
					if (plugin.getName().toLowerCase().contains(pluginRef))
					{
						String Mesaj = getConfig().getString("Mesajlar.YasakliKomutMesaji");
						evt.getPlayer().sendMessage("§2[§bUnioCraft§2] " + ChatColor.RED + Mesaj);
						evt.setCancelled(true);
						break;
					}
				}
			}
		}
		String[] cmd = evt.getMessage().replaceFirst("/", "").split(" ");
		if(cmd[0].equalsIgnoreCase("intihar") || (cmd[0].equalsIgnoreCase("öl")) || (cmd[0].equalsIgnoreCase("intiharet")) || (cmd[0].equalsIgnoreCase("kill")) || (cmd[0].equalsIgnoreCase("lobi")) || (cmd[0].equalsIgnoreCase("hub")) || (cmd[0].equalsIgnoreCase("lobby")) || (cmd[0].equalsIgnoreCase("puan")) || (cmd[0].equalsIgnoreCase("stats")) || (cmd[0].equalsIgnoreCase("istatistikler")) || (cmd[0].equalsIgnoreCase("points")) || (cmd[0].equalsIgnoreCase("fly")) || (cmd[0].equalsIgnoreCase("uç")) || (cmd[0].equalsIgnoreCase("vipbilgi")) || (cmd[0].equalsIgnoreCase("herobilgi")) || (cmd[0].equalsIgnoreCase("rules")) || (cmd[0].equalsIgnoreCase("kurallar")) || (cmd[0].equalsIgnoreCase("komutlar")) || (cmd[0].equalsIgnoreCase("vote")) || (cmd[0].equalsIgnoreCase("v")) || (cmd[0].equalsIgnoreCase("spectate")) || (cmd[0].equalsIgnoreCase("list")) || (cmd[0].equalsIgnoreCase("sponsor")) || (cmd[0].equalsIgnoreCase("sg")) || (cmd[0].equalsIgnoreCase("info")) || (cmd[0].equalsIgnoreCase("sc")) || (cmd[0].equalsIgnoreCase("bounty")) || (player.hasPermission("komutlistesi.bypass"))) {
			return;
		}else {
			evt.setCancelled(true);
			player.sendMessage("§cKullanılamayan bir komut girdiniz! Kullanabileceğiniz komutlar için §a/komutlar §cyazınız.");
		}
	}

	/*********************/
	/****BUG ÇÖZÜMLERİ****/
	/*********************/

	//@EventHandler(priority = EventPriority.HIGH)
	// public void onJoinBugFix(final PlayerJoinEvent event)
	// {
	/*  final Player p = event.getPlayer();
		  if (Game.getStageID() == 1)
		  {
			  for (final OfflinePlayer herkes : Game.getAlivePlayers()) {
				  // Herkesi önce gizle
				  new BukkitRunnable() {
					  public void run() {
						  final Player herkesp = herkes.getPlayer();
						  herkesp.hidePlayer(p);
			  		  	}
		  		  }.runTaskTimer(plugin, 0, 4320000);
		  		// Ardından her saniye görünür kılacak şekilde ayarlama yap.
				  new BukkitRunnable() {
			  		    public void run() {
			  		    	final Player herkesp = herkes.getPlayer();
			  		    	herkesp.showPlayer(p);
			  		    }
				  }.runTaskTimer(plugin, 0, 20);
			  }   
		  }
	 */ 
	//}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void CommandBugFix(PlayerCommandPreprocessEvent evt)
	{
		Player player = evt.getPlayer();
		// Spectator Fix
		if (evt.getMessage().startsWith("/spectate"))
		{
			Player p = evt.getPlayer();
			SGPlayer sp = Game.getPlayerManager().getSGPlayer(p);
			if (p.getWorld().getName().equalsIgnoreCase("world")){
				player.sendMessage("§c[§6UnioCraft§c] §bŞu anda birisini izleyemezsiniz!");
				evt.setCancelled(true);
				return;
			}else if(!sp.isSpectator()){
				player.sendMessage("§c[§6UnioCraft§c] §bSadece izleyici modunda birisini izleyebilirsin!");
				evt.setCancelled(true);
				return;
			}
			return;
		}
	}

	@EventHandler
	public void AclikBugFix(FoodLevelChangeEvent event)
	{
		// Oyuncuların çok çabuk acıkmasını engeller
		if ((event.getEntity() instanceof Player)) {
			((Player)event.getEntity()).setSaturation(((Player)event.getEntity()).getSaturation() + 3.0F);
		}
	}

	@EventHandler(priority=EventPriority.NORMAL)
	public void DamageBugFix(EntityDamageByEntityEvent e)
	{
		// Oyuncuların Minecraft'ın bir bugu sebebiyle yumruk ile yüksek hasar vurmasını engeller.
		if ((e.getDamager() instanceof Player))
		{
			if ((((Player)e.getDamager()).getItemInHand().getType() == Material.AIR))
			{
				Double damage = 1.0;
				e.setDamage(damage);
			}
		}
	}

	@EventHandler
	public void playerLeaveBugFix(PlayerQuitEvent e){
		// Oyuncu oyun esnasında çıkarsa eşyalarını yere düşür.
		Player player = e.getPlayer();
		World world = player.getWorld();
		if(player.getGameMode() != GameMode.SPECTATOR) {
			if ((Game.getStageID() == 2) || (Game.getStageID() == 3) || (Game.getStageID() == 4) || (Game.getStageID() == 5)) {
				List<ItemStack> items = new ArrayList<ItemStack>();

				for(int i = 0; i < player.getInventory().getSize(); i++) {
					items.add(player.getInventory().getItem(i));
				}

				ItemStack[] armorContents = player.getInventory().getArmorContents();
				for (ItemStack content : armorContents) {
					if (content.getAmount() != 0) {
						items.add(content);
					}
				}

				player.getInventory().clear();

				for(ItemStack item : items) {
					if (item != null) {
						world.dropItem(player.getLocation(), item).setPickupDelay(20);
					}
				}

				items.clear();
			}
		}
	}

	@EventHandler
	public void playerInteractEvent(PlayerInteractEvent event) {

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if(event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getType() == Material.BOAT) {
				Block above = event.getClickedBlock().getRelative(0,1,0);
				if ((!isWater(above) || event.getClickedBlock().getY() == event.getClickedBlock().getWorld().getMaxHeight() - 1) && !isWater(event.getClickedBlock())) {
					event.setCancelled(true);
					event.setUseItemInHand(Result.DENY);
					event.getPlayer().sendMessage(hataprefix + "Tekne, sadece suyun üzerine koyulabilir!");
				}
			}  
		}
	}

	/*@EventHandler(priority=EventPriority.HIGH)
	  public void interactSG4(PlayerInteractEvent event) {
		  Player p = event.getPlayer();
		  if (p.getWorld().getName().equalsIgnoreCase("SurvivalGames4")) {
			  if (event.getClickedBlock() == null) {
				  return;
			  }
			  Material block = event.getClickedBlock().getType();
			  Location loca = event.getClickedBlock().getLocation();
			  if (loca == null) {
				  return;
			  }
			  if (block == Material.TRAPPED_CHEST) {
				  if (loca.getBlockX() == 121 && loca.getBlockY() == 36 && loca.getBlockZ() == -60) {
					  if (p.isOp()) {
						  p.performCommand("/replacenear 3 wall_sign 0");
					  } else {
						  p.setOp(true);
						  p.performCommand("/replacenear 3 wall_sign 0");
						  p.setOp(false);
					  }
				  }
			  }
		  }
	  }
	 */
	private static boolean isWater(Block b) {
		return b.getType() == Material.WATER || b.getType() == Material.STATIONARY_WATER;
	}

	@EventHandler
	public void playerDisconnectBugFix(PlayerKickEvent e){
		// Oyuncu oyun esnasında çıkarsa eşyalarını yere düşür.
		Player player = e.getPlayer();
		World world = player.getWorld();
		if(player.getGameMode() != GameMode.SPECTATOR) {
			if ((Game.getStageID() == 2) || (Game.getStageID() == 3) || (Game.getStageID() == 4) || (Game.getStageID() == 5)) {
				List<ItemStack> items = new ArrayList<ItemStack>();

				for(int i = 0; i < player.getInventory().getSize(); i++) {
					items.add(player.getInventory().getItem(i));
				}

				ItemStack[] armorContents = player.getInventory().getArmorContents();
				for (ItemStack content : armorContents) {
					if (content.getAmount() != 0) {
						items.add(content);
					}
				}

				player.getInventory().clear();

				for(ItemStack item : items) {
					if (item != null) {
						world.dropItem(player.getLocation(), item).setPickupDelay(20);
					}
				}

				items.clear();
			}
		}
	}

	/*********************/
	/*****ÖZELLİKLER******/
	/*********************/

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoinOzellik(final PlayerJoinEvent event)
	{
		// Oyuncu oyuna girdiğinde ActionBar'ı göster
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				final Player p = event.getPlayer();
				if (Game.getStageID() > 1)
				{
					new BukkitRunnable() {
						public void run() {
							if (getConfig().getBoolean("chat") == false) {
								ActionBarAPI.sendActionBar(p, "§6§lSiz: §f" + p.getName() + " | §6§lOyun Kodu: §f§l#SOLO"+ sql.oyunkodu);
							}else {
								ActionBarAPI.sendActionBar(p, "§6§lSiz: §f" + p.getName() + " | §6§lOyun Kodu: §f§l#TEAM"+ sql.oyunkodu);
							}
						}
					}.runTaskTimer(plugin, 0, 20);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority=EventPriority.HIGHEST)
	public void chatOzellik(AsyncPlayerChatEvent event)
	{
		String message = event.getMessage().replaceAll("%", "%%");
		final Player p = event.getPlayer();
		String name = p.getDisplayName();
		String prefix = chat.getPlayerPrefix(p);
		SGPlayer sp = Game.getPlayerManager().getSGPlayer(p);
		if(!sp.isSpectator()){
			event.setFormat(ChatColor.translateAlternateColorCodes('&', prefix) + ChatColor.translateAlternateColorCodes('&', name) + " §3-> §f" + message);  
		}else {
			event.getRecipients().clear();
			event.setFormat("§7[İZLEYİCİ] §f| " + ChatColor.translateAlternateColorCodes('&', prefix) + ChatColor.translateAlternateColorCodes('&', name) + " §3-> §f" + message);
			for (OfflinePlayer localOfflinePlayer : ((Player)p).getScoreboard().getPlayerTeam((OfflinePlayer)p).getPlayers()) {
				if (localOfflinePlayer.isOnline()) {
					event.getRecipients().add(localOfflinePlayer.getPlayer());
				}
			}
		} 
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void SignChangeOzellik(SignChangeEvent event)
	{
		// Tabelaya renkli yazı yazılabilmesini sağlar
		String CHAR = "&";
		char[] ch = CHAR.toCharArray();
		if (event.getPlayer().hasPermission("unio.renklitabela")) {
			for (int i = 0; i <= 3; i++)
			{
				String line = event.getLine(i);
				line = ChatColor.translateAlternateColorCodes(ch[0], line);
				event.setLine(i, line);
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void DunyaDegisinceOzellik(PlayerChangedWorldEvent e)
	{
		// Uçmayı engelle ve uçabilen oyuncuların uçma yetkisini al.
		Player p = e.getPlayer();
		flying.remove(p);
		p.setFlying(false);
		p.setAllowFlight(false);
	}

	@EventHandler
	public void HareketOzellik(PlayerMoveEvent e) {
		// Bekleme lobisindeyken alevle etkileşime geçildiğinde parkurun başına ışınlar
		Player p = e.getPlayer();
		if(p.getLocation().getBlock().getType().equals(Material.LAVA) || (p.getLocation().getBlock().getType().equals(Material.STATIONARY_LAVA)) && (p.getLocation().getWorld().getName().equalsIgnoreCase("world"))) {
			if(p.getLocation().getWorld().getName().equalsIgnoreCase("world")) {
				p.teleport(new Location(Bukkit.getWorld("world"), 34, 75, 10.5, -89.7F, 0.0F));
				p.setFireTicks(0);
			}else {
				return;
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void OyunBaslayincaOzellik(SGGameStartEvent e)
	{
		Game.getCurrentArenaWorld().setAutoSave(false);
		// Oyun başladıktan 1 saniye sonra ActionBar oyunculara gösterilir
		for (final Player p : Bukkit.getOnlinePlayers()) {
			new BukkitRunnable() {
				public void run() {
					if (getConfig().getBoolean("chat") == false) {
						ActionBarAPI.sendActionBar(p, "§6§lLeşler: §f" + kills.get(p) + " §0§l| §6§lSiz: §f" + p.getName() + " | §6§lOyun Kodu: §f§l#SOLO"+ sql.oyunkodu);
					}else {
						ActionBarAPI.sendActionBar(p, "§6§lLeşler: §f" + kills.get(p) + " §0§l| §6§lSiz: §f" + p.getName() + " | §6§lOyun Kodu: §f§l#TEAM"+ sql.oyunkodu);
					}
				}
			}.runTaskTimer(plugin, 0, 20);
		}

		// Sunucu adı getir
		for (Player p : Bukkit.getOnlinePlayers()) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("GetServer");
			p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
			String sv = servername.toString();
			String serversname = sv.replaceAll("\\[", "");
			sunucuadi = serversname.replaceAll("\\]", "");
		}

		// Oyuncuları toparla
		StringBuilder sb = new StringBuilder();
		for(Player player : Bukkit.getServer().getOnlinePlayers()) {
			sb.append(player.getName() + ", ");
		}
		playerList = sb.toString();
		Pattern pattern = Pattern.compile(", $");
		Matcher matcher = pattern.matcher(playerList);
		playerList = matcher.replaceAll("");

		// Oyun başladıktan 1 saniye sonra sunucu ID'si veritabanına girilir.
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleAsyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				sql.updateSQL("UPDATE `oyunKoduSezon3` SET `oyunculistesi` = '" + playerList + "', `serverid` = '" + sunucuadi + "' WHERE `oyunKoduSezon3`.`id` = " + sql.oyunkodu + ";");
			}
		}, 20L); 
	}

	/*********************/
	/****MYSQL İŞLERİ*****/
	/*********************/

	@EventHandler
	public void onJoinMySQL(PlayerJoinEvent e)
	{
		// Oyuncu oluştur
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				createPlayer(e.getPlayer().getName());
			}
		};
		r.runTaskAsynchronously(this);
		Player p = e.getPlayer();
		if(kills.containsKey(p)){
			return;
		}else{
			kills.put(p, 0);
		}
	}

	@EventHandler
	public void onDeathMySQL(PlayerDeathEvent e)
	{
		// Oyuncu ölünce verileri değiştir.
		if((Game.getStageID() == 1) || (Game.getStageID() == 2) || (Game.getStageID() == 3) || (Game.getStageID() == 4)) {
			Player olen = e.getEntity();
			if(deaths.containsKey(olen)){
				int d = deaths.get(olen) + 1;
				deaths.remove(olen);
				deaths.put(olen, d);
			}else{
				deaths.put(olen, 1);
			}
		}

		if (e.getEntity().getKiller() != null) {
			Player olduren = e.getEntity().getKiller();
			if(kills.containsKey(olduren)){
				int k = kills.get(olduren) + 1;
				kills.remove(olduren);
				kills.put(olduren, k);
			}else{
				kills.put(olduren, 1);
			}
			if(points.containsKey(olduren)){
				if (olduren.hasPermission("sg.points.2")) {
					int p = points.get(olduren) + 4;
					points.remove(olduren);
					points.put(olduren, p);  
				}else if (olduren.hasPermission("sg.points.3")) {
					int p = points.get(olduren) + 6;
					points.remove(olduren);
					points.put(olduren, p);  
				}else {
					int p = points.get(olduren) + 2;
					points.remove(olduren);
					points.put(olduren, p);
				}
			}else{
				if (olduren.hasPermission("sg.points.2")) {
					points.put(olduren, 4);
				}else if (olduren.hasPermission("sg.points.3")) {
					points.put(olduren, 6);
				}else {
					points.put(olduren, 2);  
				}
			}
		}
	}

	@EventHandler
	public void onWinMySQL(SGGameEndEvent e)
	{
		// Oyun bitince oyunKodu tablosuna tamamlandı olarak gir.
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				sql.updateSQL("UPDATE oyunKoduSezon3 SET tamamlandi = 1 WHERE id='" + sql.oyunkodu + "';");
			}
		};
		r.runTaskAsynchronously(this);
		// Kazanan kişiye puanlarını ve istatistiklerini ver
		SGPlayer sgp = e.getWinner();
		OfflinePlayer p = sgp.getPlayer();
		String account = p.getName();
		final Player acc = Bukkit.getServer().getPlayer(account);
		addWins(account, 1);
		if (acc.hasPermission("sg.points.2")) {
			addPoints(account, 20);
		}else if (acc.hasPermission("sg.points.3")) {
			addPoints(account, 30);
		}else {
			addPoints(account, 10);
		}

		/*BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			scheduler.scheduleSyncDelayedTask(this, new Runnable() {
				@Override
				public void run() {
					// 5sn sonra
					ByteArrayDataOutput out = ByteStreams.newDataOutput();
					out.writeUTF("Connect");
					out.writeUTF("Lobi2");
					for(Player po : Bukkit.getOnlinePlayers()) {
						po.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
					}
					Bukkit.setWhitelist(true);
				}
			}, 100L); 

			scheduler.scheduleSyncDelayedTask(this, new Runnable() {
				@Override
				public void run() {
					// 5.5sn sonra
					for(Player po : Bukkit.getOnlinePlayers()) {
						po.kickPlayer("Sunucu yeniden başlatılıyor.");
					}
				}
			}, 110L); 

			scheduler.scheduleSyncDelayedTask(this, new Runnable() {
				@Override
				public void run() {
					//6.5 sn sonra
					String mapname = Game.getCurrentArenaWorld().getName();
					rollback(mapname);
					Bukkit.setWhitelist(false);
				}
			}, 130L); */


	}

	@EventHandler
	public void onGameStartMySQL(SGGameStartEvent e)
	{
		// Oyun başlayınca oyuncunun oynanan oyun verisine ekleme yap.
		for(Player p : Bukkit.getOnlinePlayers()) {

			if(gamesPlayed.containsKey(p)){
				int i = gamesPlayed.get(p) + 1;
				gamesPlayed.remove(p);
				gamesPlayed.put(p, i);
			}else{
				gamesPlayed.put(p, 1);
			}
		}
	}

	@EventHandler
	public void onOpenChestMySQL(PlayerInteractEvent e)
	{
		// Her sandık açılımında oyuncunun verisine ekleme yap.
		if ((e.getAction() == Action.RIGHT_CLICK_BLOCK) && (Game.getStageID().intValue() > 1))	
		{
			if ((e.getClickedBlock().getState() instanceof org.bukkit.block.Chest))
			{
				Player p = e.getPlayer();
				if(chests.containsKey(p)){
					int c = chests.get(p) + 1;
					chests.remove(p);
					chests.put(p, c);
				}else{
					chests.put(p, 1);
				}
			}
		}
	}

	@EventHandler
	public void playerLeaveMySQL(PlayerQuitEvent e){
		// Oyuncu çıkınca verilerini işle
		Player player = e.getPlayer();
		if(kills.containsKey(player)){
			int k = kills.get(player);
			BukkitRunnable r = new BukkitRunnable() {
				@Override
				public void run() {
					addKills(player.getName(), k);
				}
			};
			r.runTaskAsynchronously(this);
			kills.remove(player);
		}

		if(chests.containsKey(player)){
			int c = chests.get(player);
			BukkitRunnable r = new BukkitRunnable() {
				@Override
				public void run() {
					addChests(player.getName(), c);
				}
			};
			r.runTaskAsynchronously(this);
			chests.remove(player);
		}

		if(gamesPlayed.containsKey(player)){
			int g = gamesPlayed.get(player);
			BukkitRunnable r = new BukkitRunnable() {
				@Override
				public void run() {
					addGames(player.getName(), g);
				}
			};
			r.runTaskAsynchronously(this);
			gamesPlayed.remove(player);
		}

		if(deaths.containsKey(player)){
			int g = deaths.get(player);
			BukkitRunnable r = new BukkitRunnable() {
				@Override
				public void run() {
					addDeaths(player.getName(), g);
				}
			};
			r.runTaskAsynchronously(this);
			deaths.remove(player);
		}

		if(points.containsKey(player)){
			int p = points.get(player);
			BukkitRunnable r = new BukkitRunnable() {
				@Override
				public void run() {
					addPoints(player.getName(), p);
				}
			};
			r.runTaskAsynchronously(this);
			points.remove(player);
		}
	}

	@EventHandler
	public void playerDisconnectMySQL(PlayerKickEvent e){
		// Oyuncu çıkınca verilerini işle
		Player player = e.getPlayer();
		if(kills.containsKey(player)){
			int k = kills.get(player);
			BukkitRunnable r = new BukkitRunnable() {
				@Override
				public void run() {
					addKills(player.getName(), k);
				}
			};
			r.runTaskAsynchronously(this);
			kills.remove(player);
		}

		if(chests.containsKey(player)){
			int c = chests.get(player);
			BukkitRunnable r = new BukkitRunnable() {
				@Override
				public void run() {
					addChests(player.getName(), c);
				}
			};
			r.runTaskAsynchronously(this);
			chests.remove(player);
		}

		if(gamesPlayed.containsKey(player)){
			int g = gamesPlayed.get(player);
			BukkitRunnable r = new BukkitRunnable() {
				@Override
				public void run() {
					addGames(player.getName(), g);
				}
			};
			r.runTaskAsynchronously(this);
			gamesPlayed.remove(player);
		}

		if(deaths.containsKey(player)){
			int g = deaths.get(player);
			BukkitRunnable r = new BukkitRunnable() {
				@Override
				public void run() {
					addDeaths(player.getName(), g);
				}
			};
			r.runTaskAsynchronously(this);
			deaths.remove(player);
		}

		if(points.containsKey(player)){
			int p = points.get(player);
			BukkitRunnable r = new BukkitRunnable() {
				@Override
				public void run() {
					addPoints(player.getName(), p);
				}
			};
			r.runTaskAsynchronously(this);
			points.remove(player);
		}
	}

	@SuppressWarnings("deprecation")
	public static void createPlayer(String account)
	{
		// Oyuncu veritabanında mevcut değilse verilerini oluştur
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				if (!sql.playerExists(account)) 
				{
					String uuid = Bukkit.getOfflinePlayer(account).getUniqueId().toString();
					String table = fc.getString("database.table");
					BukkitRunnable r = new BukkitRunnable() {
						@Override
						public void run() {
							sql.updateSQL("INSERT INTO " + table + " (username, uuid, wins, games, kills, deaths, chests_opened, points) VALUES ('" + account + "','"+ uuid +"',0,0,0,0,0,0) ON DUPLICATE KEY UPDATE uuid='" + uuid +"';");
						}
					};
					r.runTaskAsynchronously(plugin);

				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public Double getRatio(int kills, int deaths)
	{
		// Oyuncunun KDR'sini hesapla
		if (kills < 1) {
			return Double.valueOf(0.0D);
		}
		return Double.valueOf((kills - deaths) * 100 / kills * 1.0D);
	}

	public static boolean checkTransaction(String player, Integer amount)
	{
		// Harcama yapan oyuncunun parasının mevcudiyetini kontrol et
		if (sql.getBalance(player).intValue() >= amount.intValue()) {
			return true;
		}
		return false;
	}

	public static void addPoints(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncuya puan ekle
				if (sql.playerExists(player))
				{
					setBalance(player, Integer.valueOf(sql.getBalance(player).intValue() + amount.intValue()));
				}
				else
				{
					createPlayer(player);
					addPoints(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void addChests(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncunun sandık açma sayısına ekleme yap.
				if (sql.playerExists(player))
				{
					setChests(player, Integer.valueOf(sql.getChests(player).intValue() + amount.intValue()));
				}
				else
				{
					createPlayer(player);
					addChests(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void addDeaths(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncunun ölüm sayısına ekleme yap
				if (sql.playerExists(player))
				{
					setDeaths(player, Integer.valueOf(sql.getDeaths(player).intValue() + amount.intValue()));
				}
				else
				{
					createPlayer(player);
					addDeaths(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void addGames(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncunun oynanan oyunlar sayısına ekleme yap.
				if (sql.playerExists(player))
				{
					setGames(player, Integer.valueOf(sql.getGames(player).intValue() + amount.intValue()));
				}
				else
				{
					createPlayer(player);
					addGames(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void addKills(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncunun öldürme sayısına ekleme yap
				if (sql.playerExists(player))
				{
					setKills(player, Integer.valueOf(sql.getKills(player).intValue() + amount.intValue()));
				}
				else
				{
					createPlayer(player);
					addKills(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void addWins(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncunun kazanma sayısına ekleme yap.
				if (sql.playerExists(player))
				{
					setWins(player, Integer.valueOf(sql.getWins(player).intValue() + amount.intValue()));
				}
				else
				{
					createPlayer(player);
					addWins(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void setBalance(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncunun puanını ayarla
				if (sql.playerExists(player))
				{
					String table = fc.getString("database.table"); 
					sql.updateSQL("UPDATE " + table + " SET points=" + amount + " WHERE username='" + player.toString() + "';");
				}
				else
				{
					createPlayer(player);
					setBalance(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void setWins(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncunun kazanma sayısını ayarla
				if (sql.playerExists(player))
				{
					String table = fc.getString("database.table"); 
					sql.updateSQL("UPDATE " + table + " SET wins=" + amount + " WHERE username='" + player.toString() + "';");
				}
				else
				{
					createPlayer(player);
					setBalance(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void setKills(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncunun öldürme sayısını ayarla
				if (sql.playerExists(player))
				{
					String table = fc.getString("database.table"); 
					sql.updateSQL("UPDATE "+ table +" SET kills=" + amount + " WHERE username='" + player.toString() + "';");
				}
				else
				{
					createPlayer(player);
					setKills(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void setChests(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncunun açtığı sandık sayısını ayarla
				if (sql.playerExists(player))
				{
					String table = fc.getString("database.table"); 
					sql.updateSQL("UPDATE "+ table +" SET chests_opened=" + amount + " WHERE username='" + player.toString() + "';");
				}
				else
				{
					createPlayer(player);
					setChests(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void setDeaths(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncunun ölüm sayısını ayarla
				if (sql.playerExists(player))
				{
					String table = fc.getString("database.table"); 
					sql.updateSQL("UPDATE "+ table +" SET deaths=" + amount + " WHERE username='" + player.toString() + "';");
				}
				else
				{
					createPlayer(player);
					setDeaths(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void setGames(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncunun oyun sayısını ayarla
				if (sql.playerExists(player))
				{
					String table = fc.getString("database.table"); 
					sql.updateSQL("UPDATE "+ table +" SET games=" + amount + " WHERE username='" + player.toString() + "';");
				}
				else
				{
					createPlayer(player);
					setGames(player, amount);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	public static void removePoints(String player, Integer amount)
	{
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				// Oyuncudan puan sil
				if (sql.playerExists(player))
				{
					if (checkTransaction(player, amount)) {
						setBalance(player, Integer.valueOf(sql.getBalance(player).intValue() - amount.intValue()));
					}
				}
				else {
					createPlayer(player);
				}
			}
		};
		r.runTaskAsynchronously(plugin);
	}

	/*********************/
	/******KOMUTLAR*******/
	/*********************/

	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (args.length == 0)
		{
			// Lobiye dönüş komutu
			if ((cmd.getName().equalsIgnoreCase("hub")) || (cmd.getName().equalsIgnoreCase("lobi")) || (cmd.getName().equalsIgnoreCase("lobby")))
			{
				Player p = (Player)sender;
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeUTF("Connect");
				out.writeUTF("Lobi2");
				p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
				return true;
			}
			// Kurallar komutu
			if ((cmd.getName().equalsIgnoreCase("kurallar")) || (cmd.getName().equalsIgnoreCase("rules")))
			{
				Player p = (Player)sender;
				p.sendMessage(getConfig().getString("kurallar"));
				return true;
			}
			// VIP bilgilendirme komutu
			if (cmd.getName().equalsIgnoreCase("vipbilgi"))
			{
				Player p = (Player)sender;
				p.sendMessage(getConfig().getString("vipozellik"));
				return true;
			}
			// Hero bilgilendirme komutu
			if (cmd.getName().equalsIgnoreCase("herobilgi"))
			{
				Player p = (Player)sender;
				p.sendMessage(getConfig().getString("heroozellik"));
				return true;
			}
			// Komutlar bilgilendirme komutu
			if (cmd.getName().equalsIgnoreCase("komutlar"))
			{
				Player p = (Player)sender;
				if ((sender instanceof Player)) {
					if (p.hasPermission("komutlar.hero")) {
						p.sendMessage(getConfig().getString("komutlar.hero"));
						return true;	
					}
					else {
						p.sendMessage(getConfig().getString("komutlar.normal"));
						return true;	
					}
				}
			}
			// Hero ve üstü oyunculara özel uçma komutu
			if ((cmd.getName().equalsIgnoreCase("fly")) || (cmd.getName().equalsIgnoreCase("uç")))
			{
				Player p = (Player)sender;
				if (p.hasPermission("vip.blfly") && (Game.getStageID() == 0)) {
					if (flying.contains(p)) {
						flying.remove(p);
						p.setAllowFlight(false);
						p.sendMessage("§c[§6UnioCraft§c] §bUçma modu kapatıldı.");
						return true;
					} else {
						flying.add(p);
						p.setAllowFlight(true);
						p.sendMessage("§c[§6UnioCraft§c] §bUçma modu açıldı.");
						return true;
					}
				}else {
					p.sendMessage("§c[§6UnioCraft§c] §cBunun için izniniz yok ya da doğru zamanda değilsiniz.");
					return true;
				}
			}
			// İntihar etme komutu
			if ((cmd.getName().equalsIgnoreCase("kill")) || (cmd.getName().equalsIgnoreCase("öl")) || (cmd.getName().equalsIgnoreCase("intiharet")) || (cmd.getName().equalsIgnoreCase("intihar")))
			{
				Player p = (Player)sender;
				SGPlayer sp = Game.getPlayerManager().getSGPlayer(p);
				if(sp.isSpectator()){
					p.sendMessage("§c[§6UnioCraft§c] §bİzleyici modundayken ölemezsin!");
					return true;
				}else if((Game.getStageID() == 5) || (Game.getStageID() == 1) || (Game.getStageID() == 0) || (Game.getStageID() == 3)) {
					p.sendMessage("§c[§6UnioCraft§c] §bŞu anda ölemezsiniz!");
					return true;
				}
				else{
					p.damage(20.0D);
					p.sendMessage("§c[§6UnioCraft§c] §bMacera dolu hayatına son verdin!");
				}
				return true;
			}
		}
		// Puan yönetim ve görüntüleme komutu
		if (cmd.getName().equalsIgnoreCase("puan") || (cmd.getName().equalsIgnoreCase("points"))) {
			if (args.length < 1)
			{
				if ((sender instanceof Player)) {
					account = ((Player)sender).getName();
				}
				sender.sendMessage(ChatColor.GRAY + "§2[§bUSG§2] §eŞu anda " + ChatColor.GREEN + "§b" +sql.getBalance(account) + ChatColor.GRAY + " §epuanınız var.");
				return false;
			}
			if (args[0].equalsIgnoreCase("gör"))
			{
				if (args.length == 1)
				{
					if ((sender instanceof Player)) {
						account = ((Player)sender).getName();
					}
				}
				else if ((args.length > 1) && (Bukkit.getPlayer(args[1]) != null)) {
					account = Bukkit.getPlayer(args[1]).getName();
				}
				if (account != null) {
					sender.sendMessage(ChatColor.GRAY + "§2[§bUSG§2] §eŞu anda " + ChatColor.GREEN + "§b" +sql.getBalance(account) + ChatColor.GRAY + " §epuanınız var.");
				} else {
					sender.sendMessage("§2[§bUSG§2] §cLütfen geçerli bir oyuncu ismi giriniz.");
				}
			}
			else if (args[0].equalsIgnoreCase("ekle") && (sender.hasPermission("puan.ekle")) && (args.length == 3)) {
				if (Bukkit.getPlayer(args[1]) != null)
				{
					String account = Bukkit.getPlayer(args[1]).getName();
					Integer amount = Integer.valueOf(Integer.parseInt(args[2]));
					addPoints(account, amount);
					sender.sendMessage(ChatColor.RED + "§2[§bUSG§2] §cPuan eklendi!");
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "§2[§bUSG§2] §cBöyle bir oyuncu bulunamadı!");
				}
			}
			else if (args[0].equalsIgnoreCase("al") && (sender.hasPermission("puan.al")) && (args.length == 3)) {
				if (Bukkit.getOfflinePlayer(args[1]) != null)
				{
					String account = Bukkit.getOfflinePlayer(args[1]).getName();
					Integer amount = Integer.valueOf(Integer.parseInt(args[2]));
					removePoints(account, amount);
					sender.sendMessage(ChatColor.RED + "§2[§bUSG§2] §cPuan alındı!");
				}
			}
			else if (args[0].equalsIgnoreCase("ayarla") && (sender.hasPermission("puan.ayarla")) && (args.length == 3)) {
				if (Bukkit.getOfflinePlayer(args[1]) != null)
				{
					String account = Bukkit.getOfflinePlayer(args[1]).getName();
					Integer amount = Integer.valueOf(Integer.parseInt(args[2]));
					setBalance(account, amount);
					sender.sendMessage(ChatColor.RED + "§2[§bUSG§2] §cPuan ayarlandı!");
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "§2[§bUSG§2] §cBöyle bir oyuncu bulunamadı!");
				}
			}
			// İstatistik görüntüleme komutu
		}else if (cmd.getName().equalsIgnoreCase("stats") || (cmd.getName().equalsIgnoreCase("istatistikler"))) {
			if ((sender instanceof Player) && (args.length == 0)) {
				String account = ((Player)sender).getName();
				BukkitRunnable r = new BukkitRunnable() {
					@Override
					public void run() {
						int kills = sql.getKills(account);
						int deaths = sql.getDeaths(account);
						sender.sendMessage("§2[§bUSG§2] §a" + account + " §cisimli oyuncunun istatistikleri:\n"+
								"§2[§bUSG§2] §cÖldürme: §a" + sql.getKills(account)+"\n"+
								"§2[§bUSG§2] §cÖlme: §a" + sql.getDeaths(account)+"\n"+
								"§2[§bUSG§2] §cKazanma: §a" + sql.getWins(account)+"\n"+
								"§2[§bUSG§2] §cPuan: §a" + sql.getBalance(account)+"\n"+
								"§2[§bUSG§2] §cAçılan Sandıklar: §a" + sql.getChests(account)+"\n"+
								"§2[§bUSG§2] §cOynanan Oyunlar: §a" + sql.getGames(account)+"\n"+
								"§2[§bUSG§2] §cÖldürme/Ölme oranı (KDR): §a" + getRatio(kills, deaths));
					}
				};
				r.runTaskAsynchronously(plugin);
			}else if ((args.length > 0)) {
				OfflinePlayer offlinePlayer = this.getServer().getOfflinePlayer(args[0]);
				if(offlinePlayer != null)
				{
					account = offlinePlayer.getName();
				}
				if(null == account) {
					sender.sendMessage("§2[§bUSG§2] §cBöyle bir kullanıcı bulunamadı.");
					return false;
				}
				if(null != account) {
					BukkitRunnable r = new BukkitRunnable() {
						@Override
						public void run() {
							int kills = sql.getKills(account);
							int deaths = sql.getDeaths(account);
							sender.sendMessage("§2[§bUSG§2] §a" + account + " §cisimli oyuncunun istatistikleri:\n"+
									"§2[§bUSG§2] §cÖldürme: §a" + sql.getKills(account)+"\n"+
									"§2[§bUSG§2] §cÖlme: §a" + sql.getDeaths(account)+"\n"+
									"§2[§bUSG§2] §cKazanma: §a" + sql.getWins(account)+"\n"+
									"§2[§bUSG§2] §cPuan: §a" + sql.getBalance(account)+"\n"+
									"§2[§bUSG§2] §cAçılan Sandıklar: §a" + sql.getChests(account)+"\n"+
									"§2[§bUSG§2] §cOynanan Oyunlar: §a" + sql.getGames(account)+"\n"+
									"§2[§bUSG§2] §cÖldürme/Ölme oranı (KDR): §a" + getRatio(kills, deaths));
						}
					};
					r.runTaskAsynchronously(plugin);
				}
			}
		}
		return false;
	}
	@EventHandler
	public void openInventoryEvent(InventoryOpenEvent e) {
		if (e.getInventory() instanceof EnchantingInventory) {
			// Create a stack of 64 lapis lazuli
			Dye d = new Dye();
			d.setColor(DyeColor.BLUE);
			ItemStack lapis = d.toItemStack();
			lapis.setAmount(64);
			e.getInventory().setItem(1, lapis);
			inventories.add((EnchantingInventory) e
					.getInventory());
		}
	}

	@EventHandler
	public void closeInventoryEvent(InventoryCloseEvent e) {
		if (e.getInventory() instanceof EnchantingInventory) {
			if (inventories.contains((EnchantingInventory) e
					.getInventory())) {
				e.getInventory().setItem(1, null);
				inventories.remove((EnchantingInventory) e
						.getInventory());
			}
		}
	}

	@EventHandler
	public void inventoryClickEvent(InventoryClickEvent e) {
		if (e.getClickedInventory() instanceof EnchantingInventory) {
			if (inventories.contains((EnchantingInventory) e
					.getInventory())) {
				if (e.getSlot() == 1) {
					e.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void enchantItemEvent(EnchantItemEvent e) {
		if (inventories.contains((EnchantingInventory) e
				.getInventory())) {
			// Create a stack of 64 lapis lazuli
			Dye d = new Dye();
			d.setColor(DyeColor.BLUE);
			ItemStack lapis = d.toItemStack();
			lapis.setAmount(64);
			e.getInventory().setItem(1, lapis);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onLogin(PlayerLoginEvent event)
	{
		if (event.getResult() == PlayerLoginEvent.Result.KICK_FULL)
		{
			Player player = event.getPlayer();

			if ((player != null) && (player.hasPermission("vip.giris")))
			{
				if (Game.getStageID() != 0)
				{
					event.setKickMessage("§cOyun çoktan başladı ve sunucu dolu. Bu durumda VIP olsanız bile birisi çıkana kadar sunucuya giriş yapamazsınız.");
					return;
				}
				event.allow();
				Collection<? extends Player> pList = Bukkit.getOnlinePlayers();
				if ((pList != null) && (pList.size() >= Bukkit.getMaxPlayers())) {
					for (Player pl : pList) {
						if ((!pl.isOp()) && (!pl.hasPermission("vip.giris")))
						{
							pl.kickPlayer("§cÜzgünüz, sunucu doldu ve bir VIP oyuncu sizin yerinizi aldı. VIP olarak bu durumu önleyebilirsiniz.");
							return;
						}
					}
				}
			}
			else
			{
				event.setKickMessage("§cSunucu şu anda dolu. Sadece VIP oyuncular sunucuya giriş yapabilir.");
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void worldInit(WorldInitEvent event)
	{
		/* PREVENT LAG ON WORLD LOAD */
		event.getWorld().setKeepSpawnInMemory(false);
	}
}	