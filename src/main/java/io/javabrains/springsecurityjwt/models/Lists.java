package io.javabrains.springsecurityjwt.models;
public class Lists {
    private  int id;
    private  String nameList;
    private  String userListName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNameList() {
        return nameList;
    }

    public void setNameList(String nameList) {
        this.nameList = nameList;
    }


    public String getUserListName() {
        return userListName;
    }

    public void setUserListName(String userListName) {
        this.userListName = userListName;
    }
}
