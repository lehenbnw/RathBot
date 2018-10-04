
package com.rath.rathbot.task.time;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

// Use ZonedDateTime.now(ZoneId.of(INSERT_ISSUERS_TZ_HERE).withZoneSameInstant(ZoneId.of("America/New_York"));
// LocalDateTime.atZone(ZoneId)

/**
 * This class handles all time configurations that happen at specific points in time. e.g.: at February 10, 2019 at 6:00
 * PM CST.
 * 
 * @author Tim Backus tbackus127@gmail.com
 *
 */
public class AbsoluteTimeConfiguration extends TimeConfiguration {
  
  /** Choose spaces before and after the words "at" and "on". */
  private static final String REGEX_SPLIT_CLAUSES = "((?<=at)\\s+)|(\\s+(?=at))|((?<=on)\\s+)|(\\s+(?=on))";
  
  /** Choose spaces between hours, minutes, and optional AM/PM. */
  private static final String REGEX_SPLIT_AT_CLAUSE = "((?<=\\d\\d)\\s*(?=\\d\\d))|(:(?=\\d\\d))|((?<=1?\\d)\\s*(?=[AaPp][Mm]))";
  
  /** Choose any spaces not between square brackets. */
  private static final String REGEX_SPLIT_ON_CLAUSE = "(\\s+(?![^\\[]*\\]))|(\\s*/\\s*)";
  
  /** Match a sequence of comma-separated numbers inside of square brackets. */
  private static final String REGEX_MATCH_BRACKET_LIST = "\\[((\\d+)|(\\w{3}))(\\s*,\\s*((\\d+)|(\\w{3})))*\\]";
  
  /** The minimum year that can be specified. */
  private static final int YEAR_MIN = 2018;
  
  /** The maximum year that can be specified. */
  private static final int YEAR_MAX = 2030;
  
  /** The minimum day that can be specified. */
  private static final int DAY_MIN = 1;
  
  /** The maximum day that can be specified. */
  private static final int DAY_MAX = 31;
  
  /** The minimum month that can be specified. */
  private static final int MONTH_MIN = 1;
  
  /** The maximum month that can be specified. */
  private static final int MONTH_MAX = 12;
  
  /** The minimum hour that can be specified for 24-hour time. */
  private static final int HOUR_24_MIN = 0;
  
  /** The maximum hour that can be specified for 24-hour time. */
  private static final int HOUR_24_MAX = 23;
  
  /** The minimum hour that can be specified for 12-hour time. */
  private static final int HOUR_12_MIN = 1;
  
  /** The maximum hour that can be specified for 12-hour time. */
  private static final int HOUR_12_MAX = 12;
  
  /** The minimum minute that can be specified. */
  private static final int MINUTE_MIN = 0;
  
  /** The maximum minute that can be specified. */
  private static final int MINUTE_MAX = 59;
  
  private static final TreeMap<String, Integer> MONTH_ALIASES = new TreeMap<String, Integer>() {
    
    private static final long serialVersionUID = 1L;
    
    {
      put("jan", 1);
      put("feb", 2);
      put("mar", 3);
      put("apr", 4);
      put("may", 5);
      put("jun", 6);
      put("jul", 7);
      put("aug", 8);
      put("sep", 9);
      put("oct", 10);
      put("nov", 11);
      put("dec", 12);
    }
    
  };
  
  /** A {@link String} that is equal to an asterisk character for the months list argument in the on-clause. */
  private static final String ASTERISK_MONTHS = generateAsteriskString(MONTH_MIN, MONTH_MAX);
  
  /** A {@link String} that is equal to an asterisk character for the days list argument in the on-clause. */
  private static final String ASTERISK_DAYS = generateAsteriskString(DAY_MIN, DAY_MAX);
  
  /** A {@link String} that is equal to an asterisk character for the years list argument in the on-clause. */
  private static final String ASTERISK_YEARS = generateAsteriskString(YEAR_MIN, YEAR_MAX);
  
  /** The time zone that the bot's local time will be converted from. */
  private final ZoneId fromTimeZone;
  
  /** The list of month numbers this time configuration is active for. */
  private ArrayList<Integer> monthList = null;
  
  /** The list of day numbers this time configuration is active for. */
  private ArrayList<Integer> dayList = null;
  
  /** The list of year numbers this time configuration is active for. */
  private ArrayList<Integer> yearList = null;
  
  private int monthPos = -1;
  private int dayPos = -1;
  private int yearPos = -1;
  
  private int hour = -1;
  private int minute = -1;
  
  /**
   * Constructs a new AbsoluteTimeConfiguration object.
   * 
   * @param configString The {@link String} containing the at-clause and/or on-clause (command and to-clause are not
   *        included).
   * @param timeZone The time zone this AbsoluteTimeConfiguration will be converted from as a {@link ZoneId}.
   * 
   * @throws BadTimeConfigException when the configString parameter is invalid.
   */
  public AbsoluteTimeConfiguration(final String configString, final ZoneId timeZone) throws BadTimeConfigException {
    super(TimeConfigurationType.ABSOLUTE, configString);
    
    this.fromTimeZone = timeZone;
    
    // At least 4 tokens are required ("on"/"at", on/at-clause, "to", to-clause)
    final String[] clauseTokens = configString.split(REGEX_SPLIT_CLAUSES);
    if (clauseTokens == null || clauseTokens.length < 2) {
      throw new BadTimeConfigException();
    }
    
    // Parse both at/on-clauses
    for (int i = 0; i < clauseTokens.length; i += 2) {
      if (clauseTokens[i].equals("at")) {
        parseAtClause(clauseTokens[i + 1]);
      } else if (clauseTokens[i].equals("on")) {
        parseOnClause(clauseTokens[i + 1]);
      }
    }
    
    // Check if every field was initialized; if not, throw an exception
    if (this.monthPos == -1 || this.dayPos == -1 || this.yearPos == -1 || this.monthList == null || this.dayList == null
        || this.yearList == null) {
      throw new BadTimeConfigException();
    }
    
  }
  
  private static final String generateAsteriskString(final int min, final int max) {
    String result = "[" + min;
    for (int i = min + 1; i <= max; i++) {
      result += "," + i;
    }
    return result;
  }
  
  /**
   * Splits the at-clause into tokens and dispatches control to the proper handling method.
   * 
   * @param atClause The at-clause as a String.
   */
  private final void parseAtClause(final String atClause) {
    
    // Split the at-clause into tokens and handle each
    final String[] atTokens = atClause.split(REGEX_SPLIT_AT_CLAUSE);
    if (atTokens == null || atTokens.length < 2 || atTokens.length > 3) {
      throw new BadTimeConfigException();
    }
    
    // Military or hour-only ({19, 00} or {7, PM})
    if (atTokens.length == 2) {
      
      // If the 2nd letter of the 2nd token is an M, it's in hour-only format
      if (atTokens[1].toLowerCase().charAt(1) == 'm') {
        handleHourOnly(atTokens[0], atTokens[1]);
      } else {
        handleMilitary(atTokens[0], atTokens[1]);
      }
      
      // Standard
    } else {
      handleStandard(atTokens[0], atTokens[1], atTokens[2]);
    }
    
  }
  
  /**
   * Handles hour-only time specification for the at-clause. e.g.: "7pm". The minutes field will be set to zero, so the
   * task will take place on the hour.
   * 
   * @param hourString the {@link String} containing the hour.
   * @param amPmString the {@link String} that determines if the hour will be interpreted as AM or PM.
   */
  private final void handleHourOnly(final String hourString, final String amPmString) {
    
    // Parse the hour string
    int h = -1;
    try {
      h = Integer.parseInt(hourString);
    } catch (@SuppressWarnings("unused") NumberFormatException nfe) {
      throw new BadTimeConfigException();
    }
    
    // Check out of bounds
    if (h < HOUR_12_MIN || h > HOUR_12_MAX) {
      throw new BadTimeConfigException();
    }
    
    // Convert to 24-hour time
    if (amPmString.toLowerCase().charAt(0) == 'p') {
      h = (h + HOUR_12_MAX) % (HOUR_24_MAX + 1);
    }
    
    // Set time fields
    this.hour = h;
    this.minute = MINUTE_MIN;
  }
  
  /**
   * Handles military time or 24-hour time for the at-clause. e.g.: "1530".
   * 
   * @param hourString the {@link String} containing the hour.
   * @param minutesString the {@link String} containing the minute.
   */
  private final void handleMilitary(final String hourString, final String minutesString) {
    
    // Parse the hour and minute strings
    int h = -1;
    int m = -1;
    try {
      h = Integer.parseInt(hourString);
      m = Integer.parseInt(minutesString);
    } catch (@SuppressWarnings("unused") NumberFormatException nfe) {
      throw new BadTimeConfigException();
    }
    
    // Check out of bounds
    if (h < HOUR_24_MIN || h > HOUR_24_MAX || m < MINUTE_MIN || m > MINUTE_MAX) {
      throw new BadTimeConfigException();
    }
    
    // Set time fields
    this.hour = h;
    this.minute = m;
    
  }
  
  /**
   * Handles standard American time for the at-clause. e.g.: "7:45 PM".
   * 
   * @param hourString the {@link String} containing the hour.
   * @param minutesString the {@link String} containing the minute.
   * @param amPmString the {@link String} that determines if the hour will be interpreted as AM or PM.
   */
  private final void handleStandard(final String hourString, final String minutesString, final String amPmString) {
    
    // Parse the hour and minute strings
    int h = -1;
    int m = -1;
    try {
      h = Integer.parseInt(hourString);
      m = Integer.parseInt(minutesString);
    } catch (@SuppressWarnings("unused") NumberFormatException nfe) {
      throw new BadTimeConfigException();
    }
    
    // Check out of bounds
    if (h < HOUR_12_MIN || h > HOUR_12_MAX || m < MINUTE_MIN || m > MINUTE_MAX) {
      throw new BadTimeConfigException();
    }
    
    // Convert to 24-hour time
    if (amPmString.toLowerCase().charAt(0) == 'p') {
      h = (h + HOUR_12_MAX) % (HOUR_24_MAX + 1);
    }
    
    // Set time fields
    this.hour = h;
    this.minute = m;
    
  }
  
  /**
   * Splits the on-clause into its three tokens and calls each token's handling function.
   * 
   * @param onClause the clause containing the date(s) to execute the task on.
   */
  private final void parseOnClause(final String onClause) {
    
    // Split the at-clause into tokens and handle each
    final String[] onTokens = onClause.split(REGEX_SPLIT_ON_CLAUSE);
    if (onTokens == null || onTokens.length != 3) {
      throw new BadTimeConfigException();
    }
    
    handleMonths(onTokens[0].trim());
    handleDays(onTokens[1].trim());
    handleYears(onTokens[2].trim());
    
  }
  
  /**
   * Handles populating the month list from the months argument of the on-clause.
   * 
   * @param monthsString a {@link String} containing a single month; a square bracket-enclosed, comma-separated list of
   *        month numbers or three-letter abbreviations; or an asterisk.
   */
  private final void handleMonths(String monthsString) {
    
    this.monthList = new ArrayList<Integer>();
    
    // Convert months argument to a square bracket list
    String[] monthTokens = null;
    if (!monthsString.matches(REGEX_MATCH_BRACKET_LIST)) {
      
      // Translate the asterisk to its alias
      if (monthsString.equals("*")) {
        
        monthsString = ASTERISK_MONTHS;
        
      } else {
        
        // Otherwise, parse as a single value and surround in square brackets
        int m = -1;
        try {
          m = Integer.parseInt(monthsString);
        } catch (@SuppressWarnings("unused") NumberFormatException nfe) {
          throw new BadTimeConfigException();
        }
        
        monthsString = "[" + m + "]";
        
      }
      
    }
    
    // Trim square brackets and split on comma
    monthsString = monthsString.substring(1, monthsString.length() - 1);
    monthTokens = monthsString.split("\\s*,\\s*");
    
    // Convert each index to an integer and add it to the months list
    for (final String monthTok : monthTokens) {
      
      int m = -1;
      
      // Check if the current token is a three-letter month abbreviation
      if (MONTH_ALIASES.containsKey(monthTok.toLowerCase())) {
        m = MONTH_ALIASES.get(monthTok);
      } else {
        
        // If not, parse it as a number
        try {
          m = Integer.parseInt(monthTok);
        } catch (@SuppressWarnings("unused") NumberFormatException nfe) {
          throw new BadTimeConfigException();
        }
        
      }
      
      // Check out of bounds
      if (m < MONTH_MIN || m > MONTH_MAX) {
        throw new BadTimeConfigException();
      }
      
      this.monthList.add(m);
    }
    this.monthPos = 0;
  }
  
  /**
   * Handles populating the days list from the years argument of the on-clause.
   * 
   * @param daysString a {@link String} containing a single day; a square bracket-enclosed, comma-separated list of day
   *        numbers; or an asterisk.
   */
  private final void handleDays(String daysString) {
    
    this.dayList = new ArrayList<Integer>();
    
    // Convert days argument to a square bracket list
    String[] dayTokens = null;
    if (!daysString.matches(REGEX_MATCH_BRACKET_LIST)) {
      
      // Translate the asterisk to its alias
      if (daysString.equals("*")) {
        
        daysString = ASTERISK_DAYS;
        
      } else {
        
        // Otherwise, parse as a single value and surround in square brackets
        int d = -1;
        try {
          d = Integer.parseInt(daysString);
        } catch (@SuppressWarnings("unused") NumberFormatException nfe) {
          throw new BadTimeConfigException();
        }
        
        daysString = "[" + d + "]";
        
      }
      
    }
    
    // Trim square brackets and split on comma
    daysString = daysString.substring(1, daysString.length() - 1);
    dayTokens = daysString.split("\\s*,\\s*");
    
    // Convert each index to an integer and add it to the days list
    for (final String dayTok : dayTokens) {
      
      int d = -1;
      
      try {
        d = Integer.parseInt(dayTok);
      } catch (@SuppressWarnings("unused") NumberFormatException nfe) {
        throw new BadTimeConfigException();
      }
      
      // Check out of bounds
      if (d < DAY_MIN || d > DAY_MAX) {
        throw new BadTimeConfigException();
      }
      
      this.dayList.add(d);
    }
    this.dayPos = 0;
  }
  
  /**
   * Handles populating the year list from the years argument of the on-clause.
   * 
   * @param yearsString a {@link String} containing a single year; a square bracket-enclosed, comma-separated list of
   *        year numbers; or an asterisk.
   */
  private final void handleYears(String yearsString) {
    
    this.yearList = new ArrayList<Integer>();
    
    // Convert years argument to a square bracket list
    String[] yearTokens = null;
    if (!yearsString.matches(REGEX_MATCH_BRACKET_LIST)) {
      
      // Translate the asterisk to its alias
      if (yearsString.equals("*")) {
        
        yearsString = ASTERISK_DAYS;
        
      } else {
        
        // Otherwise, parse as a single value and surround in square brackets
        int y = -1;
        try {
          y = Integer.parseInt(yearsString);
        } catch (@SuppressWarnings("unused") NumberFormatException nfe) {
          throw new BadTimeConfigException();
        }
        
        // Check out of bounds
        if (y < YEAR_MIN || y > YEAR_MAX) {
          throw new BadTimeConfigException();
        }
        
        yearsString = "[" + y + "]";
        
      }
      
    }
    
    // Trim square brackets and split on comma
    yearsString = yearsString.substring(1, yearsString.length() - 1);
    yearTokens = yearsString.split("\\s*,\\s*");
    
    // Convert each index to an integer and add it to the years list
    for (final String yearTok : yearTokens) {
      
      int d = -1;
      
      try {
        d = Integer.parseInt(yearTok);
      } catch (@SuppressWarnings("unused") NumberFormatException nfe) {
        throw new BadTimeConfigException();
      }
      
      this.yearList.add(d);
    }
    this.yearPos = 0;
  }
  
  /**
   * Gets the time zone the dates/times are expected to be converted from.
   * 
   * @return the time zone as a {@link ZoneId}.
   */
  public ZoneId getFromTimeZone() {
    return this.fromTimeZone;
  }
  
  @Override
  public Iterator<Long> iterator() {
    return new Iterator<Long>() {
      
      @Override
      public boolean hasNext() {
        return absTimeConfigNextAvailable();
      }
      
      @Override
      public Long next() {
        
        // TODO: Implement next() in AbsoluteTimeConfiguration.iterator()
        // TODO: DON'T FORGET TO ACCOUNT FOR NONEXISTENT DATES! (February 30, November 31, etc.)
        // TODO: Make the year wildcard store the current year in its pos field
        
        // Construct new local datetime with current list pointers
        // Convert from issuer's time zone -> my time zone
        // Increment day, ripple carry through months/years
        
        return null;
      }
      
    };
  }
  
  /**
   * Wrapper for this class' Iterator.hasNext() method to prevent fields from not being private.
   * 
   * @return true if there are still more dates to go; false if not.
   */
  final protected boolean absTimeConfigNextAvailable() {
    return this.monthPos < this.monthList.size() - 1 && this.dayPos < this.dayList.size() - 1
        && this.yearPos < this.yearList.size() - 1;
  }
  
}

//@formatter:off
/*

HELP message:
  Time Clause: "at [4-digit military time (":" is optional)] or [ Hours:Minutes (AM or PM) ] or [Hours (am or pm)]
    Examples: "at 6pm", "at 1900", "at 5:45 PM"
    (If no time is specified, 6:00 AM will be used)
  Date Clause: "on ( [Months] [Days] [Years] ) or (Month#/Day#/Year#)"
    Months: Specify the month number (1-12) or the first three letters of the month. List them in []'s with a comma separating them. Replace this
      field with an asterisk (*) to specify all months.
    Days: Specify the day number. Lists and asterisks are supported.
    Years: Specify the 4-digit year. Lists and asterisks are supported.
    Examples: "on 7/21/2020", "on dec 25 *", "on [1, 15] * 2019", "on * * *", "on [1,7] [1,7,14,21,28] [2018,2019,2020]"
    (If no date is specified, today will be used unless the time has expired, then tomorrow will be used)
  Reminder Message
    Must start with the word "to".



:: Ideas ::

Shortcut aliases (on-clause):
* "every month" -> "on * 1 *"
* "every day" -> "on * * *"
* "every week" -> "on * [1,7,14,21,28] *"
("tomorrow", "next week", and "next month" are handled by the relative time configuration)

Shortcuts (at-clause):
* "noon"/"in the afternoon" -> 1200 (today)
* "morning"/"in the morning" -> 0600 (today)
* "evening"/"in the evening" -> 1800 (today)
* "night"/"at night" -> 2200 (today)
* "midnight"/"at midnight" -> 0000 (tomorrow)











 */
