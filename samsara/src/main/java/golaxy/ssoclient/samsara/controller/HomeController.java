package golaxy.ssoclient.samsara.controller;

import golaxy.ssoclient.samsara.contstant.AppConstant;
import golaxy.ssoclient.samsara.util.CookieUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Created by wxg on 2019/5/4 20:36
 */
//设置跨域请求源
@CrossOrigin(origins = "*", maxAge = 36000)
@Controller
public class HomeController {


    private RestTemplate restTemplate = new RestTemplate();

    @RequestMapping("/home")
    public ModelAndView home(HttpSession session, ModelAndView model) {

        model.addObject("userInfo", session.getAttribute("userInfo"));
        model.setViewName("home");
        return model;
        // "home";
    }


    @ResponseBody
    @RequestMapping("/logout")
    @SuppressWarnings("all")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        //获取ticker
        String ticket = CookieUtil.getCookie(request,"ticket");
        //请求sso-server退出登录
        boolean isLogout = restTemplate.getForObject(AppConstant.SSO_URL+"logout?ticket="+ticket,Boolean.class);
        if(isLogout){
            //清除cookie
            CookieUtil.setCookie(response,"ticket","",0);
            return "用户已登出";
        }
        return "登出失败";
    }
}
