package ru.jb.tgbotjava8.DataBase.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.jb.tgbotjava8.DataBase.Entity.JbTgChat;
import ru.jb.tgbotjava8.DataBase.Entity.JbTgUser;

public interface JbTgUsersRepository extends JpaRepository<JbTgUser,Integer> {
}
