package com.example.app;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class Application {

	/**
	 * Pins the JVM to UTC so LocalDateTime.now() means the same thing everywhere.
	 *
	 * Entities store LocalDateTime and the API serialises it without an offset, so the value on the
	 * wire is only interpretable if writer and reader agree on the zone. They did not: a container
	 * runs UTC while a development machine runs local time, so the same code produced timestamps
	 * 5.5 hours apart, and the client -- which parses them as local time -- read every deployed
	 * timestamp as hours in the past. Anything comparing a timestamp to "now" was wrong by that
	 * margin, silently.
	 *
	 * The alternative is putting an offset on the wire (Instant rather than LocalDateTime), which is
	 * the better long-term answer but changes the format of every date the API returns.
	 */
	@PostConstruct
	void useUtc() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
