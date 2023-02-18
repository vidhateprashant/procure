package com.monstarbill.procure.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/procure")
public class TestController {
	
	@GetMapping("/status/check")
	public String getStatus() {
		return "working good from Procure...";
	}
	
}
