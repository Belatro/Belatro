package backend.belatro;

import backend.belatro.util.DotenvInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BelatroApplication {

	static {
		DotenvInitializer.init();
	}


	public static void main(String[] args) {
		SpringApplication.run(BelatroApplication.class, args);
	}

}
