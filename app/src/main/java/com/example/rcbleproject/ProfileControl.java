package com.example.rcbleproject;

public class ProfileControl {
    private String name;
    private long id;

    public ProfileControl(long id){
        this.id = id;
    }

    public boolean setName(String name){
        if (name.equals("") || name == null) return false;
        this.name = name;
        return true;
    }

    public String getName(){
        return name;
    }

    public long getId(){
        return id;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
