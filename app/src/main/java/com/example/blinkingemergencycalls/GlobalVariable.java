package com.example.blinkingemergencycalls;

import java.util.ArrayList;
import java.util.List;

public class GlobalVariable {
  public boolean bFirst = true;
  public int blinkCount;
  public List<String> phoneNumbers = new ArrayList<>();
  public void setbFirst(boolean value){ this.bFirst = value;}
  public void setBlinkCount(int value){ this.blinkCount = value;}
  public void setPhoneNumbers(List<String> value){
    this.phoneNumbers = value;
  }
  public boolean getbFirst(){ return bFirst;}
  public int getBlinkCount(){ return blinkCount;}
  public List<String> getPhoneNumbers() { return phoneNumbers;}

}
