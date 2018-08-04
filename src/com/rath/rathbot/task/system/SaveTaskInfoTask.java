
package com.rath.rathbot.task.system;

import com.rath.rathbot.task.RBTask;
import com.rath.rathbot.task.time.RelativeTimeConfiguration;
import com.rath.rathbot.task.time.TimeConfiguration;

public class SaveTaskInfoTask extends RBTask {
  
  private static final String TASK_NAME = "saveTaskInfo";
  
  private static final String CONFIG_STRING = "every 5m";
  
  private static final TimeConfiguration TIME_CONFIG = new RelativeTimeConfiguration(CONFIG_STRING);
  
  public SaveTaskInfoTask() {
    super(TASK_NAME, TIME_CONFIG);
  }
  
  @Override
  public void performTask() {
    // TODO: performTask() in SaveTaskInfoTask.java
  }
  
  @Override
  public boolean isResourceIntensive() {
    return false;
  }
  
  @Override
  public boolean isSystemTask() {
    return true;
  }
  
  @Override
  public long getNextEpochSecond() {
    // TODO Auto-generated method stub
    return 0;
  }
  
  @Override
  public boolean doesRepeat() {
    // TODO Auto-generated method stub
    return false;
  }
  
}
