package backend.belatro;

import backend.belatro.util.DotenvInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class BelatroApplication {

	static {
		DotenvInitializer.init();
	}


	public static void main(String[] args) {
		SpringApplication.run(BelatroApplication.class, args);
	}

}
