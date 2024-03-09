package ru.jb.tgbotjava8.DataBase.Entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Objects;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name="JbTgChats")
public class JbTgChat {
    @Id
    private long id;
    private String name;

    public long getId() {
        return id;
    }
    public void setId(long chatId) {
        this.id = chatId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JbTgChat(long id){
        this.id=id;
    }

    @Override
    public String toString() {
        return "JbTgChat{" +
                "chatId=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(getClass() == o.getClass() || o.getClass()==long.class)) return false;
        if(getClass() == o.getClass()) {
            JbTgChat jbTgChat = (JbTgChat) o;
            return id == jbTgChat.id;
        }
        else
            return id == (long)o;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
