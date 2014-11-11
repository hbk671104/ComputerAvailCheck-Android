package com.bk.checkcompavail;

/**
 * Created by BK on 11/10/14.
 */
public class ListItem {

    private String roomNumber;
    private String availables;

    public ListItem(String roomN, String avail) {
        roomNumber = roomN;
        availables = avail;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getAvailables() {
        return availables;
    }

    public void setAvailables(String availables) {
        this.availables = availables;
    }

}
