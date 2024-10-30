package com.easylive.admin.controller;

import com.easylive.component.RedisComponent;
import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.vo.CheckCodeVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import com.easylive.redis.RedisConfig;
import com.easylive.redis.RedisUtils;
import com.easylive.service.UserInfoService;
import com.easylive.utils.StringTools;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * 用户信息 Controller
 */
@RestController("userInfoController")
@RequestMapping("/account")
public class AccountController extends ABaseController {

	@Resource
	private UserInfoService userInfoService;

	@Resource
	private AppConfig appConfig;

	@Resource
	private RedisComponent redisComponent;


	@RequestMapping("/checkCode")
	public ResponseVO checkCode(){
		ArithmeticCaptcha captcha=new ArithmeticCaptcha(100,42);
		String code=captcha.text();
		String checkCodeKey=redisComponent.saveCheckCode(code);
		String checkCodeBase64=captcha.toBase64();
		CheckCodeVO codeVO=new CheckCodeVO();
		codeVO.setCheckCode(checkCodeBase64);
		codeVO.setCheckCodeKey(checkCodeKey);
		return getSuccessResponseVO(codeVO);
	}

	@RequestMapping("/register")
	public ResponseVO register(@NotEmpty @Email @Size(max = 150) String email,
							   @NotEmpty @Size(max = 20) String nickName,
							   @NotEmpty String registerPassword,
							   @NotEmpty String checkCode,
							   @NotEmpty String checkCodeKey){
		try {
			if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
				throw new BusinessException("图片验证码不正确");
			}
			userInfoService.regester(email,nickName,registerPassword);
			return getSuccessResponseVO(null);
		}finally {
			redisComponent.cleanCheckCode(checkCodeKey);
		}
	}

	@RequestMapping("/login")
	public ResponseVO login(HttpServletRequest request,
							HttpServletResponse response,
							@NotEmpty String account,
							@NotEmpty String password,
							@NotEmpty String checkCode,
							@NotEmpty String checkCodeKey){
		try {
			if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
				throw new BusinessException("图片验证码不正确");
			}
			if (!account.equals(appConfig.getAdminAccount())||!password.equals(StringTools.encodeByMd5(appConfig.getAdminPassword()))){
				throw new BusinessException("账号或密码不正确");
			}
			String token=redisComponent.saveToken4Admin(account);
			saveToken2cookie(response,token);
			return getSuccessResponseVO(account);
		}finally {
			redisComponent.cleanCheckCode(checkCodeKey);
			Cookie[] cookies=request.getCookies();
			if (cookies!=null){
				String token=null;
				for(Cookie cookie:cookies){
					if (cookie.getName().equals(Constants.TOKEN_ADMIN)){
						token=cookie.getValue();
					}
				}
				if (!StringTools.isEmpty(token)) {
					redisComponent.cleanToken4Admin(token);
				}
			}
		}
	}

	@RequestMapping("/logout")
	public ResponseVO logout(HttpServletResponse response){
		cleanCookies(response);
		return getSuccessResponseVO(null);
	}

}