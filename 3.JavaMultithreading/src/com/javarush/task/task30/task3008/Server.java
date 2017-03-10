package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Антон on 03.03.2017.
 */
public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Введите порт сервера: ");
        int serverPort = ConsoleHelper.readInt();

        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            ConsoleHelper.writeMessage("Сервер запущен");

            while (true) {
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage("Произошла ошибка");
        }
    }

    public static void sendBroadcastMessage(Message message) {
        try {
            for (Connection connection : connectionMap.values()) {
                connection.send(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleHelper.writeMessage("Сообщение не отправлено");
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("Установленно соединение с адресом " + socket.getRemoteSocketAddress());
            String clientName = null;

            //Создаем Connection
            try (Connection connection = new Connection(socket)) {
                //Выводить сообщение, что установлено новое соединение с удаленным адресом
                ConsoleHelper.writeMessage("Подключение к порту: " + connection.getRemoteSocketAddress());

                //Вызывать метод, реализующий рукопожатие с клиентом, сохраняя имя нового клиента
                clientName = serverHandshake(connection);

                //Рассылать всем участникам чата информацию об имени присоединившегося участника (сообщение с типом USER_ADDED)
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, clientName));

                //Сообщать новому участнику о существующих участниках
                sendListOfUsers(connection, clientName);

                //Запускать главный цикл обработки сообщений сервером
                serverMainLoop(connection, clientName);

                //После того как все исключения обработаны, удаляем запись из connectionMap
                connectionMap.remove(clientName);

                //и отправлялем сообщение остальным пользователям
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, clientName));

                ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто");

            } catch (IOException e) {
                ConsoleHelper.writeMessage("Ошибка при обмене данными с удаленным адресом");
            } catch (ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Ошибка при обмене данными с удаленным адресом");
            }
        }

        /**
         * Handshake
         **/
        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                // Сформировать и отправить команду запроса имени пользователя
                connection.send(new Message(MessageType.NAME_REQUEST));
                // Получить ответ клиента
                Message message = connection.receive();

                // Проверить, что получена команда с именем пользователя
                if (message.getType() == MessageType.USER_NAME) {

                    //Достать из ответа имя, проверить, что оно не пустое
                    if (!message.getData().isEmpty()) {

                        // и пользователь с таким именем еще не подключен (используй connectionMap)
                        if (!connectionMap.containsKey(message.getData())) {

                            // Добавить нового пользователя и соединение с ним в connectionMap
                            connectionMap.put(message.getData(), connection);

                            // Отправить клиенту команду информирующую, что его имя принято
                            connection.send(new Message(MessageType.NAME_ACCEPTED));

                            // Вернуть принятое имя в качестве возвращаемого значения
                            return message.getData();
                        }
                    }
                }
            }
        }
        /**
         ** Сообщать новому участнику о существующих участниках
         **/
        private void sendListOfUsers(Connection connection, String userName) throws IOException {
            for (String key : connectionMap.keySet()) {
                if (!userName.equals(key)) {
                    connection.send(new Message(MessageType.USER_ADDED, key));
                }
            }
        }
        /**
         ** Запускать главный цикл обработки сообщений сервером
         **/
        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while(true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    //Формируем новое текстовое сообщение
                    StringBuilder sb = new StringBuilder();
                    sb.append(userName).append(": ").append(message.getData());
                    String text = sb.toString();

                    // Отправляем новое текстовое сообщение
                    sendBroadcastMessage(new Message(MessageType.TEXT, text));
                } else {
                    ConsoleHelper.writeMessage("Error");
                }
            }
        }
    }
}