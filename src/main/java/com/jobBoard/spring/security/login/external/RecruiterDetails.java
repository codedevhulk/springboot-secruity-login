package com.jobBoard.spring.security.login.external;


import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RecruiterDetails {
	
	@Id
	//@GeneratedValue(strategy=GenerationType.AUTO)
	long recruiterId;
	String userName;
	String firstName;
	String lastName;
	String email;
	String mobileNumber;
	String password;
	String address;
	
}
