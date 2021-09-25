Необходимо разработать сетевое хранилище. Оно 
представляет собой клиент-серверное приложение. 
Клиент подключается к серверу
проходит аутентификацию
Получает список файлов, которые хранятся на сервере
Может отправлять файлы на сервер, или скачивать их оттуда.

Основные возможности:
* Клиент может отправить на сервер файл, скачать файл с сервера, удалить файл в локальном/удаленном репозитории 
(Удаленный репозиторий - папка пользователя на сервере, локальный репозиторий - его папка в каталоге проекта);
* Файлы на сервере хранятся в папках, имена которых совпадают с именем пользователя т.е.соответствуют логину. 
(Для пользователя с логином user1 файлы будут падать в папку /server_root/repository/user1);
* Клиент должен иметь возможность увидеть свои файлы на сервере;
(чтобы это сделать, необходимо чтобы сервер мог отправлять клиенту список файлов (а список файлов - это не файл))
* Отправка и скачивание файлов должны осуществляться по сети!!!
* На сервере должна быть база данных с пользователями (например sqlite)
*

Вопросы:
* Каким образом вы собираетесь передавать файлы по сети? 
- через сериализацию или через протокол(бинарный формат)
* Что хранить в базе данных?
- данные пользователей
* Хотим ли мы отправлять что либо, кроме файлов? 
- параллельно с файлами нам нужно передавать команды (на удаление файла на сервере, на запрос списка файлов, на скачивание)
* Как по одному каналу связи передавать и файлы и команды? Как их разделять?
- вариант 1. Открываем одно соединение для файлов, одно соединение для команд. Проблема авторизации на сервере обоих соединений
2-я проблема, если открываем 2 независимых соединения, перегружаем сервер, т.к. для 1000 клиентов это уже 2000 соединений.
- Вариант 2.1 (для сериализации). Тип объекта. Например для файла это FileMessage, для команды это CommandMessage, каждое из которых является AbstractMessage
- Вариант 2.2 (для протокольного решения). Использовать один сигнальный байт для файлов, другой для команд. 

То, чего делать не надо на первом этапе:
* на сервере в папках пользователей не надо делать подкаталоги;
* не надо делать шифрование передаваемых данных;
* не надо делать синхронизацию папок пользователя на сервере и клиенте;

ДЗ: 
1. Ответить на вопросы
2. Реализовать передачу файла от клиента серверу

Задание на курс:
* к первому код-ревью: сделать передачу файлов в обе стороны. Графический интерфейс не нужен.
* ко второму код-ревью: доработанный вариант передачи файлов + по желанию GUI.



