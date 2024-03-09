package ru.jb.tgbotjava8.DataBase.Entity;

import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@NoArgsConstructor
@Table(name="JbTgUsers")
public class JbTgUser {
    @Id
    private long id;
    private String name;

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JbTgUser(long id){
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
            JbTgUser jbTgUser = (JbTgUser) o;
            return id == jbTgUser.id;
        }
        else
            return id == (long)o;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
