package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Антон on 06.03.2017.
 */
public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    public void run() {
        // создаем вспомогательный поток и запускаем его
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        // Заставить текущий поток ожидать, пока он не получит нотификацию из другого потока
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Во время работы произошла ошибка");
                System.exit(0);
            }
            if (clientConnected) {
                ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду ‘exit’.");
            }else {
                ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
            }
        }
        while (clientConnected) {
            String text = ConsoleHelper.readString();
            if (text.equals("exit")) break;

            if (shouldSendTextFromConsole()) sendTextMessage(text);
        }
    }

    /*
     * должен запросить ввод адреса сервера у пользователя и вернуть введенное значение
     */
    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера");
        return ConsoleHelper.readString();
    }

    /*
     * должен запрашивать ввод порта сервера и возвращать его
     */
    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите номер порта сервера");
        return ConsoleHelper.readInt();
    }

    /*
     * должен запрашивать и возвращать имя пользователя
     */
    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите имя пользователя");
        return ConsoleHelper.readString();
    }

    /*
     * в данной реализации клиента всегда должен возвращать true (мы всегда отправляем текст введенный в консоль)
     * метод может быть переопределен классами - наследниками
     */
    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    /*
     * должен создавать и возвращать новый объект класса SocketThread
     */
    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    /*
     * создает новое текстовое сообщение, используя переданный текст и отправляет его серверу через соединение connection
     */
    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Во время отправки сообщения произошла ошибка");
            clientConnected = false;
        }
    }


    /**
     ** класс отвечает за поток, устанавливающий сокетное соединение и читающий сообщения сервера
     **/
    public class SocketThread extends Thread {
        @Override
        public void run() {
            //Запрашиваем адрес и порт сервера
            String serverAdress = getServerAddress();
            int serverPort = getServerPort();

            try(Socket socket = new Socket(serverAdress, serverPort)) {
                //Создаем соединение
                connection = new Connection(socket);
                //Вызываем рукопожатие
                clientHandshake();
                //Вызываем основной цикл
                clientMainLoop();

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                notifyConnectionStatusChanged(false);
            } catch (ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }

        /*
                 * Handshake
                 */
        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();

                if (message.getType() == MessageType.NAME_REQUEST) {
                    connection.send(new Message(MessageType.USER_NAME,getUserName()));
                }else if (message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                }else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        /*
         * Главный цикл обработки сообщений
         */
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();

                if (message.getType() == MessageType.TEXT) {
                    processIncomingMessage(message.getData());
                }else if (message.getType() == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(message.getData());
                }else if (message.getType() == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(message.getData());
                }else {
                    throw new IOException("Unexpected MessageType");
                }
            }

        }

        /*
         * должен выводить текст message в консоль
         */
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        /*
         * должен выводить в консоль информацию о том, что участник с именем userName присоединился к чату
         */
        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage("участник с именем " + userName + " присоединился к чату");
        }

        /*
         * должен выводить в консоль, что участник с именем userName покинул чат
         */
        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("участник с именем " + userName + " покинул чат");
        }

        /*
         * Устанавливать значение поля clientConnected внешнего объекта Client в соответствии с переданным параметром.
         * Оповещать (пробуждать ожидающий) основной поток класса Client.
         */
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;

            synchronized (Client.this) {
                Client.this.notify();
            }
        }
    }
}
