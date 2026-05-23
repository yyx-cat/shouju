package shouju.model;

import java.io.Serializable;

//存储登录信息的一个类
public class UserLoginInfo implements Serializable {
    private String username;
    private String password;

    public UserLoginInfo(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
