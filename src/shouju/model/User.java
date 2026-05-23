package shouju.model;

//创建用户类
public class User {
    private String username;//用户名
    private String password;//密码
    private String email;//邮箱

    //构造方法
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // Getter和Setter方法
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public void setPassword(String password) { this.password = password; }
}
