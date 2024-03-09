package ru.jb.tgbotjava8.JbTgBot.Xml;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

//<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
//<Keys>
//<Key>YourKey</Key>
//</Keys>

@Data
@XmlRootElement(name = "Keys")
@XmlAccessorType(XmlAccessType.FIELD)
public class OneTimePasswords {

    @XmlElement(name = "Key")
    private List<String> keys;

    public OneTimePasswords(){
        keys=new ArrayList<>();
    }
    public boolean contains(String key){
       return keys.contains(key);
    }

    public void remove(String key){
        keys.remove(key);
    }

    public void add(String key){
        keys.add(key);
    }

}
