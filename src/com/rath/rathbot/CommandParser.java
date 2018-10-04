
package com.rath.rathbot;

import com.rath.rathbot.cmd.PermissionsTable;
import com.rath.rathbot.cmd.RBCommand;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;

/**
 * This class handles parsing and dispatching of commands.
 * 
 * @author Tim Backus tbackus127@gmail.com
 */
public class CommandParser {
  
  /**
   * Parses and dispatches a command to the proper methods.
   * 
   * @param message the IMessage object containing the message contents, its author, etc.
   */
  public static final void parseCommand(final IMessage message) {
    
    DBG.pl("Parsing command.");
    
    final String msgString = message.getContent();
    final IUser author = message.getAuthor();
    final IChannel channel = message.getChannel();
    
    System.out.println("Received command: \"" + msgString + "\".");
    
    // Split into tokens separated by spaces, ignoring spaces between quotes
    final String[] tokens = msgString.split("\"?( |$)(?=(([^\"]*\"){2})*[^\"]*$)\"?");
    
    // Ensure the message isn't just the prefix
    if (tokens.length <= 1) {
      RathBot.sendMessage(channel, "Invalid command. Type \"rb! help\" for a list of commands.");
      return;
    }
    
    // Extract the command name and command reference
    DBG.pl("Extracting command.");
    final String cmdName = tokens[1].trim().toLowerCase();
    final RBCommand cmd = RathBot.getCommandMap().get(cmdName);
    
    // If no command exists
    if (cmd == null) {
      RathBot.sendMessage(channel, "Invalid command. Type \"rb! help\" for a list of commands.");
      return;
    }
    
    // If the user doesn't have an entry on the permissions table, initialize them and save
    DBG.pl("Checking permissions.");
    final long userID = author.getLongID();
    if (!PermissionsTable.hasUser(userID)) {
      PermissionsTable.initUser(userID);
      System.out.println("Initialized " + author.getName() + " in perm table.");
    }
    
    // Check permissions for this command.
    if (PermissionsTable.getLevel(userID) >= cmd.permissionLevelRequired()) {
      
      // If a PM-only command was issued in a public channel
      if (cmd.requiresDirectMessage() && !channel.isPrivate()) {
        
        // Delete the message and post a notification.
        try {
          message.delete();
        } catch (DiscordException dce) {
          System.err.println("There was an error deleting the message. Ensure that permissions are set correctly."
              + "If the message was deleted by the mod team before the bot had a chance to, ignore this error.");
          dce.printStackTrace();
        }
        
        // Let the user know that the command requires a direct message.
        RathBot.sendMessage(channel, "This command can only be issued in a direct message to RathBot.");
        return;
      }
      
      // Execute the command that matches
      cmd.executeCommand(message, tokens, 1);
      
    } else {
      
      // Log and notify the author that they don't have the required permissions level
      System.out.println(
          "User " + author.getName() + " tried to execute " + cmd.getCommandName() + " with permission level "
              + PermissionsTable.getLevel(userID) + " (" + cmd.permissionLevelRequired() + " required).");
      RathBot.sendMessage(channel, "You do not have the required permissions for that command.");
    }
    
  }
}
