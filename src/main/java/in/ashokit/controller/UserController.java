package in.ashokit.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import in.ashokit.dto.LoginDto;
import in.ashokit.dto.RegisterDto;
import in.ashokit.dto.ResetPwdDto;
import in.ashokit.dto.UserDto;
import in.ashokit.service.UserService;
import in.ashokit.utils.AppConstaints;
import in.ashokit.utils.AppProperties;

@Controller
public class UserController {

	@Autowired
	private UserService service; 
	
	@Autowired
	private AppProperties props;
	
	@GetMapping("/states/{cid}")
	@ResponseBody
	public Map<Integer, String> getStates(@PathVariable("cid") Integer cid){
		return service.getStates(cid);
	}
	
	@GetMapping("/cities/{sid}")
	@ResponseBody
	public Map<Integer, String> getCities(@PathVariable("sid") Integer sid){
		return service.getCities(sid);
	}
	
	@GetMapping("/register")
	public String registerPage(Model model) {
		model.addAttribute("register", new RegisterDto());
		model.addAttribute("countries", service.getCountries());
		return AppConstaints.REGISTER_VIEW;
	}
	
	@PostMapping("/register")
	public String registerPage(@ModelAttribute("register") RegisterDto reg, Model model) {
		
		model.addAttribute("countries", service.getCountries());
//		Map<String,String> messages = props.getMessages();
		
		UserDto user = service.getUser(reg.getEmail());
		if(user != null) {
			model.addAttribute(AppConstaints.ERROR_MSG, AppConstaints.DUP_EMAIL);
			
		}else {
			boolean registerUser = service.registerUser(reg);
			if(registerUser) {
				model.addAttribute(AppConstaints.SUCC_MSG, AppConstaints.REG_SUCC);
			}else {
				model.addAttribute(AppConstaints.ERROR_MSG, AppConstaints.REG_FAIL);
			}
		}
		
 		return AppConstaints.REGISTER_VIEW;
	}
	
	@GetMapping("/")
	public String login(Model model) {
		model.addAttribute("login", new LoginDto());
		return AppConstaints.INDEX;
	}
	
	@PostMapping("/login")
	public String loginPage(@ModelAttribute("login")LoginDto loginDto, Model model) {
		
		Map<String,String> messages = props.getMessages();
		
		UserDto user = service.getUser(loginDto);
		
		if(user == null) {
			model.addAttribute(AppConstaints.ERROR_MSG, messages.get("invalidCredentials"));
//			model.addAttribute("login", user);
			return AppConstaints.INDEX;
		}
		
		if("YES".equals(user.getUpdatedPassword())) {
			// pwd already updated : go to dashboard
			return "redirect:dashboard";
		}else {
			// pwd not updated : go to reset password page
			ResetPwdDto resetPwdDto = new ResetPwdDto();
			resetPwdDto.setEmail(user.getEmail());
			model.addAttribute("resetPwdDto", resetPwdDto);
			return AppConstaints.RESET_PWD_VIEW;
		}
	}
	
	@PostMapping("/reset")
	public String resetPswd(ResetPwdDto resetPwd, Model model) {
		
		Map<String,String> messages = props.getMessages();
		
		if(!resetPwd.getNewPassword().equals(resetPwd.getConfirmPassword())) {
			model.addAttribute(AppConstaints.ERROR_MSG, messages.get("pwdMatchErr"));
			return AppConstaints.RESET_PWD_VIEW;
		}
		
		UserDto user = service.getUser(resetPwd.getEmail());
		if(user.getPassword().equals(resetPwd.getOldPassword())) {
			boolean reset = service.resetPwd(resetPwd);
			if(reset) {
				return "redirect:/";
			}else {
				model.addAttribute(AppConstaints.ERROR_MSG, messages.get("pwdUpdateErr"));
				return AppConstaints.RESET_PWD_VIEW;
			}
		}		
		else {
			model.addAttribute(AppConstaints.ERROR_MSG, messages.get("oldPwdErr"));
			return AppConstaints.RESET_PWD_VIEW;
		}
	}
	
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		String quote = service.getQuote();
		model.addAttribute("quote", quote);
		return "dashboard";
	}
	
	@GetMapping("/logout")
	public String logout(Model model) {
		return "redirect:/";
	}
	
}
