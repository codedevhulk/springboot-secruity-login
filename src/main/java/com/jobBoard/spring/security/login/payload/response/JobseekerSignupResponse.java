package com.jobBoard.spring.security.login.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobseekerSignupResponse {

	
	
	long jobSeekerId;
	String message;
	
}
