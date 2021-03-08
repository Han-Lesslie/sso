# sso

前言
本篇项目简单介绍了一下SSO的概念及原理，然后使用SpringBoot+Redis实现了一个简单的SSO系统。系统使用ticket的形式，依靠cookie携带ticket向sso服务器进行验证，验证通过后允许访问请求地址。

项目地址：https://gitee.com/xgpxg/SSO-DEMO

二、SSO介绍
SSO(Single Sign On)，单点登录，简单来说就是在一个具有多个子系统的系统中，只用登录一个子系统，然后访问其他子系统时不需要再次登录，即“一次登录，多处访问”，能够有效的提升用户体验。

单点登录的大致流程如下（基于cookie）：

1.用户首次访问A系统，A系统发现用户未登录，则重定向到SSO认证中心并携带请求url，进行登录验证；

2.用户在SSO认证中心进行用户名和密码验证登录，登录成功后，服务器生成一个ticket，然后重定向到系统A的源url并将该ticket追加到url参数。

3.系统A获取到url参数中的ticket，向SSO发起ticket较验，较验成功，则系统A放行，并将ticket存入到cookie。

4.用户访问B系统，此时B系统domain下已经携带ticket，直接向SSO发起ticket较验，较验成功，则放行，并将ticket存入cookie(更新ticket过期时间)

5.用户登出时，移除domain下的cookie。

流程图大致如下：



三、基于SpringBoot和Redis实现
系统结构
 ### 实现原理
原理较为简单，采用共享cookie实现SSO，sso-server使用redis存储用户ticket，app-a和app-b使用Spring拦截器过滤用户请求，每个请求都需要向sso-server验证ticket,若验证失败则重定向到登录(附带源url).

系统特点
使用一种一次性的ticket，即一个ticket只能使用一次，用过之后立马失效，来保证ticket的安全性。

主要代码
请看项目源码：https://gitee.com/xgpxg/SSO-DEMO

四、问题与思考
1.使用cookie还是url?

对于ticket的传输，一直在纠结使用cookie还是url附加参数的形式，最后处于方便考虑，还是使用了cookie。

2.如何保证ticket的安全性？

这个问题也困扰了我一下午，一直在寻求一种完美的、安全的ticket传递形式，最后明白了，在网络上没有绝对的安全，当破解它所带来的利益小于破解后所带来的利益时，他就是安全的，即安全是相对的。所以既然ticket是作为cookie或者url参数传递的，那么它的安全性本来就没有保证，我们能保证的是如何对ticket进行较验，如何验证拿到同一个ticket的用户是同一个用户。对于这个问题，我使用了一种一次性的ticket，上边也说了，这样虽然不能保证绝对的安全，但是在某种程度上能够有效防止他人直接截获cookie而获得权限。

3.关于cookie的domian

在测试过程中发现cookie写入的都是二级域名，例如aa.test.com而不是test.com，这导致其他系统无法共享cookie而导致单点登录失败，解决办法是直接设置domain为.test.com即可，注意前面的点不能省略。其次直接在yml里使用如下配置设置cookie的domain是无效的

server:
	servlet:
	    session:
	      cookie:
	        domain: .test.com
4.登出时直接请求sso-server还是请求子系统？

考虑到是子系统之间共享的cookie，所以清除子系统的cookie即可。

五、单点登录和单点登出演示
所用测域名：

#对应app-a，端口为8081
127.0.0.1 aa.test.com

#对应app-b,端口为8082
127.0.0.1 aa.test.com

#对应sso-server,端口为8080
127.0.0.1 sso.com
1.首先启动这三个项目，并启动redis



2.然后访问app-a的home页面(http://aa.test.com/home)，因为未登录所以跳转到sso的登录页面;

3.登录成功后自动返回app-a的home界面，此时再次访问app-b的home界面则不需要再次登录

4.在app-b的Home界面退出登录，再访问app-a的home界面，则要求重新登录

输入图片说明
