package com.jobBoard.spring.security.login.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.jobBoard.spring.security.login.external.JobSeekerDetails;
import com.jobBoard.spring.security.login.external.RecruiterDetails;
import com.jobBoard.spring.security.login.models.ERole;
import com.jobBoard.spring.security.login.models.Role;
import com.jobBoard.spring.security.login.models.User;
import com.jobBoard.spring.security.login.payload.request.LoginRequest;
import com.jobBoard.spring.security.login.payload.request.SignupRequest;
import com.jobBoard.spring.security.login.payload.response.MessageResponse;
import com.jobBoard.spring.security.login.payload.response.RecruiterSignupResponse;
import com.jobBoard.spring.security.login.payload.response.JobseekerSignupResponse;
import com.jobBoard.spring.security.login.payload.response.UserInfoResponse;
import com.jobBoard.spring.security.login.repository.RoleRepository;
import com.jobBoard.spring.security.login.repository.UserRepository;
import com.jobBoard.spring.security.login.security.jwt.JwtUtils;
import com.jobBoard.spring.security.login.security.services.UserDetailsImpl;

import lombok.extern.slf4j.Slf4j;

//@CrossOrigin(origins = "*", maxAge = 3600)
@CrossOrigin
@RestController
@RequestMapping("/api/auth")
@Slf4j

public class AuthController {
  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;
  
  @Autowired
  RestTemplate restTemplate;

  @Autowired
  JwtUtils jwtUtils;

  
  
  // For Jobseeker signup and signin functionality
  
  
  
  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    Authentication authentication = authenticationManager
        .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

    ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

    jwtUtils.generateTokenFromUsername("Rajesh");
    
    
    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());

    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
        .body(new UserInfoResponse(userDetails.getId(),
                                   userDetails.getUsername(),
                                   userDetails.getEmail(),
                                   roles));
  }
  
  

  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
	  
	  
	  
	  
	  
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
    }

    // Create new user's account
    User user = new User(signUpRequest.getUsername(),
                         signUpRequest.getEmail(),
                         encoder.encode(signUpRequest.getPassword()));

    Set<String> strRoles = signUpRequest.getRole();
    Set<Role> roles = new HashSet<>();

    if (strRoles == null) {
      Role userRole = roleRepository.findByName(ERole.ROLE_JOBSEEKER)
          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
        case "admin":
          Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(adminRole);

          break;
        case "mod":
          Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(modRole);

          break;
        default:
          Role userRole = roleRepository.findByName(ERole.ROLE_JOBSEEKER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(userRole);
        }
      });
    }

    user.setRoles(roles);
    userRepository.save(user);
    
    JobSeekerDetails jobSeekerDetails=JobSeekerDetails.builder()
    		.email(user.getEmail())
    	.userName(user.getUsername()).jobSeekerId(user.getId()).build();
    
    	
    
    
    JobseekerSignupResponse signupresponse=new JobseekerSignupResponse(user.getId(),"User registered successfully!");
    
    		
    
    //restTemplate.postForObject("http://JOBSEEKERSERVICE/jobseeker/updateprofile", jobSeekerDetails,JobSeekerDetails.class);
    restTemplate.put("http://JOBSEEKERSERVICE/jobseeker/updateprofile", jobSeekerDetails);

   // return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    //return signupresponse;
    return new ResponseEntity<>(signupresponse,HttpStatus.OK);
  }

  @PostMapping("/signout")
  public ResponseEntity<?> logoutUser() {
    ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new MessageResponse("You've been signed out!"));
  }
  
  
  
  
  //////////////// For recruiter signup and signin ////////////////////////////
  
  
  
  
  
  @PostMapping("/recruiter/signup")
  public ResponseEntity<?> registerRecruiter(@Valid @RequestBody SignupRequest signUpRequest) {
	  
	  log.info(signUpRequest.toString());
	  
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
    }

    // Create new user's account
    User user = new User(signUpRequest.getUsername(),
                         signUpRequest.getEmail(),
                         encoder.encode(signUpRequest.getPassword()));

    Set<String> strRoles = signUpRequest.getRole();
    Set<Role> roles = new HashSet<>();

    if (strRoles == null) {
      Role userRole = roleRepository.findByName(ERole.ROLE_RECRUITER)
          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
        case "admin":
          Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(adminRole);

          break;
        case "mod":
          Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(modRole);

          break;
        default:
          Role userRole = roleRepository.findByName(ERole.ROLE_RECRUITER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(userRole);
        }
      });
    }

    user.setRoles(roles);
    userRepository.save(user);
    
    RecruiterDetails recruiterDetails=RecruiterDetails.builder()
    	.userName(user.getUsername())
    	.email(user.getEmail())
    	.recruiterId(user.getId())
    	.build();
    
    	RecruiterSignupResponse recruiterSignupResponse=new RecruiterSignupResponse(user.getId(),"User registered successfully!");
    		
    
    //restTemplate.postForObject("http://JOBSEEKERSERVICE/jobseeker/updateprofile", jobSeekerDetails,JobSeekerDetails.class);
    restTemplate.put("http://RECRUITERSERVICE/recruiter/updateprofile", recruiterDetails);

    //return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    return new ResponseEntity<>(recruiterSignupResponse,HttpStatus.OK);
  }
  
  
  
  // To delete all the data 
  @DeleteMapping("/deleteall")
  public ResponseEntity<String> deleteUserData(){
	  userRepository.deleteAll();
	  return new ResponseEntity<>("User's Information deleted",HttpStatus.OK);
	  
  }
  
  
  
  @PostMapping("/recruiter/signin")
  public ResponseEntity<?> authenticateRecruiter(@Valid @RequestBody LoginRequest loginRequest) {

    Authentication authentication = authenticationManager
        .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

    ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

    jwtUtils.generateTokenFromUsername("Rajesh");
    
    
    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());

    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
        .body(new UserInfoResponse(userDetails.getId(),
                                   userDetails.getUsername(),
                                   userDetails.getEmail(),
                                   roles));
  }
  
  
  
  
  @PostMapping("/recruiter/signout")
  public ResponseEntity<?> logoutRecruiter() {
    ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new MessageResponse("You've been signed out!"));
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
}
