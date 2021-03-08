package golaxy.ssoserver.han.login.service;

public interface LoginService {

    String getTicket(String username);

    String createTicket(String uuid);

    boolean login(String userName,String password);
    boolean loginout(String userName);
}
