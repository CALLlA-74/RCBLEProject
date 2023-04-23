package com.example.rcbleproject.Model;

import androidx.annotation.NonNull;

public class ProfileControl {
    private String name;
    private final long id;

    public ProfileControl(long id){
        this.id = id;
    }

    public void setName(String name){
        if (name == null || name.equals("")) return;
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public long getId(){
        return id;
    }

    @NonNull
    @Override
    public String toString() {
        return this.name;
    }
}
