package in.ashokit.service;

import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.ashokit.dto.LoginDto;
import in.ashokit.dto.QuoteApiDto;
import in.ashokit.dto.RegisterDto;
import in.ashokit.dto.ResetPwdDto;
import in.ashokit.dto.UserDto;
import in.ashokit.entity.CityEntity;
import in.ashokit.entity.CountryEntity;
import in.ashokit.entity.StateEntity;
import in.ashokit.entity.UserEntity;
import in.ashokit.repo.CityRepo;
import in.ashokit.repo.CountryRepo;
import in.ashokit.repo.StateRepo;
import in.ashokit.repo.UserRepo;
import in.ashokit.utils.EmailUtils;

@Service
public class UserServiceImp implements UserService {

	@Autowired
	private UserRepo userRepo;
	
	@Autowired
	private CountryRepo countryRepo;
	
	@Autowired
	private StateRepo stateRepo;
	
	@Autowired
	private CityRepo cityRepo;
	
	@Autowired
	private EmailUtils emailUtils;
	
	private QuoteApiDto[] quatations = null; 
	
	Random random = new Random();
	
	@Override
	public Map<Integer, String> getCountries() {
		Map<Integer, String> countryMap = new HashMap<>();
		List<CountryEntity> countryList = countryRepo.findAll();
		
		countryList.forEach(c ->
				countryMap.put(c.getCountryId(), c.getName())
			);
		
		return countryMap;
	}

	@Override
	public Map<Integer, String> getStates(Integer cid) {
		Map<Integer, String> map = new HashMap<>();
		CountryEntity country = new CountryEntity();
		country.setCountryId(cid);
		
		StateEntity state = new StateEntity();
		state.setCountry(country);
		
		Example<StateEntity> of = Example.of(state);
		List<StateEntity> stateList = stateRepo.findAll(of);
		
		stateList.forEach(s ->
			map.put(s.getStateId(), s.getName())
		);
		return map;
	}

	@Override
	public Map<Integer, String> getCities(Integer sid) {
		Map<Integer, String> cityMap = new HashMap<>();
		List<CityEntity> cities = cityRepo.getCities(sid);
		
		cities.forEach(c -> 
			cityMap.put(c.getCityId(), c.getName())
		);
		return cityMap;
	}

	@Override
	public UserDto getUser(String email) {
		UserEntity user = userRepo.findByEmail(email);
		
//		UserDto dto = new UserDto();
//		BeanUtils.copyProperties(byEmail, dto);
		
		if(user == null) {
			return null;
		}
		
		ModelMapper mapper = new ModelMapper();
		
		return mapper.map(user, UserDto.class);
	}

	@Override
	public boolean registerUser(RegisterDto regDto) {
		ModelMapper mapper = new ModelMapper();
		
		UserEntity entity = mapper.map(regDto, UserEntity.class);
		
		CountryEntity country = countryRepo.findById(regDto.getCountryId()).orElseThrow();
		StateEntity state = stateRepo.findById(regDto.getStateId()).orElseThrow();
		CityEntity city = cityRepo.findById(regDto.getCityId()).orElseThrow();
		
		entity.setPassword(generateRandom());
		entity.setUpdatedPassword("NO");
		entity.setCity(city);
		entity.setCountry(country);
		entity.setState(state);
		
		UserEntity savedEntity = userRepo.save(entity);
		
		String subject = "User Registation";
		String body = "your temporary password is " + entity.getPassword();
		
		emailUtils.sendEmail(regDto.getEmail(), subject, body);
		
		return savedEntity.getUserId() != null;
	}

	@Override
	public UserDto getUser(LoginDto loginDto) {
		UserEntity entity = userRepo.findByEmailAndPassword(loginDto.getEmail(), loginDto.getPassword());
		if(entity == null) {
			return null;
		}
		
		ModelMapper mapper = new ModelMapper();
		return mapper.map(entity, UserDto.class);
	}

	@Override
	public boolean resetPwd(ResetPwdDto pwdDto) {
		UserEntity entity = userRepo.findByEmailAndPassword(pwdDto.getEmail(), pwdDto.getOldPassword());
		
		if(entity == null) {
			return false;
		}
		  
		entity.setPassword(pwdDto.getNewPassword());
		entity.setUpdatedPassword("YES");
		
		userRepo.save(entity);
		return true;
	}

	@Override
	public String getQuote() {
		if(quatations == null) {
			String url = "https://type.fit/api/quotes";
			
			// web service call
			RestTemplate rt = new RestTemplate();
			ResponseEntity<String> forEntity = rt.getForEntity(url, String.class);
			String body = forEntity.getBody();
			ObjectMapper obj = new ObjectMapper();
			
			try {
				quatations = obj.readValue(body, QuoteApiDto[].class);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}else {
			int index = random.nextInt(quatations.length - 1);
			
			return quatations[index].getText();
		}
		
		return null;
	}

	private String generateRandom() {
		String aToZ = "ABCDEFGHIJKLMNOPQRSTUV1234567890";
		StringBuilder sb = new StringBuilder();
		
		for(int i=0; i<5; i++) {
			int nextInt = random.nextInt(aToZ.length());
			sb.append(aToZ.charAt(nextInt));
		}
		
		return sb.toString();
	}
}
