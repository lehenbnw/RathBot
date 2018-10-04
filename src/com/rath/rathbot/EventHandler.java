
package com.rath.rathbot;

import com.rath.rathbot.log.MessageLogger;

import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

/**
 * This class handles bot events, such as reading messages and responsing to them.
 * 
 * @author Tim Backus tbackus127@gmail.com
 *
 */
public class EventHandler implements IListener<MessageReceivedEvent> {
  
  /** The prefix for all commands. */
  private static final String COMMAND_PREFIX = "rb!";
  
  /**
   * Message received handler.
   * 
   * @param event contains event details.
   */
  @Override
  public void handle(MessageReceivedEvent event) {
    
    DBG.pl("Message receieved.");
    
    // Let's not respond to bots
    final IUser author = event.getAuthor();
    if (author.isBot()) {
      return;
    }
    
    // Parse commands if it starts with the command prefix
    final IMessage message = event.getMessage();
    final String messageString = message.getContent();
    if (messageString.startsWith(COMMAND_PREFIX)) {
      
      // TODO: Spin up a new thread here so we can accept new commands while we process, unless that's already handled
      // by this method.
      CommandParser.parseCommand(message);
    }
    
    // Log the message after performing the action
    MessageLogger.logMessage(message);
  }
  
  /**
   * Prints message details.
   * 
   * @param evt contains event details.
   */
  @SuppressWarnings("unused")
  private final static void logMessageDetailsToConsole(final MessageReceivedEvent evt) {
    final String author = evt.getAuthor().getName();
    final String channel = evt.getChannel().getName();
    final String message = evt.getMessage().getContent();
    System.out.println("Received message from " + author + " in #" + channel + ":");
    System.out.println("\"" + message + "\"");
  }
}
