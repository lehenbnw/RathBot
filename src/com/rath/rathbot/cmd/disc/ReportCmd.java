
package com.rath.rathbot.cmd.disc;

import java.time.Instant;

import com.rath.rathbot.RBConfig;
import com.rath.rathbot.RathBot;
import com.rath.rathbot.action.ActionReport;
import com.rath.rathbot.cmd.RBCommand;
import com.rath.rathbot.log.ActionLogger;
import com.rath.rathbot.util.MessageHelper;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

/**
 * Provides a report command for users to report rule breaking activity, posts the report in server #report channel.
 * 
 * @author Nathan Lehenbauer lehenbnw@gmail.com
 *
 */

public class ReportCmd extends RBCommand {
  
  @Override
  public String getCommandName() {
    return "report";
  }
  
  @Override
  public String getCommandDescription() {
    return "**REQUIRES A DIRECT MESSAGE** Reports a user to the moderation team with a reason for the report. "
        + "You will need the member's internal ID. Get it using the \"uid <Discord username>\" command. "
        + "The uid command also requires a direct message."
        + "Note that this is NOT their nickname for the server, but the username you see when you open their profile."
        + "For example, Kami's Discord username is \"Loli no Kami\".";
  }
  
  @Override
  public String getCommandUsage() {
    return "rb! report <uid> <reason..>";
  }
  
  @Override
  public int permissionLevelRequired() {
    return RBCommand.PERM_MINIMAL;
  }
  
  @Override
  public boolean requiresDirectMessage() {
    return true;
  }
  
  @Override
  public boolean executeCommand(final IMessage msg, final String[] tokens, final int tokenDepth) {
    
    final IChannel channel = msg.getChannel();
    final IUser issuedUser = msg.getAuthor();
    
    // Ensures at least minimum valid arguments used.
    if (tokens.length < 4) {
      RathBot.sendDirectMessage(issuedUser, "Syntax Error! Usage: " + this.getCommandUsage());
      return RBCommand.STOP_CMD_SEARCH;
    }
    
    // Create IUser object from token to issue disciplinary action on them.
    final IUser infringingUser = MessageHelper.getUserFromToken(tokens[tokenDepth + 1], channel);
    if (infringingUser == null) {
      RathBot.sendMessage(channel,
          "The given username or user ID was not found. Ensure that you've entered the member's username or user ID correctly.");
      return RBCommand.STOP_CMD_SEARCH;
    }
    
    // Message user for confirmation that report is being filed successfully.
    System.out.println("Filing report...");
    RathBot.sendDirectMessage(issuedUser, "Thank you, your report against " + infringingUser.getName()
        + " has been filed. We will look into this and take action accordingly.");
    
    Instant timestamp = msg.getTimestamp();
    
    // Log Report
    ActionLogger.logAction(new ActionReport(timestamp, issuedUser, infringingUser));
    System.out.println("Report filed and logged in */logs/actions.txt");
    
    final IChannel report = RathBot.getClient().getChannelByID(RBConfig.getReportChannelID());
    if (report == null) {
      System.err.println("client.getChannelByID(ReportCmd.REPORT_CHANNEL_ID) returned null in ReportCmd.java");
      return RBCommand.STOP_CMD_SEARCH;
    }
    
    final String reason = MessageHelper.concatenateTokens(tokens, tokenDepth + 2);
    
    // Posts report in #reports.
    RathBot.sendMessage(report, "User " + infringingUser.getName() + " was reported by " + issuedUser.getName() + " at "
        + timestamp + ".\nReason: " + reason);
    
    return RBCommand.STOP_CMD_SEARCH;
  }
}
