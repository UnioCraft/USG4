package net.uniodex.USG4;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLManager {
	 
    private final ConnectionPoolManager pool;
    public Integer oyunkodu;
 
    public SQLManager(Main plugin) {
    	pool = new ConnectionPoolManager(plugin);
    }
    
	  public void oyunKodugir()
	  {
		  final long unixTime = System.currentTimeMillis() / 1000L;
		  try ( Connection connection = pool.getConnection() ) {
			  PreparedStatement statement = connection.prepareStatement("INSERT INTO `sg`.`oyunKoduSezon3` (`id`, `tarih`, `oyunculistesi`, `serverid`, `tamamlandi`) VALUES (NULL, '" + unixTime + "', '', '', '0');", new String[] { "id" });
			  if (statement.executeUpdate() > 0) {
				  ResultSet generatedKeys = statement.getGeneratedKeys();
				  if (null != generatedKeys && generatedKeys.next()) {
					  oyunkodu = generatedKeys.getInt(1);
				  }
			  }
		  } catch (SQLException e) {
			  e.printStackTrace();
		  }
	  }
	  
	  public void updateSQL(String QUERY)
	  {
		  try ( Connection connection = pool.getConnection() ) {
			  PreparedStatement statement = connection.prepareStatement(QUERY);
			  statement.execute();
		  } catch (SQLException e) {
			  e.printStackTrace();
		  }
	  }
	  
	  public boolean playerExists(String account)
	  {
		  String table = Main.fc.getString("database.table"); 
	      String QUERY = "SELECT * FROM " + table + " WHERE username = '" + account.toString() + "';";
		  try ( Connection connection = pool.getConnection() ) {
			  PreparedStatement statement = connection.prepareStatement(QUERY);
			  ResultSet res = statement.executeQuery();
			  if (res.next())
			  {
				  if (res.getString("username") == null) {
					  return false;
				  }
				  return true;
			  }
		  } catch (SQLException e) {
			  e.printStackTrace();
		  }
		return false;
	  }
	  
	  public Integer getBalance(String account)
	  {
		  Integer retval = null;
		  if (playerExists(account))
		  {
			  String table = Main.fc.getString("database.table"); 
			  String QUERY = "SELECT * FROM " + table + " WHERE username = '" + account.toString() + "';";
			  try ( Connection connection = pool.getConnection() ) {
				  PreparedStatement statement = connection.prepareStatement(QUERY);
				  ResultSet res = statement.executeQuery();
				  if ((res.next()) && 
						  (Integer.valueOf(res.getInt("points")) != null)) {
					  retval = Integer.valueOf(res.getInt("points"));
				  }
			  } catch (SQLException e) {
				  e.printStackTrace();
			  }
		  }
		  else
		  {
			  Main.createPlayer(account);
			  getBalance(account);
		  }
	      return retval;
	    }
	    

	    public Integer getWins(String account)
	    {
	    	Integer retval = null;
	    	if (playerExists(account))
	    	{
	    		String table = Main.fc.getString("database.table"); 
	    		String QUERY = "SELECT * FROM " + table + " WHERE username = '" + account.toString() + "';";
	    		try ( Connection connection = pool.getConnection() ) {
	    			PreparedStatement statement = connection.prepareStatement(QUERY);
	    			ResultSet res = statement.executeQuery();
	    			if ((res.next()) && 
	    					(Integer.valueOf(res.getInt("wins")) != null)) {
	    				retval = Integer.valueOf(res.getInt("wins"));
	    			}
	    		} catch (SQLException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    	else
	    	{
	    		Main.createPlayer(account);
	    		getBalance(account);
	    	}
	    	return retval;
	    }
	    
	    public  Integer getGames(String account)
	    {
	    	Integer retval = null;
	    	if (playerExists(account))
	    	{
	    		String table = Main.fc.getString("database.table"); 
	    		String QUERY = "SELECT * FROM " + table + " WHERE username = '" + account.toString() + "';";
	    		try ( Connection connection = pool.getConnection() ) {
	    			PreparedStatement statement = connection.prepareStatement(QUERY);
	    			ResultSet res = statement.executeQuery();
	    			if ((res.next()) && 
	    					(Integer.valueOf(res.getInt("games")) != null)) {
	    				retval = Integer.valueOf(res.getInt("games"));
	    			}
	    		} catch (SQLException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    	else
	    	{
	    		Main.createPlayer(account);
	    		getBalance(account);
	    	}
	    	return retval;
	    }
	    
	    public Integer getChests(String account)
	    {
	    	Integer retval = null;
	    	if (playerExists(account))
	    	{
	    		String table = Main.fc.getString("database.table"); 
	    		String QUERY = "SELECT * FROM " + table + " WHERE username = '" + account.toString() + "';";
	    		try ( Connection connection = pool.getConnection() ) {
	    			PreparedStatement statement = connection.prepareStatement(QUERY);
	    			ResultSet res = statement.executeQuery();
	    			if ((res.next()) && 
	    					(Integer.valueOf(res.getInt("chests_opened")) != null)) {
	    				retval = Integer.valueOf(res.getInt("chests_opened"));
	    			}
	    		} catch (SQLException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    	else
	    	{
	    		Main.createPlayer(account);
	    		getBalance(account);
	    	}
	    	return retval;
	    }
	    
	    public Integer getKills(String account)
	    {
	    	Integer retval = null;
	    	if (playerExists(account))
	    	{
	    		String table = Main.fc.getString("database.table"); 
	    		String QUERY = "SELECT * FROM " + table + " WHERE username = '" + account.toString() + "';";
	    		try ( Connection connection = pool.getConnection() ) {
	    			PreparedStatement statement = connection.prepareStatement(QUERY);
	    			ResultSet res = statement.executeQuery();
	    			if ((res.next()) && 
	    					(Integer.valueOf(res.getInt("kills")) != null)) {
	    				retval = Integer.valueOf(res.getInt("kills"));
	    			}
	    		} catch (SQLException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    	else
	    	{
	    		Main.createPlayer(account);
	    		getBalance(account);
	    	}
	    	return retval;
	    }
	    
	    public Integer getDeaths(String account)
	    {
	    	Integer retval = null;
	    	if (playerExists(account))
	    	{
	    		String table = Main.fc.getString("database.table"); 
	    		String QUERY = "SELECT * FROM " + table + " WHERE username = '" + account.toString() + "';";
	    		try ( Connection connection = pool.getConnection() ) {
	    			PreparedStatement statement = connection.prepareStatement(QUERY);
	    			ResultSet res = statement.executeQuery();
	    			if ((res.next()) && 
	    					(Integer.valueOf(res.getInt("deaths")) != null)) {
	    				retval = Integer.valueOf(res.getInt("deaths"));
	    			}
	    		} catch (SQLException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    	else
	    	{
	    		Main.createPlayer(account);
	    		getBalance(account);
	    	}
	    	return retval;
	    }
	    
	    public void onDisable() {
	        pool.closePool();
	    }
 
}