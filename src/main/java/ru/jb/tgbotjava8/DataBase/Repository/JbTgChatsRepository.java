package ru.jb.tgbotjava8.DataBase.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.jb.tgbotjava8.DataBase.Entity.JbTgChat;

public interface JbTgChatsRepository extends JpaRepository<JbTgChat,Integer> {
}
