package golaxy.ssoserver.han.login.service.impl;

import golaxy.ssoserver.han.login.service.LoginService;
import golaxy.ssoserver.han.util.EncryptUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class LoginServiceImpl implements LoginService {
    @Override
    public String getTicket(String username) {
        return null;
    }

    @Override
    public String createTicket(String uuid) {
        return DigestUtils.md5DigestAsHex((EncryptUtil.SALT + uuid + System.currentTimeMillis()).getBytes());
    }

    @Override
    public boolean login(String userName, String password) {
        return userName.equalsIgnoreCase(password);
    }

    @Override
    public boolean loginout(String userName) {
        return false;
    }
}
