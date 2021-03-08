package golaxy.ssoserver.han.login.controller;

import golaxy.ssoserver.han.constant.AppConstant;
import golaxy.ssoserver.han.login.service.LoginService;
import golaxy.ssoserver.han.util.CookieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
//设置跨域请求源
@CrossOrigin(origins = "*", maxAge = 36000)
public class LoginController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);
    private final RedisTemplate<String,String> redisTemplate;
    private final LoginService loginService;

    @Autowired
    public LoginController(LoginService loginService,RedisTemplate<String,String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.loginService = loginService;
    }

    /**
     * 登录页面
     * @param originalUrl 源url
     * @param uuid 客户端唯一标识符
     * @param response
     * @return
     */
    @GetMapping("/loginPage")
    public String loginPage(String originalUrl, String uuid, HttpServletResponse response) {
        CookieUtil.setCookie(response,"originalUrl",originalUrl, AppConstant.REDIS_TICKET_ALIVE_SECONDS);
        CookieUtil.setCookie(response,"uuid",uuid,AppConstant.REDIS_TICKET_ALIVE_SECONDS);
        return "login";
    }

    /**
     * 登录验证
     * @param userName 用户名
     * @param password 密码
     * @param request
     * @param response
     * @return
     */
    @ResponseBody
    @PostMapping("/login")
    public boolean login(String userName, String password, HttpServletRequest request,HttpServletResponse response) {
        boolean loginSuccess = loginService.login(userName,password);
        // 取到客户端唯一标识
        String uuid = CookieUtil.getCookie(request,"uuid");
        // 创建ticket
        String ticket = loginService.createTicket(uuid);
        //存redis，测试60秒过期
        redisTemplate.opsForValue().set(AppConstant.REDIS_TICKET_PREFIX+ticket,userName,AppConstant.REDIS_TICKET_ALIVE_SECONDS, TimeUnit.SECONDS);
        //回源
        String originalUrl = CookieUtil.getCookie(request,"originalUrl") + "?ticket=" + ticket;
        System.out.println(originalUrl);
        try {
            //重定向到源url
            response.sendRedirect(originalUrl);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return loginSuccess;
    }

    /**
     * 检查ticket
     *
     * @param ticket ticket
     * @param uuid   客户端唯一标识
     */
    @ResponseBody
    @GetMapping("/checkTicket")
    public String checkTicket(String ticket,String uuid) {
        String oldTicketValue = redisTemplate.opsForValue().get(AppConstant.REDIS_TICKET_PREFIX+ticket);
        //检验ticket
        // 当redis存在ticket时验证成功
        if (ticket != null && oldTicketValue != null) {
            //开启一次性ticket,每次返回新ticket，增强安全性，但是性能会降低
            //注意：开启一次性ticket验证时，uuid和ticket必传
            if (AppConstant.ENABLE_DISPOSABLE_TICKET) {
                //清除旧的ticket
                redisTemplate.delete(AppConstant.REDIS_TICKET_PREFIX+ticket);
                //生成新的ticket
                String newTicket = loginService.createTicket(uuid);
                //保存新的ticket
                redisTemplate.opsForValue().set(AppConstant.REDIS_TICKET_PREFIX+newTicket,oldTicketValue,AppConstant.REDIS_TICKET_ALIVE_SECONDS,TimeUnit.SECONDS);
                //返回新的ticket
                return newTicket;
            }else {
                return ticket;
            }
        }else return null;
    }

    /**
     * 获取用户信息
     *
     * @param ticket ticket
     */
    @ResponseBody
    @GetMapping("/getUserInfo")
    public Map<String, String> getUserInfo(String ticket) {
        //模拟根据ticket获取用户用户名,根据用户名获取用户信息
        String userName = redisTemplate.opsForValue().get(AppConstant.REDIS_TICKET_PREFIX + ticket);
        if (userName == null || "".equals(userName)) {
            return null;
        }
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("userName", userName);
        userInfo.put("ticket", ticket);
        return userInfo;
    }

    @ResponseBody
    @GetMapping("/logout")
    @SuppressWarnings("all")
    public boolean logout(String ticket){
        return redisTemplate.delete(AppConstant.REDIS_TICKET_PREFIX+ticket);
    }
}
