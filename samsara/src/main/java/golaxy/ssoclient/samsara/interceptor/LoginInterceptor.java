package golaxy.ssoclient.samsara.interceptor;

import golaxy.ssoclient.samsara.contstant.AppConstant;
import golaxy.ssoclient.samsara.util.CookieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * Created by wxg on 2019/5/4 20:39
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoginInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        //客户端唯一标识，这里采用第一次访问时的session作为唯一标识，若为空则写入cookie
        String uuid = CookieUtil.getCookie(request, "uuid");
        System.out.println("uuid ========= >>>>> " + uuid);
        if (null ==uuid ) {
            uuid = session.getId();
            CookieUtil.setCookie(response, "uuid", uuid, 3600);
        }

        //从cookie获取ticket
        String ticket = CookieUtil.getCookie(request, "ticket");
        //cookie没有从url参数中获取
        if (null == ticket) {
            ticket = request.getParameter("ticket");
        }

        if (null == ticket) {
            logger.debug("非法请求:未获取到ticket,重定向到登录...");
            String url = AppConstant.SSO_URL + "loginPage?originalUrl=" + request.getRequestURL() + "&uuid=" + uuid;
            System.out.println("requestUrl ============= >>>> " + url);
            response.sendRedirect(AppConstant.SSO_URL + "loginPage?originalUrl=" + request.getRequestURL() + "&uuid=" + uuid);
            return false;
        } else {
            RestTemplate restTemplate = new RestTemplate();

            //较验ticket,较验成功返回ticket，实际上检验成功时可以返回用户信息，校验失败则返回null
            String t = restTemplate.getForObject(AppConstant.SSO_URL + "checkTicket?ticket=" + ticket + "&uuid=" + uuid, String.class);

            if (t != null) {
                Map userInfo = restTemplate.getForObject(AppConstant.SSO_URL + "getUserInfo?ticket=" + t, Map.class);
                logger.debug("ticket较验通过:" + userInfo);

                //userInfo存入session
                session.setAttribute("userInfo", userInfo);

                //更新ticket
                CookieUtil.setCookie(response, "ticket", t, 60);
            } else {
                logger.debug("非法请求:ticket较验失败,重定向到登录...");
                response.sendRedirect(AppConstant.SSO_URL + "loginPage?originalUrl=" + request.getRequestURL() + "&uuid=" + uuid);

                return false;
            }
        }
        return true;
    }
}
