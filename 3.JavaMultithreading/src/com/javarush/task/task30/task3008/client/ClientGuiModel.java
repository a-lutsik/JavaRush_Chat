package com.javarush.task.task30.task3008.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Антон on 07.03.2017.
 */
public class ClientGuiModel {
    private final Set<String> allUserNames = new HashSet<>();
    private String newMessage;

    //добавление пользователя во множество
    public void addUser(String newUserName) {

        allUserNames.add(newUserName);
    }

    // удалять имя участника из множества
    public void deleteUser(String userName) {

        allUserNames.remove(userName);
    }

    public Set<String> getAllUserNames() {
        return Collections.unmodifiableSet(allUserNames);
    }

    public String getNewMessage() {
        return newMessage;
    }

    public void setNewMessage(String newMessage) {
        this.newMessage = newMessage;
    }
}
