public class Log {
    String user;
    String message;
    long time;
    public Log(String user, String message, long time) {
        this.user = user;
        this.message = message;
        this.time = time;
    }

    public String getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    public long getTime() {
        return time;
    }
}
